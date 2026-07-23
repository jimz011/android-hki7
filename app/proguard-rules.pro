# HKI 7 R8 / ProGuard rules.
#
# Most libraries here (Ktor client, Coil 3, AndroidX, Compose) ship their own consumer rules,
# so the app-specific keeps below are almost entirely about kotlinx.serialization, whose
# generated $$serializer classes and Companion serializers are discovered by name.

# Keep line numbers for readable crash reports; hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- kotlinx.serialization ----------------------------------------------------------------
# (Canonical rules from the kotlinx.serialization README, plus a belt-and-braces keep of this
#  app's model package because every HA websocket payload is deserialized through it.)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep every generated serializer.
-keepclassmembers class **$$serializer { *; }

# Keep the Companion of every @Serializable class so its serializer() can be resolved.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# The app's serialized model classes and their serializers, kept whole to avoid any
# reflective-lookup surprises across the ~50 @Serializable types in data/.
-keep,includedescriptorclasses class com.jimz011apps.hki7.data.**$$serializer { *; }
-keepclassmembers class com.jimz011apps.hki7.data.** {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class com.jimz011apps.hki7.** { *; }

# ---- Coroutines ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
