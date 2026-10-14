package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.network.model.CbsScheduleItemResponse
import io.jacob.episodive.core.network.model.KbsScheduleDayResponse
import io.jacob.episodive.core.network.model.MbcScheduleResponse
import io.jacob.episodive.core.network.model.MbcSongResponse
import io.jacob.episodive.core.network.model.OnAirSource
import io.jacob.episodive.core.network.model.SbsOnAirResponse
import io.jacob.episodive.core.network.util.fetchBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 편성표 API 호스트. 테스트에서 MockWebServer 로 돌리기 위해 갈아끼울 수 있게 뒀다. */
data class OnAirEndpoints(
    val kbs: HttpUrl = "https://static.api.kbs.co.kr/".toHttpUrl(),
    val mbc: HttpUrl = "https://miniapi.imbc.com/".toHttpUrl(),
    val sbs: HttpUrl = "https://apis.sbs.co.kr/".toHttpUrl(),
    val cbs: HttpUrl = "https://www.cbs.co.kr/".toHttpUrl(),
)

/**
 * 방송사 비공식 편성표 API 파서. 시각은 전부 Asia/Seoul 로 해석해 epoch millis 로 바꾼다.
 * 자정을 넘기는 편성은 종료 시각이 시작보다 이르거나(HH:mm) 24 이상(KBS, SBS)이라 다음날로 본다.
 */
class OnAirDataSourceImpl(
    private val callFactory: Call.Factory,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher,
    private val endpoints: OnAirEndpoints = OnAirEndpoints(),
) : OnAirDataSource {

    override suspend fun schedule(source: OnAirSource, nowMillis: Long): List<OnAirProgram> =
        withContext(ioDispatcher) {
            val today = Instant.ofEpochMilli(nowMillis).atZone(SEOUL).toLocalDate()
            when (source) {
                is OnAirSource.Kbs -> kbsSchedule(source, today)
                is OnAirSource.Mbc -> mbcSchedule(source, today)
                is OnAirSource.Sbs -> sbsSchedule(source, today)
                is OnAirSource.Cbs -> cbsSchedule(source, today)
            }.sortedBy { it.startMillis }
        }

    override suspend fun currentSong(source: OnAirSource.Mbc): String? = withContext(ioDispatcher) {
        val url = endpoints.mbc.newBuilder()
            .addPathSegments("music/somitem")
            .addQueryParameter("rtype", "json")
            .build()
        val rows: List<MbcSongResponse> = gson.fromJson(get(url), object : TypeToken<List<MbcSongResponse>>() {}.type)
            ?: return@withContext null
        rows.firstOrNull { it.channel == source.channel }
            ?.somItem
            ?.trim()
            ?.trimStart(*SONG_SYMBOLS, ' ')
            ?.takeIf { it.isNotEmpty() }
    }

    private suspend fun kbsSchedule(source: OnAirSource.Kbs, today: LocalDate): List<OnAirProgram> {
        // 자정을 넘긴 전날 편성("25:00")이 지금 시각에 걸릴 수 있어 전날부터 받는다.
        val yesterday = today.minusDays(1)
        val url = endpoints.kbs.newBuilder()
            .addPathSegments("mediafactory/v1/schedule/weekly")
            .addQueryParameter("rtype", "json")
            .addQueryParameter("local_station_code", "00")
            .addQueryParameter("channel_code", source.channelCode)
            .addQueryParameter("program_planned_date_from", yesterday.compact())
            .addQueryParameter("program_planned_date_to", today.compact())
            .build()
        val days: List<KbsScheduleDayResponse> =
            gson.fromJson(get(url), object : TypeToken<List<KbsScheduleDayResponse>>() {}.type) ?: return emptyList()
        return days.flatMap { day ->
            day.schedules.orEmpty().mapNotNull { item ->
                val title = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val date = (item.date ?: day.date)?.let(::parseCompactDate) ?: today
                val start = parseKbsTime(date, item.startTime)
                val end = parseKbsTime(date, item.endTime)
                OnAirProgram(
                    title = title,
                    host = item.staff.cleaned(),
                    imageUrl = item.image.toHttpsUrl(),
                    startMillis = start,
                    endMillis = end,
                    song = null,
                )
            }
        }
    }

    private suspend fun mbcSchedule(source: OnAirSource.Mbc, today: LocalDate): List<OnAirProgram> {
        val url = endpoints.mbc.newBuilder().addPathSegments("Schedule/schedulelist").build()
        val rows: List<MbcScheduleResponse> =
            gson.fromJson(get(url), object : TypeToken<List<MbcScheduleResponse>>() {}.type) ?: return emptyList()
        val mine = rows.filter { it.channel == source.channel }
        // 어제 시작해 자정을 넘기는 편성이 지금 걸려 있을 수 있어 어제도 함께 펼친다.
        return listOf(today.minusDays(1), today).flatMap { date ->
            val label = date.dayOfWeek.koreanLabel()
            mine.filter { row -> row.liveDays?.contains(label) == true }.mapNotNull { row ->
                val title = row.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val range = parseHhmmRange(date, row.startTime, row.endTime, colon = false)
                OnAirProgram(
                    title = title,
                    host = null,
                    imageUrl = row.hdPicture.toHttpsUrl() ?: row.picture.toHttpsUrl() ?: row.thumbnailPicture.toHttpsUrl(),
                    startMillis = range?.first,
                    endMillis = range?.second,
                    song = null,
                )
            }
        }
    }

    private suspend fun sbsSchedule(source: OnAirSource.Sbs, today: LocalDate): List<OnAirProgram> {
        val url = endpoints.sbs.newBuilder()
            .addPathSegments("play-api/1.0/onair/channels")
            .addQueryParameter("v_type", "2")
            .build()
        val channel = gson.fromJson(get(url), SbsOnAirResponse::class.java)
            ?.list
            ?.firstOrNull { it.channelId == source.channelId }
            ?: return emptyList()
        val title = channel.title?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val range = parseHhmmRange(today, channel.startTime, channel.endTime, colon = true)
        // subtitle 은 "SBS" 고정이라 진행자로 쓰지 않는다.
        return listOf(
            OnAirProgram(
                title = title,
                host = null,
                imageUrl = channel.thumbs?.get("1280").toHttpsUrl()
                    ?: channel.thumbs?.get("640").toHttpsUrl()
                    ?: channel.thumbImg.toHttpsUrl(),
                startMillis = range?.first,
                endMillis = range?.second,
                song = null,
            )
        )
    }

    private suspend fun cbsSchedule(source: OnAirSource.Cbs, today: LocalDate): List<OnAirProgram> {
        val url = endpoints.cbs.newBuilder().addPathSegments("schedule/radio/ajax").build()
        val body = callFactory.fetchBody(Request.Builder().url(url).post("".toRequestBody()).build())
        val channels: Map<String, List<CbsScheduleItemResponse>> = gson.fromJson(
            body, object : TypeToken<Map<String, List<CbsScheduleItemResponse>>>() {}.type
        ) ?: return emptyList()
        return channels[source.key].orEmpty().mapNotNull { item ->
            val title = item.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val date = item.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val range = parseHhmmRange(date, item.startTime, item.endTime, colon = true)
            val image = item.program?.image
            OnAirProgram(
                title = title,
                host = item.mc.cleaned() ?: item.program?.mc.cleaned(),
                imageUrl = (image?.radioProfil ?: image?.vod ?: image?.mainTop).toCbsUrl(),
                startMillis = range?.first,
                endMillis = range?.second,
                song = null,
            )
        }
    }

    private suspend fun get(url: HttpUrl): String = callFactory.fetchBody(Request.Builder().url(url).build())

    private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** https 만 통과시킨다. http 는 https 로 바꾸고 그 외 스킴·빈 값은 버린다. */
    private fun String?.toHttpsUrl(): String? {
        val url = cleaned() ?: return null
        return when {
            url.startsWith("https://") -> url
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            else -> null
        }
    }

    private fun String?.toCbsUrl(): String? {
        val path = cleaned() ?: return null
        return if (path.startsWith("/")) (CBS_ORIGIN + path) else path.toHttpsUrl()
    }

    private fun LocalDate.compact(): String = "%04d%02d%02d".format(year, monthValue, dayOfMonth)

    private fun parseCompactDate(value: String): LocalDate? {
        if (value.length != 8) return null
        return runCatching {
            LocalDate.of(value.substring(0, 4).toInt(), value.substring(4, 6).toInt(), value.substring(6, 8).toInt())
        }.getOrNull()
    }

    /** "HHmmssff". HH 가 24 이상이면 [date] 다음날로 넘어간 것이다. */
    private fun parseKbsTime(date: LocalDate, value: String?): Long? {
        if (value == null || value.length < 4 || !value.all(Char::isDigit)) return null
        val hour = value.substring(0, 2).toLong()
        val minute = value.substring(2, 4).toLong()
        val second = if (value.length >= 6) value.substring(4, 6).toLong() else 0L
        return date.atStartOfDay(SEOUL).toInstant().toEpochMilli() + (hour * 3600 + minute * 60 + second) * 1000
    }

    /** 시작·종료 한 쌍. 종료가 시작보다 이르거나 같으면 종료를 다음날로 본다. 파싱 실패면 null. */
    private fun parseHhmmRange(date: LocalDate, start: String?, end: String?, colon: Boolean): Pair<Long, Long>? {
        val s = parseHhmm(date, start, colon) ?: return null
        var e = parseHhmm(date, end, colon) ?: return null
        if (e <= s) e += DAY_MILLIS
        return s to e
    }

    private fun parseHhmm(date: LocalDate, value: String?, colon: Boolean): Long? {
        val text = value?.trim() ?: return null
        val (h, m) = if (colon) {
            val parts = text.split(':')
            if (parts.size != 2) return null
            parts[0] to parts[1]
        } else {
            if (text.length != 4) return null
            text.substring(0, 2) to text.substring(2, 4)
        }
        val hour = h.toIntOrNull() ?: return null
        val minute = m.toIntOrNull() ?: return null
        return date.atStartOfDay(SEOUL).toInstant().toEpochMilli() + (hour * 3600L + minute * 60L) * 1000
    }

    private fun DayOfWeek.koreanLabel(): String = when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val CBS_ORIGIN = "https://www.cbs.co.kr"
        val SONG_SYMBOLS = charArrayOf('♬', '♪', '♩', '♫')
    }
}
