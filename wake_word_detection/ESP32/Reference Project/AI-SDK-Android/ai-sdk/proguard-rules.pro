# =============================================================================
# AI-SDK 统一混淆规则（唯一规则文件）
#
# 设计说明：
#   1. 本文件由 ai-sdk（fat-aar 宿主）在 release 混淆时应用，同时作为
#      consumerProguardFiles 下发给开启 R8 的客户 App。
#   2. 4 个被聚合的子模块（deviceAccess_gatewayProxy / aiFoundationKit /
#      asrTranslation / asrIntelligentDialogue）均已关闭单独混淆
#      （minifyEnabled false），SDK 全部代码仅在 ai-sdk 宿主处「单次混淆」，
#      以避免多次混淆造成的 mapping 断链、跨模块误裁与优化叠加。
#   3. 原则：只 keep「我们自己」对外暴露/被反射/被 JNI 绑定的内容；
#      第三方库（okhttp/gson/coroutines 等）非聚合进 AAR，依赖其自带的
#      consumer 规则，本文件仅做 -dontwarn 消警，避免拖累客户 App 裁剪。
# =============================================================================

# -----------------------------------------------------------------------------
# 0. 通用属性保留
# -----------------------------------------------------------------------------
# 泛型签名、内部类、方法异常表等：Gson 泛型解析与反射依赖
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions
# 注解信息：供 Gson @SerializedName 等运行时注解使用
-keepattributes *Annotation*,RuntimeVisibleAnnotations,AnnotationDefault
# 保留源文件名与行号，便于线上崩溃堆栈定位
-keepattributes SourceFile,LineNumberTable
# 隐藏真实源文件名（仅显示为 SourceFile），不影响行号还原
-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# 1. 对外公开 API（客户直接调用，类名/方法名不可混淆）
# -----------------------------------------------------------------------------
# SDK 初始化入口（注意 Kotlin companion 编译为内部类 $Companion）
-keep public class com.cmdc.ai.assist.AIAssistantManager { public *; }
-keep public class com.cmdc.ai.assist.AIAssistantManager$Companion { public *; }
# 全部对外能力类：GateWay / AIFoundationKit / SpeechRecognition /
# SpeechRecognitionPersistent / StreamTextProcessor / AISessionManager /
# ASRTranslation / ASRIntelligentDialogue / ASRDialogueErrorCode 等
# 注意：AIAssistantManager 通过 Class.forName(...).getDeclaredConstructor().newInstance()
# 反射创建 AIFoundationKit / ASRTranslation / ASRIntelligentDialogue 实例，
# 故必须显式保留无参/公开构造函数，避免反射实例化失败。
-keep public class com.cmdc.ai.assist.api.** {
    public <init>(...);
    public *;
}
# 对外回调接口（如 IRecordStream，客户实现）
-keep public interface com.cmdc.ai.assist.api.** { *; }

# -----------------------------------------------------------------------------
# 2. 公开数据/配置模型（constraint 包，整包保留）
# -----------------------------------------------------------------------------
# constraint 包既是 Gson 序列化模型（字段名即 JSON 键，不可改），
# 也是对外公开 API 契约：
#   - AIAssistConfig.Builder 的链式 setXxx() 方法（客户端构建配置）
#   - TtsConfig.Companion.DEFAULT（走 getDEFAULT()）
#   - 枚举属性 Language.languageCode / TranslationTypeCode.code 的 getter
#   - 各 Response 数据类的 getXxx() getter（客户端读取结果）
# 因此整包保留「字段 + 方法 + 构造 + companion + 枚举成员」，
# 仅保字段会导致上述方法被改名、客户端调用失败。
-keep class com.cmdc.ai.assist.constraint.** { *; }

# -----------------------------------------------------------------------------
# 2b. 对外业务异常（上层 App 通过 e is BizException 判型并读取 code/msg/rawJson）
# -----------------------------------------------------------------------------
# BizException 属对外 API 契约：客户端在 onError 回调里 `if (e is BizException)`
# 分流，并读取其 code/msg/rawJson。必须保留「类名 + 全部成员」，否则：
#   - 类名被改（如混淆成 e.b）→ 客户端 `e is BizException` 判型失效；
#   - 成员被改 → getCode()/getMsg()/getRawJson() 改名，客户端取不到属性。
-keep class com.cmdc.ai.assist.http.error.BizException { *; }

# -----------------------------------------------------------------------------
# 3. JNI native（C++ 端使用静态命名 Java_com_cmdc_ai_assist_native_NativeUrlProvider_*）
# -----------------------------------------------------------------------------
# 包名 + 类名 + native 方法名都必须保持原样，否则 JNI 绑定失效（UnsatisfiedLinkError）
-keep class com.cmdc.ai.assist.native.NativeUrlProvider { *; }
# 任何 native 方法签名一律保留
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
# 第二个 JNI 类：Opus 音频编解码（System.loadLibrary("duopus")，静态命名
# Java_com_baidu_voicesearch_opus_OpusTools_*）。整类保留，连同静态初始化块与
# 可能被 native 反向调用的成员，避免编解码调用异常。
-keep class com.baidu.voicesearch.opus.OpusTools { *; }

# -----------------------------------------------------------------------------
# 4. 枚举（Gson 按 name 序列化，需保留 values()/valueOf()）
# -----------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -----------------------------------------------------------------------------
# 5. Kotlin 运行时
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# -----------------------------------------------------------------------------
# 6. Gson 注解字段（后续若使用 @SerializedName 时生效）
# -----------------------------------------------------------------------------
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# -----------------------------------------------------------------------------
# 7. 第三方依赖仅消警（库自带 consumer 规则，无需大范围 keep）
# -----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.java_websocket.**
-dontwarn org.jetbrains.annotations.**

# -----------------------------------------------------------------------------
# 8. 实现/继承第三方接口的 SDK 回调类（防 AbstractMethodError）
# -----------------------------------------------------------------------------
# 这些 SDK 类被 embed 进 ai-sdk 后由宿主 R8 混淆。它们 override 了第三方库
# （okhttp / okhttp-sse / Java-WebSocket）的回调方法，如果方法名被改写，运行时
# 真库仍按原名回调，将抛出 java.lang.AbstractMethodError。故整类保留其成员，
# 确保 onResponse / onFailure / onMessage / onError / onOpen / onClosed 等
# override 方法名与签名维持原样。
# 已知触发点（7 处）：
#   1) AIFoundationKitRequestApi 匿名 okhttp3.Callback
#   2) AIFoundationKitRequestApi 匿名 okhttp3.sse.EventSourceListener（SSE 流式）
#   3) DealSotaOne 的 okhttp3.WebSocketListener
#   4~7) RtAsrClient / AsrClient / AsrClientPersistent / AsrClientPersistentAliYun
#        继承 org.java_websocket.client.WebSocketClient
-keep class * implements okhttp3.Callback { *; }
-keep class * extends okhttp3.WebSocketListener { *; }
-keep class * extends okhttp3.sse.EventSourceListener { *; }
-keep class * extends org.java_websocket.client.WebSocketClient { *; }

# -----------------------------------------------------------------------------
# 9. 语音唤醒（voiceWakeUp 模块 + 底层唤醒引擎）
# -----------------------------------------------------------------------------
# 9.1 对外唤醒封装：VoiceWakeup 由 AIAssistantManager 通过
#     Class.forName("com.cmdc.ai.assist.wakeup.VoiceWakeup").getMethod("getInstance")
#     反射获取，且 WakeupCallback / WakeupEventInfo / WakeupError / 各 config 类
#     均为对外 API 契约（客户实现回调、读写配置字段）。整包保留类名 + 成员，
#     否则反射失败或客户端取不到属性。
-keep class com.cmdc.ai.assist.wakeup.** { *; }
# 9.1b 对外语音助手引擎：VoiceAssistant 由 AIAssistantManager 通过
#      Class.forName("com.cmdc.ai.assist.voiceAssistant.VoiceAssistant").getMethod("getInstance")
#      反射获取，且 Listener / State / Config / PromptMode 均为对外 API 契约。整包保留。
-keep class com.cmdc.ai.assist.voiceAssistant.** { *; }
# 9.2 底层唤醒引擎（ai.dueros.wakeup.*，随 aar 提供）：
#     DuWakeup 单例 + WakeupCallback 回调 + config/数据类被 VoiceWakeup 直接调用，
#     且引擎内部含 JNI 绑定，整包保留以防混淆导致调用/绑定失效。
-keep class ai.dueros.wakeup.** { *; }
# 9.3 真实唤醒引擎实现 sherpa-onnx（ai.dueros.wakeup 底层依赖的 KWS 引擎）：
#     含 JNI native 方法与被 native 反向调用的类，必须整包保留。
-keep class com.k2fsa.sherpa.** { *; }

