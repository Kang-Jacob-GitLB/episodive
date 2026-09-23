package io.jacob.episodive.core.caption.sherpa

import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import io.jacob.episodive.core.caption.asset.InstalledCaptionModel
import io.jacob.episodive.core.caption.engine.SpeechRecognizer
import io.jacob.episodive.core.caption.engine.SpeechRecognizerFactory
import javax.inject.Inject

/**
 * [InstalledCaptionModel] 로 sherpa-onnx `OnlineRecognizer` 를 올린다.
 *
 * `OnlineRecognizer(assetManager, config)` 의 첫 인자는 nullable 이다(1.13.8 aar 의
 * `RuntimeInvisibleParameterAnnotations` 로 확인). 모델 파일이 앱 전용 저장소의 절대경로에
 * 있으므로(`InstalledCaptionModel`) `null` 을 넘겨 `newFromFile` 경로를 태운다 —
 * `AssetManager` 를 얻으려고 `Context` 를 끌고 올 이유가 없다.
 */
class SherpaSpeechRecognizerFactory @Inject constructor() : SpeechRecognizerFactory {
    override fun create(model: InstalledCaptionModel): SpeechRecognizer {
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = FeatureSampleRate, featureDim = FeatureDim),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = model.encoderPath,
                    decoder = model.decoderPath,
                    joiner = model.joinerPath,
                ),
                tokens = model.tokensPath,
                numThreads = NumThreads,
                // "" 이면 sherpa 가 파일 셋을 보고 transducer/paraformer 등 모델 종류를 자동
                // 감지한다. 스모크 테스트도 지정하지 않았다.
                modelType = "",
            ),
            endpointConfig = EndpointConfig(
                // 말이 없는 채로 2.4초가 지나면(내용 없이도) 끝점으로 본다 — 문장 사이 긴 침묵.
                rule1 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 2.4f, minUtteranceLength = 0f),
                // 뭔가 말한 뒤 0.8초 침묵이면 끝점 — 일반적인 문장 끝.
                rule2 = EndpointRule(mustContainNonSilence = true, minTrailingSilence = 0.8f, minUtteranceLength = 0f),
                // 20초를 넘게 말이 이어지면 침묵과 무관하게 강제로 끊는다 — 화면 줄이 끝없이
                // 늘어나는 것을 막는 안전판.
                rule3 = EndpointRule(mustContainNonSilence = false, minTrailingSilence = 0f, minUtteranceLength = 20f),
            ),
            enableEndpoint = true,
            // greedy_search 로 고정한다. CaptionLineTracker 는 "이미 방출한 토큰은 다시 고치지
            // 않는다" 는 전제로 확정 줄을 잘라내는데, 빔 서치 계열은 뒤늦게 들어온 증거로 이미
            // 내보낸 가설을 바꿀 수 있어 그 전제를 깬다.
            decodingMethod = "greedy_search",
        )
        return SherpaSpeechRecognizer(OnlineRecognizer(null, config))
    }

    private companion object {
        const val FeatureSampleRate = 16_000
        const val FeatureDim = 80
        const val NumThreads = 2
    }
}
