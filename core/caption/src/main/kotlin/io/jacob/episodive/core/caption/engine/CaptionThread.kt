package io.jacob.episodive.core.caption.engine

import javax.inject.Qualifier

/**
 * 라이브 자막 인식 루프 전용 단일 스레드 디스패처. sherpa-onnx 의 네이티브 객체
 * (`OnlineRecognizer`/`OnlineStream`)는 단일 스레드에서만 써야 하므로, [LiveCaptionEngine] 의
 * 루프와 그 안에서 파생되는 모든 코루틴(번역 포함)이 이 디스패처 하나로 모인다.
 *
 * 실제 daemon 단일 스레드 executor 는 `di/CaptionModule` 이 제공한다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CaptionThread
