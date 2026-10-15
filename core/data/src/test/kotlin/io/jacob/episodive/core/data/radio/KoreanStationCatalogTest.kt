package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.art
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

class KoreanStationCatalogTest {
    private val entries = KoreanStationCatalog.entries

    @Test
    fun `Given catalog, Then station ids are unique`() {
        val ids = entries.map { it.station.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `Given catalog, Then every entry has radioBrowserIds and none are shared`() {
        assertTrue(entries.all { it.radioBrowserIds.isNotEmpty() })
        val all = entries.flatMap { it.radioBrowserIds }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `Given catalog, Then fallbackUrl avoids excluded third party domains`() {
        val excluded = listOf("bsod.kr", "qtfm.cn", "duckdns.org")
        entries.mapNotNull { it.fallbackUrl }.forEach { url ->
            assertFalse(url, excluded.any { url.contains(it) })
        }
    }

    @Test
    fun `Given catalog, Then fallbackUrl has no expiring token or chunklist`() {
        // 변형·세션 chunklist 는 CDN 재구성 시 404 가 나므로 master playlist 만 허용한다.
        val tokens = listOf("Policy=", "token=", "_lsu_sa_", "chunklist")
        entries.mapNotNull { it.fallbackUrl }.forEach { url ->
            assertFalse(url, tokens.any { url.contains(it) })
        }
    }

    @Test
    fun `Given catalog, Then broadcasterSource 가 있는 항목은 HLS 이고 방송사 9개가 모두 갖는다`() {
        val withSource = entries.filter { it.broadcasterSource != null }
        assertTrue(withSource.all { it.isHls })
        assertEquals(
            setOf(
                "kbs-1radio", "kbs-happyfm", "kbs-classicfm", "kbs-coolfm",
                "mbc-sfm", "mbc-fm4u", "mbc-allthatmusic", "sbs-powerfm", "sbs-lovefm",
            ),
            withSource.map { it.station.id }.toSet(),
        )
    }

    @Test
    fun `Given catalog, Then onAirSource 는 KBS·MBC·SBS·CBS 전국 12채널만 갖는다`() {
        val withSource = entries.filter { it.onAirSource != null }.map { it.station.id }
        assertEquals(
            setOf(
                "kbs-1radio", "kbs-happyfm", "kbs-classicfm", "kbs-coolfm",
                "mbc-sfm", "mbc-fm4u", "mbc-allthatmusic",
                "sbs-powerfm", "sbs-lovefm",
                "cbs-sfm", "cbs-musicfm", "cbs-joy4u",
            ),
            withSource.toSet(),
        )
    }

    @Test
    fun `Given catalog, Then every station is radio`() {
        assertTrue(entries.all { it.station.kind == StationKind.RADIO })
    }

    @Test
    fun `Given catalog, Then 이니셜이 정해 둔 값과 같다`() {
        val expected = mapOf(
            "kbs-1radio" to "1라", "kbs-happyfm" to "해", "kbs-classicfm" to "클", "kbs-coolfm" to "쿨",
            "mbc-sfm" to "표", "mbc-fm4u" to "FM4U", "mbc-allthatmusic" to "올",
            "sbs-powerfm" to "파", "sbs-lovefm" to "러",
            "cbs-sfm" to "표", "cbs-musicfm" to "음", "cbs-joy4u" to "JOY",
            "ebs-fm" to "EBS", "ytn-radio" to "YTN", "tbs-fm" to "TBS", "tbs-efm" to "eFM",
            "gugak-fm" to "국", "kookbang-fm" to "국", "cpbc-radio" to "가", "febc-seoul" to "서",
            "febc-jeju" to "제", "ifm" to "경", "obs-radio" to "OBS", "tbn-gyeongin" to "경",
            "tbn-ulsan" to "울", "arirang-radio" to "아", "afn-humphreys" to "험",
            "mbc-chungbuk-sfm" to "표", "mbc-chungbuk-fm4u" to "FM4U",
            "andong-mbc-am" to "표", "andong-mbc-fm4u" to "FM4U",
            "befm" to "부", "jeonju-fm" to "전", "mapo-fm" to "마", "seongseo-fm" to "성", "sone-fm" to "수",
        )
        assertEquals(expected.keys, entries.map { it.station.id }.toSet())
        entries.forEach { assertEquals(it.station.id, expected[it.station.id], it.station.art.initials) }
    }

    @Test
    fun `Given catalog, Then 한 방송사 안에서 이니셜이 겹치지 않는다`() {
        entries.map { it.station }.groupBy { it.broadcaster }.forEach { (broadcaster, stations) ->
            val initials = stations.map { it.art.initials }
            assertEquals(broadcaster, initials.size, initials.toSet().size)
        }
    }

    @Test
    fun `Given catalog, Then 이니셜은 1~4자다`() {
        entries.forEach { assertTrue(it.station.id, it.station.art.initials.length in 1..4) }
    }

    @Test
    fun `Given http fallback, Then host is allowed by network security config`() {
        // 허용 목록에 없으면 재생 시점에 IO_CLEARTEXT_NOT_PERMITTED 로 조용히 실패한다.
        val path = System.getProperty("episodive.networkSecurityConfig")
            ?: return fail("episodive.networkSecurityConfig 시스템 프로퍼티가 없다 — core/data/build.gradle.kts 확인")
        val allowed = cleartextAllowedDomains(File(path).readText())

        val httpHosts = entries.mapNotNull { it.fallbackUrl }
            .filter { it.startsWith("http://") }
            .map { URI(it).host }
        assertTrue("http fallback 이 하나도 없다", httpHosts.isNotEmpty())

        httpHosts.forEach { host ->
            assertTrue("cleartext 허용 목록에 없는 호스트: $host", isCleartextAllowed(host, allowed))
        }
    }

    @Test
    fun `Given config xml, Then only domains under a cleartext-permitted config count as allowed`() {
        val xml = """
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="true">open.example.com</domain>
                    <domain>exact.example.com</domain>
                </domain-config>
                <domain-config cleartextTrafficPermitted="false">
                    <domain includeSubdomains="true">closed.example.com</domain>
                </domain-config>
                <domain-config>
                    <domain includeSubdomains="true">unspecified.example.com</domain>
                </domain-config>
            </network-security-config>
        """.trimIndent()
        val allowed = cleartextAllowedDomains(xml)

        assertTrue(isCleartextAllowed("open.example.com", allowed))
        assertTrue(isCleartextAllowed("18533.open.example.com", allowed))
        assertTrue(isCleartextAllowed("exact.example.com", allowed))
        assertFalse(isCleartextAllowed("sub.exact.example.com", allowed))
        assertFalse(isCleartextAllowed("closed.example.com", allowed))
        assertFalse(isCleartextAllowed("unspecified.example.com", allowed))
        assertFalse(isCleartextAllowed("notopen.example.com", allowed))
    }

    @Test
    fun `Given nested domain-config, Then a child inherits the parent cleartext setting unless it overrides`() {
        val xml = """
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain>parent.example.com</domain>
                    <domain-config>
                        <domain>inherit.example.com</domain>
                    </domain-config>
                    <domain-config cleartextTrafficPermitted="false">
                        <domain>override.example.com</domain>
                    </domain-config>
                </domain-config>
            </network-security-config>
        """.trimIndent()
        val allowed = cleartextAllowedDomains(xml)

        assertTrue(isCleartextAllowed("parent.example.com", allowed))
        assertTrue(isCleartextAllowed("inherit.example.com", allowed))
        assertFalse(isCleartextAllowed("override.example.com", allowed))
    }
}

/** 허용된 도메인과 includeSubdomains 여부. */
private data class AllowedDomain(val domain: String, val includeSubdomains: Boolean)

/**
 * cleartext 를 허용하는 `<domain-config>` 아래의 `<domain>` 만 모은다. 자식 config 가 속성을 생략하면 부모를
 * 따르고, base-config 는 보지 않는다(Android 의 병합 규칙을 전부 흉내 내지 않는 단순화다).
 */
private fun cleartextAllowedDomains(xml: String): List<AllowedDomain> {
    val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(InputSource(StringReader(xml))).documentElement
    val result = mutableListOf<AllowedDomain>()

    fun walk(config: Element, inherited: Boolean) {
        val permitted = config.getAttribute("cleartextTrafficPermitted")
            .takeIf { it.isNotEmpty() }?.toBoolean() ?: inherited
        for (i in 0 until config.childNodes.length) {
            val child = config.childNodes.item(i) as? Element ?: continue
            when (child.tagName) {
                "domain" -> if (permitted) {
                    result += AllowedDomain(
                        domain = child.textContent.trim(),
                        includeSubdomains = child.getAttribute("includeSubdomains") == "true",
                    )
                }

                "domain-config" -> walk(child, permitted)
            }
        }
    }

    for (i in 0 until root.childNodes.length) {
        val child = root.childNodes.item(i) as? Element ?: continue
        if (child.tagName == "domain-config") walk(child, inherited = false)
    }
    return result
}

private fun isCleartextAllowed(host: String, allowed: List<AllowedDomain>): Boolean =
    allowed.any { host == it.domain || (it.includeSubdomains && host.endsWith(".${it.domain}")) }
