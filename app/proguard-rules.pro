# Keep the whole app package — models are serialized to JSON, workers are
# instantiated reflectively by WorkManager.
-keep class com.expensetracker.tk.** { *; }

# WorkManager instantiates workers by class name
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
