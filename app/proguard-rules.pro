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

# Retrofit/Gson use annotations and generic signatures at runtime. Preserve the
# metadata and the JSON field names while allowing the remaining implementation
# classes and methods to be optimized and obfuscated by R8.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

-keepclassmembers,allowoptimization class com.rdfonseca.iwebit.data.remote.dto.** {
    <fields>;
}

-keepclassmembers,allowoptimization class com.rdfonseca.iwebit.domain.model.** {
    <fields>;
}

-keepclassmembers,allowoptimization class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
