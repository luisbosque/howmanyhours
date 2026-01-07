package net.luisico.howmanyhours.utils

import android.content.Context

/**
 * Crash Reporting Infrastructure
 *
 * This is a placeholder for crash reporting integration.
 * Choose one of the following options and implement:
 *
 * 1. Firebase Crashlytics (Google, free tier available)
 *    - Add: implementation 'com.google.firebase:firebase-crashlytics-ktx:18.6.0'
 *    - Docs: https://firebase.google.com/docs/crashlytics/get-started?platform=android
 *
 * 2. Sentry (Open source friendly, generous free tier)
 *    - Add: implementation 'io.sentry:sentry-android:7.0.0'
 *    - Docs: https://docs.sentry.io/platforms/android/
 *
 * 3. Bugsnag (Enterprise, good for commercial apps)
 *    - Add: implementation 'com.bugsnag:bugsnag-android:5.31.0'
 *    - Docs: https://docs.bugsnag.com/platforms/android/
 *
 * 4. ACRA (Completely open source, self-hosted or email reports)
 *    - Add: implementation 'ch.acra:acra-http:5.11.0'
 *    - Docs: https://github.com/ACRA/acra
 */
object CrashReporting {

    private var initialized = false

    /**
     * Initialize crash reporting
     * Call this from Application.onCreate() or MainActivity.onCreate()
     *
     * @param context Application context
     */
    fun initialize(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (initialized) {
            return
        }

        // TODO: Initialize your chosen crash reporting service here
        // Example for Firebase Crashlytics:
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Example for Sentry:
        // SentryAndroid.init(context) { options ->
        //     options.dsn = "YOUR_DSN_HERE"
        //     options.environment = if (BuildConfig.DEBUG) "debug" else "production"
        // }

        initialized = true
    }

    /**
     * Log a non-fatal exception
     * Use this for caught exceptions that you want to track
     *
     * @param throwable The exception to log
     * @param message Optional additional context
     */
    fun logException(throwable: Throwable, message: String? = null) {
        if (!initialized) {
            // Fallback to Android Log if not initialized
            android.util.Log.e("CrashReporting", message ?: "Exception occurred", throwable)
            return
        }

        // TODO: Log to your crash reporting service
        // Example for Firebase Crashlytics:
        // if (message != null) {
        //     FirebaseCrashlytics.getInstance().log(message)
        // }
        // FirebaseCrashlytics.getInstance().recordException(throwable)

        // Example for Sentry:
        // if (message != null) {
        //     Sentry.captureException(throwable) { scope ->
        //         scope.setExtra("message", message)
        //     }
        // } else {
        //     Sentry.captureException(throwable)
        // }
    }

    /**
     * Set user identifier for crash reports
     * Helps track which users are experiencing issues
     *
     * @param userId Unique user identifier (use hashed/anonymized IDs for privacy)
     */
    fun setUserId(@Suppress("UNUSED_PARAMETER") userId: String) {
        if (!initialized) return

        // TODO: Set user ID in your crash reporting service
        // Example for Firebase Crashlytics:
        // FirebaseCrashlytics.getInstance().setUserId(userId)

        // Example for Sentry:
        // Sentry.setUser(User().apply { id = userId })
    }

    /**
     * Log custom key-value pairs for crash context
     * Useful for tracking app state during crashes
     *
     * @param key The key name
     * @param value The value
     */
    fun setCustomKey(
        @Suppress("UNUSED_PARAMETER") key: String,
        @Suppress("UNUSED_PARAMETER") value: String
    ) {
        if (!initialized) return

        // TODO: Set custom keys in your crash reporting service
        // Example for Firebase Crashlytics:
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)

        // Example for Sentry:
        // Sentry.setExtra(key, value)
    }

    /**
     * Log a breadcrumb (event trail leading to crash)
     * Helps understand user actions before a crash
     *
     * @param message Description of the event
     * @param category Optional category (e.g., "navigation", "database", "network")
     */
    fun logBreadcrumb(
        @Suppress("UNUSED_PARAMETER") message: String,
        @Suppress("UNUSED_PARAMETER") category: String = "app"
    ) {
        if (!initialized) return

        // TODO: Log breadcrumb to your crash reporting service
        // Example for Firebase Crashlytics:
        // FirebaseCrashlytics.getInstance().log("[$category] $message")

        // Example for Sentry:
        // Sentry.addBreadcrumb(Breadcrumb().apply {
        //     this.message = message
        //     this.category = category
        //     level = SentryLevel.INFO
        // })
    }

    /**
     * Force a test crash (debug builds only!)
     * Use this to verify crash reporting is working
     */
    fun testCrash() {
        if (initialized) {
            throw RuntimeException("Test crash from CrashReporting.testCrash()")
        }
    }
}

/**
 * Extension function to easily log exceptions in try-catch blocks
 *
 * Usage:
 * try {
 *     riskyOperation()
 * } catch (e: Exception) {
 *     e.report("Failed to perform risky operation")
 * }
 */
fun Throwable.report(message: String? = null) {
    CrashReporting.logException(this, message)
}
