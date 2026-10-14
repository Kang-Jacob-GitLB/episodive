package io.jacob.episodive.core.network.datasource

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.network.model.CbsScheduleItemResponse
import io.jacob.episodive.core.network.model.KbsScheduleDayResponse
import io.jacob.episodive.core.network.model.MbcScheduleResponse
import io.jacob.episodive.core.network.model.MbcSongResponse
import io.jacob.episodive.core.network.model.OnAirSongLog
import io.jacob.episodive.core.network.model.OnAirSource
import io.jacob.episodive.core.network.model.SbsScheduleItemResponse
import io.jacob.episodive.core.network.util.fetchBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/** 편성표 API 호스트. 테스트에서 MockWebServer 로 돌리기 위해 갈아끼울 수 있게 뒀다. */
data class OnAirEndpoints(
    val kbs: HttpUrl = "https://static.api.kbs.co.kr/".toHttpUrl(),
    val mbc: HttpUrl = "https://miniapi.imbc.com/".toHttpUrl(),
    val sbs: HttpUrl = "https://static.cloud.sbs.co.kr/".toHttpUrl(),
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
            val zoned = Instant.ofEpochMilli(nowMillis).atZone(SEOUL)
            val today = zoned.toLocalDate()
            // 접힌 편성표의 "다음 몇 개"가 자정 앞에서 비지 않도록, 밤이 깊으면 내일분도 받는다. 낮에는 받지 않는다.
            val late = zoned.hour >= TOMORROW_FROM_HOUR
            when (source) {
                is OnAirSource.Kbs -> kbsSchedule(source, today)
                is OnAirSource.Mbc -> mbcSchedule(source, today)
                is OnAirSource.Sbs -> sbsSchedule(source, today, late)
                is OnAirSource.Cbs -> cbsSchedule(source, today, late)
            }.sortedBy { it.startMillis }
        }

    override suspend fun currentSong(source: OnAirSource.Mbc): OnAirSongLog? = withContext(ioDispatcher) {
        val url = endpoints.mbc.newBuilder()
            .addPathSegments("music/somitem")
            .addQueryParameter("rtype", "json")
            .build()
        val rows: List<MbcSongResponse> = gson.fromJson(get(url), object : TypeToken<List<MbcSongResponse>>() {}.type)
            ?: return@withContext null
        val row = rows.firstOrNull { it.channel == source.channel } ?: return@withContext null
        val text = row.somItem
            ?.trim()
            ?.trimStart(*SONG_SYMBOLS, ' ')
            ?.takeIf { it.isNotEmpty() }
            ?: return@withContext null
        OnAirSongLog(text = text, loggedAtMillis = parseRegDate(row.regDate))
    }

    /** "yyyy-MM-dd HH:mm:ss"(Asia/Seoul). 파싱하지 못하면 null. */
    private fun parseRegDate(value: String?): Long? = runCatching {
        LocalDateTime.parse(value!!.trim().replace(' ', 'T')).atZone(SEOUL).toInstant().toEpochMilli()
    }.getOrNull()

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
                // 요청한 날(어제~오늘)이 아닌 방송일이 섞여 와도 받지 않는다. 내일 오후 편성이 목록에 새지 않게 한다.
                if (date < yesterday || date > today) return@mapNotNull null
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
        val mine = mbcRows(today).filter { it.channel == source.channel }
        // 어제 시작해 자정을 넘기는 편성이 지금 걸려 있을 수 있어 어제도 함께 펼친다.
        // 요일 편성은 이미 받은 응답이라 내일분은 공짜다.
        return listOf(today.minusDays(1), today, today.plusDays(1)).flatMap { date ->
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

    /**
     * SBS 웹 편성표가 쓰는 날짜별 정적 JSON(`schedule/Y/M/D/{Power|Love}.json`). 하루가 01:00 에 시작하고 자정 이후는
     * "24:00"·"25:00" 처럼 24 이상으로 와서, 00시대 방송은 전날 파일에 있다. 그래서 어제도 받는다.
     */
    private class MbcRaw(val dateKey: String, val rows: List<MbcScheduleResponse>)

    private var mbcRaw: MbcRaw? = null
    private val mbcMutex = Mutex()

    /**
     * MBC 편성표는 전 채널·전 요일이 한 응답(수백 KB)이라 세 채널이 공유한다. 주간 고정 편성이라 날짜가 바뀔 때만 다시
     * 받는다. 실패는 캐시하지 않는다.
     */
    private suspend fun mbcRows(today: LocalDate): List<MbcScheduleResponse> = mbcMutex.withLock {
        val dateKey = today.toString()
        mbcRaw?.takeIf { it.dateKey == dateKey }?.let { return@withLock it.rows }
        val url = endpoints.mbc.newBuilder().addPathSegments("Schedule/schedulelist").build()
        val rows: List<MbcScheduleResponse> =
            gson.fromJson(get(url), object : TypeToken<List<MbcScheduleResponse>>() {}.type) ?: emptyList()
        rows.also { mbcRaw = MbcRaw(dateKey, it) }
    }

    private suspend fun sbsSchedule(source: OnAirSource.Sbs, today: LocalDate, withTomorrow: Boolean): List<OnAirProgram> {
        val name = SBS_SCHEDULE_NAMES[source.channelId] ?: return emptyList()
        val dates = listOfNotNull(today.minusDays(1), today, today.plusDays(1).takeIf { withTomorrow })
        return dates.flatMap { date ->
            val url = endpoints.sbs.newBuilder()
                .addPathSegments("schedule/${date.year}/${date.monthValue}/${date.dayOfMonth}/$name.json")
                .build()
            // 어제·내일 파일이 없어도 오늘 편성은 보여 준다. 오늘 것이 없으면 그대로 실패로 올린다.
            val body = if (date == today) {
                get(url)
            } else {
                try {
                    get(url)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@flatMap emptyList()
                }
            }
            val items: List<SbsScheduleItemResponse> =
                gson.fromJson(body, object : TypeToken<List<SbsScheduleItemResponse>>() {}.type) ?: emptyList()
            items.mapNotNull { item ->
                val title = item.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val range = parseHhmmRange(date, item.startTime, item.endTime, colon = true)
                OnAirProgram(
                    title = title,
                    host = item.host.cleaned(),
                    imageUrl = item.image.toHttpsUrl(),
                    startMillis = range?.first,
                    endMillis = range?.second,
                    song = null,
                )
            }
        }
    }

    private suspend fun cbsSchedule(source: OnAirSource.Cbs, today: LocalDate, withTomorrow: Boolean): List<OnAirProgram> {
        // date 쿼리 없이 부르면 오늘, `?date=yyyy-MM-dd` 를 붙이면 그날이다(둘 다 POST).
        val items = cbsItems(source, null) + if (withTomorrow) {
            // 내일 것을 못 받아도 오늘 편성은 보여 준다.
            try {
                cbsItems(source, today.plusDays(1))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        val programs = items.mapNotNull { item ->
            val title = item.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val date = item.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val range = parseHhmmRange(date, item.startTime, item.endTime, colon = true)
            val image = item.program?.image
            OnAirProgram(
                title = title,
                host = item.mc.cleaned() ?: item.program?.mc.cleaned(),
                // 빈 문자열은 값이 아니다. 필드마다 정리한 뒤 이어야 radioProfil 이 비었을 때 vod 로 넘어간다.
                imageUrl = image?.radioProfil.toCbsUrl() ?: image?.vod.toCbsUrl() ?: image?.mainTop.toCbsUrl(),
                startMillis = range?.first,
                endMillis = range?.second,
                song = null,
            )
        }
        // 마지막 항목은 "23:59" 에 끝난다고 온다. 자정까지 이어지는 것으로 본다.
        return programs.map { p ->
            val end = p.endMillis
            if (end != null && isLastMinuteOfDay(end)) p.copy(endMillis = end + MINUTE_MILLIS) else p
        }
    }

    private suspend fun cbsItems(source: OnAirSource.Cbs, date: LocalDate?): List<CbsScheduleItemResponse> {
        val url = endpoints.cbs.newBuilder()
            .addPathSegments("schedule/radio/ajax")
            .apply { date?.let { addQueryParameter("date", it.toString()) } }
            .build()
        val body = callFactory.fetchBody(Request.Builder().url(url).post("".toRequestBody()).build())
        val channels: Map<String, List<CbsScheduleItemResponse>> = gson.fromJson(
            body, object : TypeToken<Map<String, List<CbsScheduleItemResponse>>>() {}.type
        ) ?: return emptyList()
        return channels[source.key].orEmpty()
    }

    private suspend fun get(url: HttpUrl): String = callFactory.fetchBody(Request.Builder().url(url).build())

    private fun isLastMinuteOfDay(millis: Long): Boolean {
        val time = Instant.ofEpochMilli(millis).atZone(SEOUL).toLocalTime()
        return time.hour == 23 && time.minute == 59
    }

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
        /** 이 시각(Asia/Seoul) 이후에는 내일 편성도 받는다. */
        const val TOMORROW_FROM_HOUR = 20
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val CBS_ORIGIN = "https://www.cbs.co.kr"
        /** SBS 편성표 파일 이름. */
        val SBS_SCHEDULE_NAMES = mapOf("S07" to "Power", "S08" to "Love")
        val SONG_SYMBOLS = charArrayOf('♬', '♪', '♩', '♫')
    }
}
