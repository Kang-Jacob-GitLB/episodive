# sherpa-onnx 는 JNI 로 네이티브 코드에서 필드를 리플렉션으로 채운다.
# minify 는 현재 꺼져 있지만, 소비 측(app)에서 켜질 경우를 대비해 여기서 막아 둔다.
-keep class com.k2fsa.sherpa.onnx.** { *; }
