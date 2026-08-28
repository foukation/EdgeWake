# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#避免混淆一键登录SDK
-dontwarn com.cmic.sso.sdk.**
-keep class com.cmic.sso.sdk.** {*;}
# OkHttp 3.x 及以上版本混淆规则
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**

# OkHttp 依赖的 Okio 库
-keep class com.squareup.okio.** { *; }
-keep interface com.squareup.okio.** { *; }
-dontwarn com.squareup.okio.**

# 如果使用了 Retrofit（常与 OkHttp 配合使用），也需要保留
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Exceptions

# 保留 Retrofit 接口和注解（如果用到）
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# 保留所有 Activity 类及其构造方法
-keep public class * extends android.app.Activity {
    public <init>();
}

# 保留 Service、BroadcastReceiver、ContentProvider
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# 保留 Fragment（若使用）
-keep public class * extends android.app.Fragment
# 保留注解
-keepattributes *Annotation*
-keepattributes Signature

# 保留资源相关类（如 R 文件）
-keep class **.R$* {
    public static <fields>;
}

-keep class com.fxzs.lingxiagent.model.** { *; }
-keep class com.fxzs.lingxiagent.viewmodel.** { *; }
-keep class com.fxzs.lingxiagent.view.** { *; }
-keep class com.fxzs.lingxiagent.network.** { *; }
# 兜底：保留所有 Activity 子类（防止遗漏其他页面）
-keep public class * extends android.app.Activity {
    public <init>();
    public void onCreate(android.os.Bundle);
    public void startActivity(android.content.Intent); # 保留启动 Activity 的关键方法
    public void startActivity(android.content.Intent, android.os.Bundle);
}
-keep public class * extends androidx.appcompat.app.AppCompatActivity {
     public <init>();
     public void onCreate(android.os.Bundle);
     public void startActivity(android.content.Intent); # 保留启动 Activity 的关键方法
     public void startActivity(android.content.Intent, android.os.Bundle);
 }
 # 保留 Gson 相关的类型信息（关键）
 -keepattributes Signature
 -keepattributes *Annotation*
 -keep class com.google.gson.** { *; }
 -dontwarn com.google.gson.**
 # 保留抽象类（日志中提到的 com.cmdc.ai.assist.constraint.d）
 # 注意：若类名被混淆，需根据 mapping.txt 还原真实类名
 -keep public abstract class com.cmdc.ai.assist.constraint.* {
     <fields>;
     <methods>;
 }

 # 保留其所有实现类
 -keep public class * extends com.cmdc.ai.assist.constraint.* {
     public <init>(); # 保留无参构造，Gson 需要通过反射创建实例
     <fields>;
     <methods>;
 }
 # 保留整个包下的所有类（包括子包），不进行混淆
 -keep class com.cmdc.ai.assist.*.** {
     *; # 保留所有字段和方法
 }
 # 对实现 Serializable 的类，保留 serialVersionUID 字段
 -keepclassmembers class * implements java.io.Serializable {
     static final long serialVersionUID;
     private static final java.io.ObjectStreamField[] serialPersistentFields;
     private void writeObject(java.io.ObjectOutputStream);
     private void readObject(java.io.ObjectInputStream);
     java.lang.Object writeReplace();
     java.lang.Object readResolve();
 }

 -keep public class * extends androidx.fragment.app.Fragment {
     public <init>();
 }

-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public void onCreate(android.os.Bundle);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onDestroyView();
}
-keep public class com.fxzs.lingxiagent.view.chat.SuperChatFragment {
    public <init>();
    <fields>;
    <methods>;
}
# 保留 Gson TypeToken 及其子类的泛型签名
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken {
    <init>(...);
}

# 保留泛型签名（关键）：防止 R8 移除类/方法的泛型信息
-keepattributes Signature
-keepattributes *Annotation*

# 若使用 Gson 解析自定义实体类，需保留实体类的泛型（如有）
# 示例：假设你的实体类在 com.fxzs.lingxiagent.entity 包下
-keep class com.fxzs.lingxiagent.entity.** {
    <fields>;
    <methods>;
}
-keep class com.fxzs.lingxiagent.viewmodel.chat.** {
    <fields>;
    <methods>;
}
-keep class com.fxzs.lingxiagent.viewmodel.chat.** {
    <fields>;
    <methods>;
}
# 保留 ChatFlowController 类及所有成员（方法、字段、内部类）
-keep class com.fxzs.lingxiagent.viewmodel.chat.flow.ChatFlowController {
    *; # 通配符表示保留所有成员
}

# 保留其内部类（若有，如 lambda 相关的匿名内部类 b）
-keep class com.fxzs.lingxiagent.viewmodel.chat.flow.ChatFlowController$* {
    *;
}
-keep class com.fxzs.lingxiagent.lingxi.lingxi_conversation.* {
    *;
}

# 方式2：保留所有使用了 Gson 注解的类（如 @SerializedName）
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclasseswithmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留类的成员信息（构造方法、字段等，Gson 反射需要）
-keepclassmembers class * {
    *;
}

# 如果使用Gson的@SerializedName注解，还需要保持注解
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.fxzs.lingxiagent.lingxi.lingxi_sys_controller.* {
    *;
}
##########################################################
# Apache POI / XMLBeans 混淆保护规则 (for poi-ooxml 5.2.0)
##########################################################

# 保留 Apache POI 主包
-keep class org.apache.poi.** { *; }

# 保留 XMLBeans 的核心类与资源
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

# 保留类成员（反射需要）
-keepclassmembers class org.apache.xmlbeans.** { *; }
-keepclassmembers class org.openxmlformats.schemas.** { *; }
-keepclassmembers class schemaorg_apache_xmlbeans.** { *; }

# 避免移除内部匿名类
-keepattributes InnerClasses,EnclosingMethod

# 禁止优化和压缩这些包（否则 .xsb 文件丢失）
-dontshrink
-dontoptimize

# 忽略无用警告
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn schemaorg_apache_xmlbeans.**

# 防止 POI 在反射时找不到属性
-keepattributes *Annotation*,Signature,Exceptions,SourceFile,LineNumberTable

# 保留可能通过反射加载的类型
-keep class com.microsoft.schemas.** { *; }
-keep class org.etsi.uri.** { *; }

##########################################################
# Optional: 只要不报错，可以再微调为：
# -keep class org.apache.poi.xwpf.** { *; }  // Word 部分
# -keep class org.apache.poi.xssf.** { *; }  // Excel 部分
##########################################################


##########################################################
# iText 7 混淆配置 (for com.itextpdf:itext7-core:7.2.3)
##########################################################

# 保留所有 iText 7 的核心类
-keep class com.itextpdf.** { *; }

# 保留字体相关类（否则 PdfFontFactory.createFont() 可能失败）
-keep class com.itextpdf.io.font.** { *; }

# 保留 kernel 模块
-keep class com.itextpdf.kernel.** { *; }

# 保留 layout 模块（段落、表格等）
-keep class com.itextpdf.layout.** { *; }

# 保留 pdfa、pdfhtml、pdftools 模块（如果有用到）
-keep class com.itextpdf.pdfa.** { *; }
-keep class com.itextpdf.html2pdf.** { *; }
-keep class com.itextpdf.kernel.pdf.** { *; }

# 保留事件系统类（防止事件监听反射错误）
-keep class com.itextpdf.kernel.events.** { *; }

# 保留反射调用所需注解
-keepattributes *Annotation*

# 保留字体与元数据信息
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable

# 不优化、不删除这些类（部分为内部引用）
-dontshrink
-dontoptimize

# 屏蔽多余的警告
-dontwarn com.itextpdf.**
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
-keep class * implements com.itextpdf.kernel.events.IEventHandler { *; }
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Lifecycle
-keep class androidx.lifecycle.** { *; }
# Lifecycle 注解（防止反射丢失）
-keepclassmembers class * {
    @androidx.lifecycle.* <methods>;
}

# sherpa-onnx KeywordSpotter - 禁止混淆 JNI 访问的类
-keep class com.k2fsa.sherpa.onnx.KeywordSpotter { *; }
-keep class com.k2fsa.sherpa.onnx.KeywordSpotterConfig { *; }
-keep class com.k2fsa.sherpa.onnx.OnlineStream { *; }
-keep class com.k2fsa.sherpa.onnx.FeatureConfig { *; }
-keep class com.k2fsa.sherpa.onnx.OnlineModelConfig { *; }
-keep class com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig { *; }

# 保守策略：直接 keep 整个 sherpa.onnx 包
-keep class com.k2fsa.sherpa.onnx.** { *; }




