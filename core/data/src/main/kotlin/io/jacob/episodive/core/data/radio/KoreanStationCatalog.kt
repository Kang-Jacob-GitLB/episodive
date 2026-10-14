package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.network.model.BroadcasterSource

/**
 * 국내 라디오 큐레이션 카탈로그. Radio Browser 원본을 그대로 쓰지 않고 아래 기준으로 걸러 옮겼다.
 *
 * - 같은 방송이 여러 번 등록된 항목은 하나로 합치고, 등록 uuid 는 [StationCatalogEntry.radioBrowserIds]
 *   에 후보로 모두 남긴다(앞에서부터 시도).
 * - 제3자 중계(radio.bsod.kr, qtfm.cn, serpent0.duckdns.org)는 제외한다. 방송사 공식 스트림이 아니다.
 * - 오분류는 제외한다(LA 한인방송 K-RADIO, 쿠란 채널 등).
 * - 북한 방송은 제외한다.
 * - 인터넷 개인 방송은 v1 에서 제외한다.
 * - KBS 3라디오는 공식 스트림 등록이 없어 제외한다.
 * - 토큰이 붙은 URL(KBS·MBC·SBS·CPBC·지역 MBC)은 만료되므로 [StationCatalogEntry.fallbackUrl] 을
 *   두지 않는다. 항상 재생 직전에 새로 받는다. KBS·MBC·SBS 전국 채널은 방송사 자체 API
 *   ([StationCatalogEntry.broadcasterSource])에서, 나머지는 Radio Browser 에서 받는다(Radio Browser 가
 *   주는 서명 URL 은 크롤 시점 토큰이라 이미 만료돼 있는 경우가 많다).
 * - "KBS 1Radio" 로 등록된 항목 하나는 실제로 2라디오 주소라 해피FM 으로 분류했다.
 * - http fallback 호스트의 cleartext 허용은 network_security_config 에 추가했고
 *   KoreanStationCatalogTest 의 불변식 테스트가 지킨다(IP 호스트 포함). 새 http 항목을 더하면
 *   그 호스트도 함께 허용해야 테스트가 통과한다.
 *
 * TV 는 모델에는 있으나 v1 카탈로그에는 넣지 않는다.
 */
internal object KoreanStationCatalog {
    val entries: List<StationCatalogEntry> = listOf(
        StationCatalogEntry(
            station = Station(
                id = "kbs-1radio",
                name = "KBS 1라디오",
                broadcaster = "KBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "6d4a1b58-b7c6-11e9-acb2-52543be04c81",
                "16c1e8aa-1e9e-422b-9611-842b61a70983",
            ),
            broadcasterSource = BroadcasterSource.Kbs("21"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "kbs-happyfm",
                name = "KBS 해피FM",
                broadcaster = "KBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "9b3576ec-7f67-11e9-aa30-52543be04c81",
                "8dc941a6-c866-4552-9fea-541c137cfb5d",
            ),
            broadcasterSource = BroadcasterSource.Kbs("22"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "kbs-classicfm",
                name = "KBS 클래식FM",
                broadcaster = "KBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "e3d4515f-eb35-40e2-aeb9-dc48e9ba910c",
                "d197d3eb-87b2-48f9-8624-d96d17c3b023",
                "fe39221a-c1de-4c2f-a9ca-28a916f96010",
                "c038a859-7be1-11e9-aa30-52543be04c81",
                "d52bcf11-db1b-44d7-be0d-c37dad6bb568",
            ),
            broadcasterSource = BroadcasterSource.Kbs("24"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "kbs-coolfm",
                name = "KBS 쿨FM",
                broadcaster = "KBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "d06a6392-127d-40f8-b52d-fc610bc97178",
                "5cfee8db-7be2-11e9-aa30-52543be04c81",
                "2672e98d-b642-456e-8d77-e331ab4e007c",
                "d5642b73-a600-4fdd-9a07-a63116b04e38",
            ),
            broadcasterSource = BroadcasterSource.Kbs("25"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "mbc-sfm",
                name = "MBC 표준FM",
                broadcaster = "MBC",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "fc4c247a-7f66-11e9-aa30-52543be04c81",
            ),
            broadcasterSource = BroadcasterSource.Mbc("sfm"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "mbc-fm4u",
                name = "MBC FM4U",
                broadcaster = "MBC",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "730d9f95-57a3-4eec-94a2-ed270baa64c8",
                "8f6a0b47-7be2-11e9-aa30-52543be04c81",
            ),
            broadcasterSource = BroadcasterSource.Mbc("mfm"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "mbc-allthatmusic",
                name = "MBC 올댓뮤직",
                broadcaster = "MBC",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "01335461-d022-4ad8-9fbc-1bd3ef534def",
            ),
            broadcasterSource = BroadcasterSource.Mbc("chm"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "sbs-powerfm",
                name = "SBS 파워FM",
                broadcaster = "SBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "1cc29b79-7be3-11e9-aa30-52543be04c81",
            ),
            broadcasterSource = BroadcasterSource.Sbs("powerpc/powerfm"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "sbs-lovefm",
                name = "SBS 러브FM",
                broadcaster = "SBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "0fc7173f-5106-4c47-9156-621124fb0803",
                "e6bc48f7-7be2-11e9-aa30-52543be04c81",
            ),
            broadcasterSource = BroadcasterSource.Sbs("lovepc/lovefm"),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "cbs-sfm",
                name = "CBS 표준FM",
                broadcaster = "CBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "3058649c-068d-43e8-8c47-4a5a89dbdc4c",
            ),
            isHls = true,
            fallbackUrl = "https://m-aac.cbs.co.kr/mweb_cbs981/_definst_/cbs981.stream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "cbs-musicfm",
                name = "CBS 음악FM",
                broadcaster = "CBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "3043d560-4358-4583-9e29-9fce6777a09c",
                "015f47d3-16d1-40de-bd07-e9686d329108",
                "018935b5-88de-46da-a323-f35b4178503b",
            ),
            isHls = true,
            fallbackUrl = "https://m-aac.cbs.co.kr/cbs939/_definst_/cbs939.stream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "cbs-joy4u",
                name = "CBS JOY4U",
                broadcaster = "CBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "9b8512b7-436d-4da8-8777-633ad9b6671f",
            ),
            isHls = true,
            fallbackUrl = "https://m-aac.cbs.co.kr/mweb_cbscmc/_definst_/cbscmc.stream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "ebs-fm",
                name = "EBS FM",
                broadcaster = "EBS",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "4540380c-8d7c-49da-b0dc-699c1fb30ebc",
                "b666f9d5-126e-48ce-8826-81fcfb065485",
            ),
            isHls = true,
            fallbackUrl = "https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "ytn-radio",
                name = "YTN 라디오",
                broadcaster = "YTN",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "10fbdfdd-2a6f-49fb-a035-8963c57f52e8",
            ),
            isHls = true,
            fallbackUrl = "https://radiolive.ytn.co.kr/radio/_definst_/20211118_fmlive/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "tbs-fm",
                name = "TBS FM",
                broadcaster = "TBS",
                kind = StationKind.RADIO,
                region = "서울",
            ),
            radioBrowserIds = listOf(
                "6ad5927a-7ef7-456c-ad54-059873ff9f28",
            ),
            isHls = true,
            fallbackUrl = "https://cdnfm.tbs.seoul.kr/tbs/_definst_/tbs_fm_web_360.smil/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "tbs-efm",
                name = "TBS eFM",
                broadcaster = "TBS",
                kind = StationKind.RADIO,
                region = "서울",
            ),
            radioBrowserIds = listOf(
                "e89958f1-bc76-44fe-b681-841b5ec4ceae",
            ),
            isHls = true,
            fallbackUrl = "https://cdnefm.tbs.seoul.kr/tbs/_definst_/tbs_efm_web_360.smil/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "gugak-fm",
                name = "국악방송",
                broadcaster = "국악방송",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "2f47d2d9-ac17-49cd-9e1c-b5b295800e37",
                "e849a616-e2ff-4427-a2f6-a1071d2a073a",
                "c72c8602-5359-4b23-95a8-a7c2b4d067de",
                "7ecf4a00-7be0-11e9-aa30-52543be04c81",
            ),
            isHls = true,
            fallbackUrl = "https://mgugaklive.nowcdn.co.kr/gugakradio/gugakradio.stream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "kookbang-fm",
                name = "국방FM",
                broadcaster = "국방홍보원",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "3df9fa02-ae92-4423-8f01-e007129c00ee",
            ),
            isHls = true,
            fallbackUrl = "https://mediaworks.dema.mil.kr/live_edge/audio.sdp/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "cpbc-radio",
                name = "CPBC 가톨릭평화방송",
                broadcaster = "CPBC",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "38b5da3e-1859-47f3-9fb3-38e34741a84a",
            ),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "febc-seoul",
                name = "극동방송 서울",
                broadcaster = "극동방송",
                kind = StationKind.RADIO,
                region = "서울",
            ),
            radioBrowserIds = listOf(
                "a5e6a0b8-b68e-4931-80c6-1861299fad86",
            ),
            isHls = true,
            fallbackUrl = "http://mlive2.febc.net:1935/live/seoulfm/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "febc-jeju",
                name = "극동방송 제주",
                broadcaster = "극동방송",
                kind = StationKind.RADIO,
                region = "제주",
            ),
            radioBrowserIds = listOf(
                "e9307192-2116-4d23-a2a2-93a0e662150a",
            ),
            isHls = true,
            fallbackUrl = "http://mlive2.febc.net:1935/live/jejufm/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "ifm",
                name = "경인방송",
                broadcaster = "경인방송",
                kind = StationKind.RADIO,
                region = "경기·인천",
            ),
            radioBrowserIds = listOf(
                "d9db0a16-9100-4917-b5f5-bd48a42a1e70",
            ),
            isHls = true,
            fallbackUrl = "https://stream.ifm.kr/live/aod1/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "obs-radio",
                name = "OBS 라디오",
                broadcaster = "OBS",
                kind = StationKind.RADIO,
                region = "경기·인천",
            ),
            radioBrowserIds = listOf(
                "c60ab0ce-723c-43a9-85a4-ff544710f27f",
            ),
            isHls = true,
            fallbackUrl = "https://vod3.obs.co.kr:444/live/obsstream1/radio.stream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "tbn-gyeongin",
                name = "TBN 경인교통방송",
                broadcaster = "TBN",
                kind = StationKind.RADIO,
                region = "경기·인천",
            ),
            radioBrowserIds = listOf(
                "f9ab75ae-a4ce-440a-8b29-1dab8ba76f5c",
            ),
            isHls = true,
            fallbackUrl = "http://radio2.tbn.or.kr:1935/gyeongin/myStream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "tbn-ulsan",
                name = "TBN 울산교통방송",
                broadcaster = "TBN",
                kind = StationKind.RADIO,
                region = "울산",
            ),
            radioBrowserIds = listOf(
                "6240924d-bc3a-4cd6-8e09-399dbdeb0a36",
            ),
            isHls = true,
            fallbackUrl = "http://radio2.tbn.or.kr:1935/ulsan/myStream/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "arirang-radio",
                name = "아리랑 라디오",
                broadcaster = "아리랑국제방송",
                kind = StationKind.RADIO,
                region = null,
            ),
            radioBrowserIds = listOf(
                "dad75d66-3246-434f-b16c-83854fe573a0",
                "03750ff6-6222-4672-8817-864830fac0eb",
                "00f532d5-7b4b-4f6f-8195-bef905df50ab",
            ),
            isHls = true,
            fallbackUrl = "http://amdlive.ctnd.com.edgesuite.net/arirang_3ch/smil:arirang_3ch.smil/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "afn-humphreys",
                name = "AFN 험프리스 The Eagle",
                broadcaster = "AFN",
                kind = StationKind.RADIO,
                region = "평택",
            ),
            radioBrowserIds = listOf(
                "1ade2300-f75f-4dcd-aa77-06d276537c0e",
            ),
            isHls = false,
            fallbackUrl = "http://18533.live.streamtheworld.com/AFNP_OSNAAC_SC",
        ),
        StationCatalogEntry(
            station = Station(
                id = "mbc-chungbuk-sfm",
                name = "MBC충북 표준FM",
                broadcaster = "MBC충북",
                kind = StationKind.RADIO,
                region = "충북",
            ),
            radioBrowserIds = listOf(
                "3a6cf532-d730-4b92-91bf-f3d5434a2159",
            ),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "mbc-chungbuk-fm4u",
                name = "MBC충북 FM4U",
                broadcaster = "MBC충북",
                kind = StationKind.RADIO,
                region = "충북",
            ),
            radioBrowserIds = listOf(
                "db7b598f-2428-4b7f-bae3-677091ab4648",
            ),
            isHls = true,
            fallbackUrl = null,
        ),
        StationCatalogEntry(
            station = Station(
                id = "andong-mbc-am",
                name = "안동MBC 표준FM",
                broadcaster = "안동MBC",
                kind = StationKind.RADIO,
                region = "경북",
            ),
            radioBrowserIds = listOf(
                "1814c6bf-528b-446d-8f40-f5261a2b3b7b",
            ),
            isHls = true,
            fallbackUrl = "http://andong.webcasting.co.kr:1935/live/amlive/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "andong-mbc-fm4u",
                name = "안동MBC FM4U",
                broadcaster = "안동MBC",
                kind = StationKind.RADIO,
                region = "경북",
            ),
            radioBrowserIds = listOf(
                "b2b7599e-16b9-4225-9ffa-263b0cb541a6",
            ),
            isHls = true,
            fallbackUrl = "https://live.andongmbc.co.kr/live/fmlive/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "befm",
                name = "부산영어방송 BeFM",
                broadcaster = "BeFM",
                kind = StationKind.RADIO,
                region = "부산",
            ),
            radioBrowserIds = listOf(
                "0cbefa4e-77f0-4b98-bfdf-37a32aeb0c8b",
            ),
            isHls = true,
            fallbackUrl = "http://befm905.live.smilecdn.com:1935/befm905_live/live/playlist.m3u8",
        ),
        StationCatalogEntry(
            station = Station(
                id = "jeonju-fm",
                name = "전주FM",
                broadcaster = "전주공동체라디오",
                kind = StationKind.RADIO,
                region = "전북",
            ),
            radioBrowserIds = listOf(
                "1a512cf7-f77c-49c0-b42d-4e3db0656b6a",
            ),
            isHls = false,
            fallbackUrl = "http://radiostream.communityradio.kr/jcfm1",
        ),
        StationCatalogEntry(
            station = Station(
                id = "mapo-fm",
                name = "마포FM",
                broadcaster = "마포FM",
                kind = StationKind.RADIO,
                region = "서울",
            ),
            radioBrowserIds = listOf(
                "1c5e70a7-491a-455a-b1b3-48f4b8241155",
                "875ed109-2dd2-47e4-b9b3-3fd10ca3d8ba",
            ),
            isHls = false,
            fallbackUrl = "http://onair.mapofm.net/mapofm",
        ),
        StationCatalogEntry(
            station = Station(
                id = "seongseo-fm",
                name = "성서공동체FM",
                broadcaster = "성서공동체FM",
                kind = StationKind.RADIO,
                region = "대구",
            ),
            radioBrowserIds = listOf(
                "4edaf384-7c48-4852-a390-c879e2fc41aa",
            ),
            isHls = false,
            fallbackUrl = "http://115.85.182.39/scnfm",
        ),
        StationCatalogEntry(
            station = Station(
                id = "sone-fm",
                name = "수원 SoneFM",
                broadcaster = "수원공동체라디오",
                kind = StationKind.RADIO,
                region = "경기",
            ),
            radioBrowserIds = listOf(
                "7702bb1f-26bf-4df6-9209-5f10fb847058",
            ),
            isHls = false,
            fallbackUrl = "http://115.85.182.39/sonefm",
        ),
    )
}
