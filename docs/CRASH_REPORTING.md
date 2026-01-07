# Crash Reporting Setup Guide

This app has placeholder infrastructure for crash reporting. This document explains how to integrate a crash reporting service.

## Current Status

- ✅ `CrashReporting.kt` utility created
- ✅ Ready for integration
- ⚠️ **NOT YET ACTIVE** - You need to choose and configure a service

## Recommended Services

### Option 1: Firebase Crashlytics (Recommended for Play Store)

**Pros:**
- Free tier is very generous
- Integrates with Google Play Console
- Real-time crash reporting
- Good analytics integration

**Cons:**
- Requires Google account
- Proprietary service

**Setup:**

1. **Add Firebase to your project**:
   - Go to https://console.firebase.google.com/
   - Create a new project or use existing
   - Add Android app with package `net.luisico.howmanyhours`
   - Download `google-services.json` to `app/` folder

2. **Update build files**:

   In `build.gradle.kts` (project level):
   ```kotlin
   plugins {
       id("com.google.gms.google-services") version "4.4.0" apply false
       id("com.google.firebase.crashlytics") version "2.9.9" apply false
   }
   ```

   In `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
       id("com.google.firebase.crashlytics")
   }

   dependencies {
       implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
       implementation("com.google.firebase:firebase-crashlytics-ktx")
       implementation("com.google.firebase:firebase-analytics-ktx")
   }
   ```

3. **Initialize in MainActivity**:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       CrashReporting.initialize(this)
       // ...
   }
   ```

4. **Implement in CrashReporting.kt**:
   Uncomment the Firebase examples in the placeholder methods.

### Option 2: Sentry (Recommended for Open Source)

**Pros:**
- Open source friendly
- Self-hostable (but also has cloud option)
- Excellent error grouping and search
- Free tier: 5,000 events/month

**Cons:**
- More complex setup than Firebase

**Setup:**

1. **Create account**: https://sentry.io/signup/

2. **Add dependency** in `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation("io.sentry:sentry-android:7.0.0")
   }
   ```

3. **Add to AndroidManifest.xml** inside `<application>`:
   ```xml
   <meta-data
       android:name="io.sentry.dsn"
       android:value="YOUR_DSN_HERE" />
   <meta-data
       android:name="io.sentry.environment"
       android:value="production" />
   ```

4. **Initialize in MainActivity**:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       CrashReporting.initialize(this)
       // ...
   }
   ```

5. **Implement in CrashReporting.kt**:
   Uncomment the Sentry examples.

### Option 3: ACRA (Fully Open Source, No Cloud Required)

**Pros:**
- Completely open source
- No third-party service required
- Can email crash reports or use HTTP endpoint
- Full data control

**Cons:**
- More manual setup
- No built-in dashboard (unless you set up your own)

**Setup:**

1. **Add dependency** in `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation("ch.acra:acra-mail:5.11.0")
       // Or for HTTP reporting:
       // implementation("ch.acra:acra-http:5.11.0")
   }
   ```

2. **Create Application class** (if you don't have one):

   Create `HowManyHoursApplication.kt`:
   ```kotlin
   package net.luisico.howmanyhours

   import android.app.Application
   import org.acra.config.mailSender
   import org.acra.data.StringFormat
   import org.acra.ktx.initAcra

   class HowManyHoursApplication : Application() {
       override fun onCreate() {
           super.onCreate()

           initAcra {
               buildConfigClass = BuildConfig::class.java
               reportFormat = StringFormat.JSON
               mailSender {
                   mailTo = "your-email@example.com"
                   reportAsFile = true
                   subject = "HowManyHours Crash Report"
               }
           }
       }
   }
   ```

3. **Update AndroidManifest.xml**:
   ```xml
   <application
       android:name=".HowManyHoursApplication"
       ...>
   ```

## Integration Checklist

After choosing a service:

- [ ] Add dependency to `app/build.gradle.kts`
- [ ] Implement initialization in `CrashReporting.initialize()`
- [ ] Implement logging methods in `CrashReporting.kt`
- [ ] Test with `CrashReporting.testCrash()` in debug build
- [ ] Verify crashes appear in your dashboard
- [ ] Add breadcrumbs in critical user flows
- [ ] Set user IDs (use hashed/anonymized for privacy)
- [ ] Add custom keys for important app state
- [ ] Update ProGuard rules if needed (service-specific)

## Usage Examples

### Basic Exception Logging

```kotlin
try {
    val result = database.query()
} catch (e: Exception) {
    CrashReporting.logException(e, "Database query failed")
    // Handle error gracefully
}
```

### Using Extension Function

```kotlin
try {
    processTimeEntry()
} catch (e: Exception) {
    e.report("Failed to process time entry")
}
```

### Adding Breadcrumbs

```kotlin
fun startTimeTracking(projectId: Long) {
    CrashReporting.logBreadcrumb("Starting time tracking for project $projectId", "tracking")
    // ... your code
}
```

### Setting User Context

```kotlin
// Use hashed ID for privacy
val userId = project.id.toString().hashCode().toString()
CrashReporting.setUserId(userId)
```

### Custom Keys for State Tracking

```kotlin
CrashReporting.setCustomKey("active_projects", activeProjects.size.toString())
CrashReporting.setCustomKey("period_mode", currentPeriodMode)
```

## Privacy Considerations

- **Anonymize user data**: Don't send personally identifiable information
- **Hash IDs**: Use hashed project/entry IDs, not raw data
- **Exclude sensitive data**: Don't log project names or time entry details
- **Comply with GDPR**: Update privacy policy to mention crash reporting
- **Allow opt-out**: Consider adding a setting to disable crash reporting

## Testing

### In Debug Builds

Add to your debug menu or developer settings:

```kotlin
Button(onClick = { CrashReporting.testCrash() }) {
    Text("Test Crash Reporting")
}
```

### In Release Builds

1. Build release APK
2. Install on test device
3. Trigger a real crash
4. Check your dashboard for the report

## ProGuard Configuration

Most crash reporting services need ProGuard rules. Check your chosen service's documentation.

Example for Firebase Crashlytics (already handled by Firebase SDK):
```proguard
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
```

Example for Sentry:
```proguard
-keepattributes LineNumberTable,SourceFile
-dontwarn io.sentry.**
-keep class io.sentry.** { *; }
```

## Next Steps

1. Choose a crash reporting service based on your needs
2. Follow the setup instructions above
3. Test in debug builds first
4. Deploy to a small group of beta testers
5. Monitor your dashboard
6. Fix the most common crashes
7. Iterate

## Resources

- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Sentry Android](https://docs.sentry.io/platforms/android/)
- [ACRA](https://github.com/ACRA/acra)
- [Android Vitals (Play Console)](https://developer.android.com/topic/performance/vitals)
