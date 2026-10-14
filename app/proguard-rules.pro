# Feather release rules.
#
# Hilt, Room, WorkManager, Glance, DataStore and Navigation ship their own consumer rules, so only
# what this app adds on top is listed here. These come from the kotlinx.serialization documentation:
# the generated serializers of our @Serializable classes (the backup file records and the type-safe
# navigation routes) are found by name at run time.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.elkrrai.feather.**$$serializer { *; }
-keepclassmembers class com.elkrrai.feather.** {
    *** Companion;
}
-keepclasseswithmembers class com.elkrrai.feather.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager creates workers by class name from its own database.
-keep class com.elkrrai.feather.reminders.ReminderWorker { <init>(...); }
-keep class com.elkrrai.feather.data.backup.BackupWorker { <init>(...); }

# Readable crash reports from a release build (upload mapping.txt together with the app).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
