# Security and Reliability TODO

## CRITICAL

- [ ] **#1 - Backup Restore with No Rollback**
  - Location: BackupManager.kt:455-515
  - Issue: Deletes current database before verifying backup is valid
  - Solution: TBD - validate backup before deletion OR enable rollback
  - Status: In discussion

- [x] **#2 - setMonthlyHours() Deletes Entries** ✅ COMPLETED
  - Location: TimeTrackingRepository.kt:130-156 (REMOVED)
  - Issue: Unused old function that deleted entries
  - Solution: Function removed entirely - was not used anywhere
  - Status: FIXED - Removed from repository, ViewModel, and DAO

- [ ] **#3 - Race Condition in Project Activation**
  - Location: TimeTrackingRepository.kt:49-52
  - Issue: Deactivates all projects, then activates one - not atomic
  - Note: Behavior is expected - "active" means currently displayed project
  - Status: Keep in TODO - needs transaction wrapping

- [ ] **#4 - Backup Missing period_closes Table**
  - Location: BackupManager.kt:366-452
  - Issue: Backup only saves projects and time_entries, not period_closes
  - Status: Keep in TODO, will tackle later (entries are priority)

- [x] **#5 - Timezone Handling Throughout App** ✅ COMPLETED
  - Location: Multiple files (FIXED)
  - Issue: Used Calendar/SimpleDateFormat without explicit timezone
  - Solution: Added explicit TimeZone.getDefault() to all date operations
  - Files fixed:
    - TimeTrackingRepository.kt: Period.getLabel(), getTimeEntriesForMonth(), checkMonthlyAutoClose()
    - TimeTrackingViewModel.kt: All 3 Calendar.getInstance() calls
    - PeriodDetailScreen.kt: All SimpleDateFormat calls
    - PeriodHistoryScreen.kt: formatDateRange()
    - ProjectDetailScreen.kt: formatDate()
    - SettingsScreen.kt: CSV export timestamp
    - BackupScreen.kt: Backup display timestamps
    - BackupManager.kt: Backup file naming
  - Status: FIXED - All timezone usage is now explicit
  - Related: Also fixes #10 (timezone change during tracking)

## HIGH

- [x] **#6 - CSV Export Doesn't Escape Quotes** ✅ COMPLETED
  - Location: TimeTrackingViewModel.kt:456 (FIXED)
  - Issue: Entry names with quotes break CSV format
  - Solution: Now properly escapes quotes by doubling them: `"Client "ABC"` → `"Client ""ABC""`
  - Status: FIXED - Proper CSV quote escaping implemented

- [ ] **#7 - Database Auto-Backup Enabled (Privacy)**
  - Location: AndroidManifest.xml:12
  - Issue: allowBackup="true" allows ADB extraction
  - Status: TODO

- [ ] **#8 - Unsafe stopAllRunningEntries()**
  - Location: TimeEntryDao.kt:57-58
  - Issue: Silently stops multiple entries without reporting
  - Status: TODO

- [ ] **#9 - Process Death During Backup Creation**
  - Location: BackupManager.kt:467
  - Issue: Database closed during backup, crash if process killed
  - Status: TODO

- [x] **#10 - Timezone Change During Active Tracking** ✅ COMPLETED
  - Fixed as part of #5
  - All date operations now use explicit timezone

## MEDIUM

- [x] **#11 - Missing Foreign Key Index on projectId** ✅ COMPLETED
  - Location: time_entries table schema (FIXED)
  - Issue: No index on foreign key, slow cascading deletes
  - Solution: Added database migration (3→4) that creates index
  - Status: FIXED - Index created in MIGRATION_3_4

- [x] **#12 - Period Calculation Off-By-One** ✅ COMPLETED
  - Location: TimeEntryDao.kt:32 (FIXED)
  - Issue: Entry at exact close time was not included in any period
  - Decision: Entry at exact close time belongs to period being closed
  - Solution: Changed `startTime < :periodEnd` to `startTime <= :periodEnd`
  - Status: FIXED - Inclusive end boundary for closed periods

- [x] **#13 - CSV Generation Out of Memory** ✅ COMPLETED
  - Location: TimeTrackingViewModel.kt:437-472 (REFACTORED)
  - Issue: Loaded all entries into memory for large datasets
  - Solution: Implemented chunked processing (1000 entries per batch)
  - Details:
    - Added `getTimeEntriesBatch(limit, offset)` to DAO
    - Changed `exportToCsv()` to `exportToCsvToStream(OutputStream)`
    - Streams data directly to file, never loads all entries
  - Status: FIXED - Memory-efficient streaming implementation

- [ ] **#14 - Long-Running getAllTimeEntries() on UI Thread**
  - Location: TimeTrackingRepository.kt:277
  - Issue: Loads all entries to find one by ID
  - Status: TODO

- [ ] **#15 - No Validation on periodMode Values**
  - Location: TimeTrackingRepository.kt:262-266
  - Issue: Accepts any string, should use enum
  - Status: TODO

- [ ] **#16 - Concurrent Access to Running Entry**
  - Location: Multiple ViewModel methods
  - Issue: Race conditions on running entry updates
  - Status: TODO

- [x] **#17 - CSV Export to Public Downloads** ✅ COMPLETED
  - Location: SettingsScreen.kt:228-251 (FIXED)
  - Issue: Public folder, any app can read
  - Solution: Changed to `getExternalFilesDir(DIRECTORY_DOCUMENTS)`
  - Details:
    - App-specific folder (deleted on uninstall)
    - Still accessible via file manager for users
    - Not readable by other apps
    - No storage permission needed on Android 10+
  - Status: FIXED - Now uses secure app-specific storage

## LOW

- [x] **#18 - No Input Length Validation on Project Name** ✅ COMPLETED
  - Location: MainScreen.kt:547 (FIXED)
  - Issue: No maximum length limit
  - Solution: Set 64 character limit with character counter
  - Details: Added `onValueChange = { if (it.length <= 64) projectName = it }`
  - Status: FIXED - 64 char limit with counter display

- [ ] **#19 - Backup Files Potentially World-Readable**
  - Location: BackupManager.kt:46
  - Issue: On rooted devices, other apps might access
  - Status: TODO (low priority, requires rooted device)

- [ ] **#20 - Missing Notification Permission Feedback**
  - Location: MainActivity.kt:38-42
  - Issue: No user feedback if permission denied
  - Status: TODO

## ANDROID VERSION SUPPORT

**Minimum:** Android 9.0 (API 28) - August 2018
**Target:** Android 14 (API 34)
**Coverage:** ~85% of active devices
**Status:** Acceptable, will document in app description

## POSITIVE FINDINGS

✓ No SQL injection vulnerabilities
✓ Proper scoped storage implementation
✓ No memory leaks in ViewModels/coroutines
✓ Foreign keys properly defined with CASCADE
✓ Correct ViewModel lifecycle management
