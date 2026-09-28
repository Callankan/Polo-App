# Rutas de navegación y modelos de copia de seguridad (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers @kotlinx.serialization.Serializable class com.callankan.poloapp.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.callankan.poloapp.**$$serializer { *; }
-keepclasseswithmembers class com.callankan.poloapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.callankan.poloapp.navigation.** { *; }
-keep class com.callankan.poloapp.data.backup.** { *; }
-keep class com.callankan.poloapp.data.db.entity.** { *; }
-keep enum com.callankan.poloapp.** { *; }

# Glance widget receiver
-keep class com.callankan.poloapp.widget.** { *; }
