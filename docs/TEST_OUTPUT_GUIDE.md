# Understanding Test Output in Gradle

## What Those Numbers Mean

When you run tests, Gradle shows output like:
```
BUILD SUCCESSFUL in 14s
70 actionable tasks: 33 executed, 37 up-to-date
```

**These are NOT tests!** These are Gradle build tasks:

- **70 actionable tasks**: Total build steps (compile, process resources, run tests, etc.)
- **33 executed**: Tasks that actually ran this time
- **37 up-to-date**: Tasks skipped because nothing changed (cached)

## Unit Tests (`./gradlew test`)

With the new configuration, you'll see clear output like:

```
> Task :app:testDebugUnitTest

net.luisico.howmanyhours.backup.BackupManagerTest > restore result success type exists PASSED
net.luisico.howmanyhours.backup.BackupManagerTest > backup info should store correct data PASSED
net.luisico.howmanyhours.backup.BackupManagerTest > restore result failed should contain error message PASSED
```

**What you see:**
- Test class name (e.g., `BackupManagerTest`)
- `>` separator
- Test method name (converted to readable text)
- Status: `PASSED`, `FAILED`, or `SKIPPED`

### Detailed Unit Test Reports

Open in browser:
```bash
xdg-open app/build/reports/tests/testDebugUnitTest/index.html
```

Or manually navigate to:
- `app/build/reports/tests/testDebugUnitTest/index.html` - Debug variant tests
- `app/build/reports/tests/testReleaseUnitTest/index.html` - Release variant tests

The HTML reports show:
- Summary with pass/fail counts
- Execution time for each test
- Full stack traces for failures
- Standard output/error from tests

## Instrumented Tests (`./gradlew connectedAndroidTest`)

Output looks like:
```
> Task :app:connectedDebugAndroidTest
Starting 24 tests on HowManyHours_Dev(AVD) - 14

BUILD SUCCESSFUL in 14s
```

**What the numbers mean:**
- **24 tests**: Number of test methods being executed
- **HowManyHours_Dev(AVD)**: Device name
- **14**: Android API level (Android 14)

### Detailed Instrumented Test Reports

Open in browser:
```bash
xdg-open app/build/reports/androidTests/connected/debug/index.html
```

Or manually navigate to:
- `app/build/reports/androidTests/connected/debug/index.html` - Main report

The HTML reports include:
- Individual test class reports
  - `DatabaseIntegrityTest.html` - 8 tests
  - `BackupCreationAndVerificationTest.html` - 6 tests
  - `BackupValidationTest.html` - 7 tests
  - `BackupRestorationTest.html` - 7 tests
- Execution time per test
- Full failure details with stack traces
- Device information

## More Verbose Output Options

### See Live Test Output

For unit tests, add `--info`:
```bash
./gradlew test --info
```

For instrumented tests with orchestrator output:
```bash
./gradlew connectedAndroidTest --info
```

### See Which Tests Are Disabled

Both commands will also show if any tests are skipped/ignored.

## Test Configuration Improvements Applied

### In `app/build.gradle.kts`:

```kotlin
testOptions {
    unitTests.all {
        it.testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    execution = "ANDROIDX_TEST_ORCHESTRATOR"
}
```

**What this does:**
- Shows test names and status in console
- Prints full exception details on failure
- Uses test orchestrator for cleaner Android test execution
- Each test runs in isolated process for better reliability

## Quick Reference

| Task | Command | Report Location |
|------|---------|-----------------|
| Unit tests | `./gradlew test` | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Instrumented tests | `./gradlew connectedAndroidTest` | `app/build/reports/androidTests/connected/debug/index.html` |
| All tests | `./gradlew test connectedAndroidTest` | Both locations above |
| Clean reports | `./gradlew clean` | Removes `app/build/` directory |

## Warnings vs Errors

**Warnings** (prefixed with `w:`):
- Kotlin compiler suggestions
- Code style issues
- Deprecation notices
- Do NOT cause build to fail

**Errors**:
- Compilation failures
- Test failures
- WILL cause build to fail with exit code != 0

**All warnings have been fixed!** You should no longer see them.
