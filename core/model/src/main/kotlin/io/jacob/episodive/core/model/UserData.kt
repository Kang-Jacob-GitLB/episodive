package io.jacob.episodive.core.model

import java.util.Locale

/**
 * @property country 기기 로케일의 국가(ISO alpha-2 대문자). 라디오 전체 목록의 기준이다.
 * 로케일에 국가가 없거나 두 글자 알파벳이 아니면 null — 한국 카탈로그로 취급한다.
 */
data class UserData(
    val isFirstLaunch: Boolean = true,
    val language: String = Locale.getDefault().language,
    val categories: List<Category> = emptyList(),
    val speed: Float = 1f,
    val isCaptionEnabled: Boolean = false,
    val country: String? = Locale.getDefault().countryOrNull(),
)

private val CountryCode = Regex("[A-Z]{2}")

/** 로케일의 국가 코드. `en`(국가 없음)이나 `419` 같은 UN M.49 지역 코드는 null. */
fun Locale.countryOrNull(): String? = country.takeIf { CountryCode.matches(it) }
