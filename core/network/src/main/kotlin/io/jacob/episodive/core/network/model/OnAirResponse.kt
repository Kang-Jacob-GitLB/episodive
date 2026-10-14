package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class KbsScheduleDayResponse(
    @SerializedName("program_planned_date") val date: String? = null,
    @SerializedName("schedules") val schedules: List<KbsScheduleResponse>? = null,
)

data class KbsScheduleResponse(
    @SerializedName("program_title") val title: String? = null,
    @SerializedName("program_staff") val staff: String? = null,
    @SerializedName("image_w") val image: String? = null,
    @SerializedName("program_planned_date") val date: String? = null,
    @SerializedName("program_planned_start_time") val startTime: String? = null,
    @SerializedName("program_planned_end_time") val endTime: String? = null,
)

data class MbcScheduleResponse(
    @SerializedName("Channel") val channel: String? = null,
    @SerializedName("ProgramTitle") val title: String? = null,
    @SerializedName("StartTime") val startTime: String? = null,
    @SerializedName("EndTime") val endTime: String? = null,
    @SerializedName("LiveDays") val liveDays: String? = null,
    @SerializedName("HDPicture") val hdPicture: String? = null,
    @SerializedName("Picture") val picture: String? = null,
    @SerializedName("ThumbnailPicture") val thumbnailPicture: String? = null,
)

data class MbcSongResponse(
    @SerializedName("Channel") val channel: String? = null,
    @SerializedName("SomItem") val somItem: String? = null,
)

data class SbsScheduleItemResponse(
    @SerializedName("title") val title: String? = null,
    @SerializedName("bora_title") val host: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("program_image") val image: String? = null,
)

data class CbsScheduleItemResponse(
    @SerializedName("name") val name: String? = null,
    @SerializedName("mc") val mc: String? = null,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("program") val program: CbsProgramResponse? = null,
)

data class CbsProgramResponse(
    @SerializedName("mc") val mc: String? = null,
    @SerializedName("image") val image: CbsImageResponse? = null,
)

data class CbsImageResponse(
    @SerializedName("radioProfil") val radioProfil: String? = null,
    @SerializedName("vod") val vod: String? = null,
    @SerializedName("mainTop") val mainTop: String? = null,
)
