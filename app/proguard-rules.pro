# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.shortdrama.count.**$$serializer { *; }
-keepclassmembers class com.shortdrama.count.** {
    *** Companion;
}
-keepclasseswithmembers class com.shortdrama.count.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 数据模型
-keep class com.shortdrama.count.model.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# 保留行号便于排查
-keepattributes SourceFile,LineNumberTable
