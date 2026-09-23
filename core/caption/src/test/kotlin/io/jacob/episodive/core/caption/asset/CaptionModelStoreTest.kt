package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CaptionModelStoreTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val spec = CaptionAssetTestData.spec
    private val other = CaptionAssetTestData.otherSpec

    private lateinit var root: File
    private lateinit var store: CaptionModelStore

    @Before
    fun setUp() {
        root = tmpFolder.newFolder("caption-models")
        store = CaptionModelStore(root, listOf(spec, other))
    }

    @Test
    fun `설치되지 않은 스펙은 installed 가 null`() {
        assertNull(store.installed(spec.language))
    }

    @Test
    fun `모든 최종 파일이 있고 크기가 같으면 설치된 것으로 본다`() {
        CaptionAssetTestData.writeInstalled(store, spec)

        val installed = store.installed(spec.language)

        assertTrue(store.isInstalled(spec))
        requireNotNull(installed)
        assertEquals(spec.language, installed.language)
        assertEquals(store.file(spec, spec.tokens).absolutePath, installed.tokensPath)
        assertEquals(spec.maxLineChars, installed.maxLineChars)
    }

    @Test
    fun `파일 하나라도 크기가 다르면 설치된 것이 아니다`() {
        CaptionAssetTestData.writeInstalled(store, spec)
        // tokens 파일만 크기를 어긋나게 만든다.
        store.file(spec, spec.tokens).writeBytes(ByteArray(1))

        assertFalse(store.isInstalled(spec))
        assertNull(store.installed(spec.language))
    }

    @Test
    fun `downloadedBytes 는 완성 파일과 part 파일 길이를 합친다`() {
        val dir = store.directory(spec)
        dir.mkdirs()
        store.file(spec, spec.encoder).writeBytes(ByteArray(spec.encoder.sizeBytes.toInt()))
        store.partFile(spec, spec.tokens).writeBytes(ByteArray(3))

        val downloaded = store.downloadedBytes(spec)

        assertEquals(spec.encoder.sizeBytes + 3L, downloaded)
    }

    @Test
    fun `레지스트리에 없는 디렉터리는 pruneUnregistered 가 지운다`() {
        File(root, spec.id).mkdirs()
        val stray = File(root, "stray-language").apply { mkdirs() }
        File(stray, "leftover.onnx").writeBytes(ByteArray(4))

        CaptionModelStore(root, listOf(spec)).pruneUnregistered()

        assertFalse(stray.exists())
        assertTrue(File(root, spec.id).exists())
    }

    @Test
    fun `invalidate 는 언어 디렉터리를 통째로 지운다`() {
        CaptionAssetTestData.writeInstalled(store, spec)
        assertTrue(store.isInstalled(spec))

        store.invalidate(spec.language)

        assertFalse(store.directory(spec).exists())
        assertNull(store.installed(spec.language))
    }
}
