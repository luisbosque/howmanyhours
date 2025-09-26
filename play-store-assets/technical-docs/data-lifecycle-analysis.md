# HowManyHours - Data Lifecycle & Storage Analysis

## Data Storage Overview

### Storage Technology: Android Room Database
**Type**: SQL Database (SQLite wrapper)  
**Location**: Internal app storage (`/data/data/com.howmanyhours/databases/`)  
**Engine**: SQLite 3.x (Android's built-in database engine)  
**ORM**: Android Room (architecture component)

## Data Structure Analysis

### Database Schema

#### Table 1: `projects`
```sql
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 0,  -- Boolean (0/1)
    createdAt INTEGER  -- Timestamp
);
```

#### Table 2: `time_entries` 
```sql
CREATE TABLE time_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    projectId INTEGER NOT NULL,
    startTime INTEGER NOT NULL,  -- Date as timestamp
    endTime INTEGER,             -- Date as timestamp, NULL if running
    isRunning INTEGER NOT NULL DEFAULT 0,  -- Boolean (0/1)
    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
);
```

### Data Types Supported by SQLite:
- **INTEGER**: Whole numbers, timestamps, booleans (0/1)
- **TEXT**: Strings, project names
- **REAL**: Floating point numbers (unused in this app)
- **BLOB**: Binary data (unused in this app)
- **NULL**: Empty values

## Data Lifecycle

### 1. Data Creation
```
User creates project → Room DAO → SQLite INSERT → Internal storage
User starts tracking → TimeEntry created with startTime, isRunning=true
User stops tracking → TimeEntry updated with endTime, isRunning=false
```

### 2. Data Access
```
App startup → Room queries → LiveData/Flow → UI updates
Timer updates → Real-time duration calculation → UI refresh
Project selection → Database queries → State updates
```

### 3. Data Modification
```
Manual time entry → New TimeEntry with start/end times
Project deletion → CASCADE DELETE removes related time entries
Time discarding → DELETE TimeEntry from database
```

### 4. Data Deletion
```
Individual project → DELETE project + CASCADE delete entries
App uninstall → Android removes entire app data directory
Clear app data → Android removes database files
```

## Storage Location & Security

### Physical Location
- **Path**: `/data/data/com.howmanyhours/databases/`
- **Files**: `time_tracking_database`, `time_tracking_database-shm`, `time_tracking_database-wal`
- **Permissions**: Only accessible to the app (Android app sandboxing)

### Security Model
- **App Sandboxing**: Other apps cannot access this data
- **Root Access**: Root users could access (but most devices aren't rooted)
- **Encryption**: Not encrypted by default, relies on Android's sandboxing
- **Backup**: Included in Android's automatic app data backup (if user enabled)

### Access Control
```
✅ App itself (full access)
✅ Android system (for backup/restore)
✅ Root access (if device is rooted)
❌ Other apps (sandboxed)
❌ External storage (data is internal)
❌ Cloud services (no network transmission)
```

## Data Persistence & Vulnerabilities

### What Can Affect Data:

#### **Safe Operations** (Data Preserved):
- App updates/upgrades
- Device restart
- App being killed by system
- Screen rotation
- Background/foreground transitions

#### **Data Loss Scenarios**:
- App uninstallation
- "Clear app data" in settings
- Device factory reset
- Storage corruption (rare)
- Manual database file deletion (root access required)

#### **Potential Issues**:
- **Database migrations**: Schema changes need migration scripts
- **Concurrent access**: Room handles this automatically
- **Storage full**: Could cause write failures
- **App crashes during write**: SQLite transactions provide protection

## SQLite Engine Capabilities

### Features Used by This App:
- **ACID transactions**: Ensures data consistency
- **Foreign key constraints**: Maintains referential integrity
- **Auto-increment primary keys**: Unique record identification
- **NULL values**: Optional end times for running entries
- **Date/time storage**: Unix timestamps for precise time tracking

### SQLite Advanced Features (Not Currently Used):
- **Triggers**: Could automate calculations
- **Views**: Could simplify complex queries
- **Indexes**: Could improve query performance
- **Full-text search**: Could enable project name searching
- **JSON support**: Could store structured metadata
- **Window functions**: Could enable advanced analytics

## Backup & Recovery Options

### Current State:
- **Android Auto Backup**: Included if user enables cloud backup
- **Manual Export**: CSV export functionality exists
- **No automatic backup**: App doesn't create its own backups

### Manual Backup Methods:
1. **CSV Export**: User-initiated export to external storage
2. **ADB Backup**: `adb backup com.howmanyhours` (requires USB debugging)
3. **Root Methods**: Direct database file copying (root required)

### Restoration:
- **From backup**: Restore app data through Android recovery
- **From CSV**: Would require import functionality (not currently implemented)
- **Fresh install**: Starts with empty database

## Storage Technology Assessment

### Is Room/SQLite Deprecated?
**No** - Room and SQLite are:
- ✅ **Current**: Actively maintained and updated
- ✅ **Recommended**: Google's preferred local database solution
- ✅ **Modern**: Supports latest Android APIs
- ✅ **Stable**: Mature, battle-tested technology

### Future-Proof Alternatives:
If you needed to migrate later:
- **DataStore**: For simple key-value data (not suitable for relational data)
- **Cloud Firestore**: For cloud-synced data
- **External SQLite**: Direct SQLite without Room wrapper
- **Custom solutions**: JSON files, etc. (not recommended)

### Room Advantages for This App:
- **Perfect fit**: Relational data (projects → time entries)
- **Type safety**: Compile-time SQL verification
- **LiveData integration**: Automatic UI updates
- **Migration support**: Schema evolution path
- **Testing support**: Easy to unit test

## Recommendations

### Current Architecture: ✅ **Excellent Choice**
- Room/SQLite is ideal for this use case
- No need to change storage technology
- Stable, fast, and reliable

### Potential Improvements:
1. **Database encryption**: Use SQLCipher for sensitive data
2. **Export/Import**: Add CSV import functionality
3. **Migration scripts**: Plan for future schema changes
4. **Indexing**: Add indexes if performance becomes an issue
5. **Backup reminders**: Remind users to export data periodically

### Long-term Considerations:
- Room/SQLite will remain viable for years
- Cloud sync could be added later without changing local storage
- Current architecture scales well for personal time tracking needs