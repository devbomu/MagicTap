# MagicTap ProGuard/R8 rules.

# kotlinx.serialization keeps generated serializers via @Serializable; keep the
# serializer lookup for our model classes.
-keepclassmembers class com.magictap.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.magictap.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.magictap.data.model.**$$serializer { *; }

# OkHttp pulls in optional platform classes that are safe to ignore.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
