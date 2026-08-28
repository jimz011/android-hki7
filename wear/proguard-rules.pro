# kotlinx.serialization ships its own consumer R8 rules, which keep the generated serializer for
# every class that is actually reachable. A blanket keep across com.jimz011apps.hki7.** defeats
# that: it pins every @Serializable model in :core into the watch APK, including the dashboard
# config the watch never touches.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
