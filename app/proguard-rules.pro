# Quick Clean PRO release shrink baseline.
# Keep this file brand-neutral where possible so copied shell apps can reuse it.

# Kotlin, generic signatures, annotations and inner classes are used by Koin,
# Compose, SDK builders, callbacks and reflective platform integrations.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,AnnotationDefault

# Keep anything explicitly marked for framework or SDK reflection.
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <init>(...);
}

# Android framework entry points. Prefer inheritance-based rules over concrete
# package names so variant apps can change applicationId/package names safely.
-keep class * extends android.app.Application { public <init>(); }
-keep class * extends android.app.Activity { public <init>(); }
-keep class * extends android.app.Service { public <init>(); }
-keep class * extends android.content.BroadcastReceiver { public <init>(); }
-keep class * extends android.content.ContentProvider { public <init>(); }
-keep class * extends android.service.notification.NotificationListenerService { public <init>(); }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { public <init>(); }
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
-keepclassmembers class * extends androidx.work.ListenableWorker { public <init>(...); }

# WorkManager builds its internal Room database through generated *_Impl classes.
# Some transitive ad SDK versions still bring older Work/Room consumer rules, so
# keep the startup database path intact for minified release APKs.
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.WorkDatabaseMigrations { *; }
-keep class androidx.work.impl.WorkDatabasePathHelper { *; }
-keep class androidx.work.impl.model.** { *; }
-keep class androidx.room.Room { *; }
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.RoomDatabase$* { *; }
-keep class androidx.room.DatabaseConfiguration { *; }
-keep class androidx.room.InvalidationTracker { *; }
-keep class androidx.room.RoomOpenHelper { *; }
-keep class androidx.room.RoomOpenHelper$* { *; }
-keep class androidx.room.RoomMasterTable { *; }
-keep class androidx.room.util.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }

# JNI and enum compatibility.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# App code that is commonly reached through DI, manifest services, notifications,
# or system callbacks. Avoid keeping feature/skin pages wholesale so R8 can still shrink.
-keep class **.use.app.MyApp { *; }
-keep class **.use.app.MainActivity { *; }
-keep class **.use.app.di.** { *; }
-keep class **.use.service.notification.** { *; }
-keep class **.use.service.applock.** { *; }
-keep class **.use.core.ads.AdvertiseConfigFactory { *; }
-keep class **.use.core.ads.AdAreaKeys { *; }
-keep class **.use.core.ads.AdAreaKeys$* { *; }
-keep class **.use.core.navigation.NotificationRouteAliases { *; }
-keep class **.use.service.notification.ToolNotificationIntentFactory { *; }

# Local Trustlook cloud scan AAR. Keep conservatively because it is a binary SDK.
-keep class com.trustlook.** { *; }
-dontwarn com.trustlook.**

# Company advertising SDK and integrated ad/attribution networks.
-keep class com.pdffox.adv.** { *; }
-dontwarn com.pdffox.adv.**
-dontwarn cn.thinkingdata.ta_apt.**
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.gms.internal.ads.**
-dontwarn com.google.ads.**
-dontwarn com.facebook.**
-dontwarn com.pangle.**
-dontwarn com.bytedance.**
-dontwarn com.tiktok.**
-dontwarn com.applovin.**
-dontwarn com.chartboost.**
-dontwarn com.ironsource.**
-dontwarn com.mbridge.**
-dontwarn com.anythink.**
-dontwarn com.singular.sdk.**

# Firebase, Play Integrity and Google services optional implementation classes.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.play.core.**
-dontwarn com.google.android.play.integrity.**
-dontwarn com.google.android.gms.**

# Koin dependency injection and Kotlin metadata/reflection helpers.
-keep class org.koin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn org.koin.**
-dontwarn kotlin.reflect.**

# Lottie animation runtime.
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Kotlin, coroutines, AndroidX, Compose and common optional dependencies.
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**
-dontwarn androidx.appcompat.**
-dontwarn androidx.activity.**
-dontwarn androidx.savedstate.**
-dontwarn androidx.profileinstaller.**
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Internal Android APIs accessed reflectively for device/battery information.
-dontwarn com.android.internal.os.PowerProfile
