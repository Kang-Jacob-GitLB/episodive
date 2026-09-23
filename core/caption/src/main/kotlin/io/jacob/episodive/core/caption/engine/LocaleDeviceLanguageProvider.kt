package io.jacob.episodive.core.caption.engine

import java.util.Locale
import javax.inject.Inject

/** 기기 로케일 기본값을 그대로 태그로 돌려준다. 번역 대상 언어가 이 값이다. */
class LocaleDeviceLanguageProvider @Inject constructor() : DeviceLanguageProvider {
    override fun languageTag(): String = Locale.getDefault().toLanguageTag()
}
