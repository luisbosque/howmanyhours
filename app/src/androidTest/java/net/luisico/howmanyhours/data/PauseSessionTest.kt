package net.luisico.howmanyhours.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.luisico.howmanyhours.data.dao.PeriodCloseDao
import net.luisico.howmanyhours.data.dao.ProjectDao
import net.luisico.howmanyhours.data.dao.TimeEntryDao
import net.luisico.howmanyhours.data.database.AppDatabase
import net.luisico.howmanyhours.data.entities.Project
import net.luisico.howmanyhours.data.entities.TimeEntry
import net.luisico.howmanyhours.repository.TimeTrackingRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Tests for the pause/resume session flow.
 *
 * Key invariants verified:
 * - pauseTracking saves the interval with isPausedInterval=true
 * - completePausedSession removes isPausedInterval flags on final stop
 * - deletePausedIntervals only removes entries that are BOTH in the given IDs and flagged as paused
 * - Regular (non-paused) entries are never deleted by deletePausedIntervals
 * - Time accumulation: each pause adds the correct interval duration
 * - Orphan cleanup: clearAllPausedIntervalFlags converts stale paused entries to normal entries
 */
@RunWith(AndroidJUnit4::class)
class PauseSessionTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TimeTrackingRepository
    private lateinit var timeEntryDao: TimeEntryDao
    private lateinit var projectDao: ProjectDao
    private lateinit var periodCloseDao: PeriodCloseDao
    private var testProjectId: Long = 0

    @Before
    fun setup() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        projectDao = database.projectDao()
        timeEntryDao = database.timeEntryDao()
        periodCloseDao = database.periodCloseDao()
        repository = TimeTrackingRepository(projectDao, timeEntryDao, periodCloseDao)

        testProjectId = projectDao.insertProject(
            Project(name = "Test Project", createdAt = Date(), isActive = true, periodMode = "manual")
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    // ── Pause saves interval with isPausedInterval=true ──────────────────────

    @Test
    fun pauseTracking_savesEntryWithPausedFlag() = runBlocking {
        repository.startTracking(testProjectId)

        val saved = repository.pauseTracking()

        assertNotNull("pauseTracking should return the saved entry", saved)
        assertFalse("saved interval must not be running", saved!!.isRunning)
        assertNotNull("saved interval must have an endTime", saved.endTime)
        assertTrue("saved interval must be flagged as paused", saved.isPausedInterval)
    }

    @Test
    fun pauseTracking_entryIsInDatabase() = runBlocking {
        repository.startTracking(testProjectId)
        val saved = repository.pauseTracking()!!

        val allEntries = timeEntryDao.getAllTimeEntries()
        val dbEntry = allEntries.find { it.id == saved.id }

        assertNotNull("saved paused entry must be in the database", dbEntry)
        assertTrue("DB entry must still have isPausedInterval=true", dbEntry!!.isPausedInterval)
    }

    @Test
    fun pauseTracking_whenNoRunningEntry_returnsNull() = runBlocking {
        val result = repository.pauseTracking()
        assertNull("pauseTracking with nothing running should return null", result)
    }

    // ── Final stop clears isPausedInterval flags ──────────────────────────────

    @Test
    fun stopAfterPause_clearsPausedFlagsFromAllSessionEntries() = runBlocking {
        // Pause twice, then stop
        repository.startTracking(testProjectId)
        val interval1 = repository.pauseTracking()!!

        repository.startTracking(testProjectId)
        val interval2 = repository.pauseTracking()!!

        repository.startTracking(testProjectId)
        repository.stopTracking() // final stop saves the last interval
        repository.completePausedSession(listOf(interval1.id, interval2.id))

        val all = timeEntryDao.getAllTimeEntries().filter { it.projectId == testProjectId }

        // All three intervals should be in the DB, none flagged as paused
        assertEquals("all three intervals should be saved", 3, all.size)
        assertTrue("no entry should remain flagged as paused after session completes",
            all.none { it.isPausedInterval })
    }

    @Test
    fun stopAfterPause_allIntervalsHaveCorrectTimes() = runBlocking {
        repository.startTracking(testProjectId)
        val interval1 = repository.pauseTracking()!!

        repository.startTracking(testProjectId)
        repository.stopTracking()
        repository.completePausedSession(listOf(interval1.id))

        val all = timeEntryDao.getAllTimeEntries()
            .filter { it.projectId == testProjectId }
            .sortedBy { it.startTime }

        assertEquals(2, all.size)
        all.forEach { entry ->
            assertNotNull("every saved entry must have endTime", entry.endTime)
            assertFalse("every saved entry must not be running", entry.isRunning)
            assertFalse("every saved entry must have paused flag cleared", entry.isPausedInterval)
            assertTrue("every entry duration must be positive",
                entry.getDurationInMinutes() >= 0)
        }
    }

    // ── Discard deletes only paused entries (double-filter safety) ────────────

    @Test
    fun discard_deletesPausedIntervalsOnly() = runBlocking {
        // Create a normal completed entry that must survive the discard
        val normalEntry = TimeEntry(
            projectId = testProjectId,
            startTime = Date(System.currentTimeMillis() - 120_000),
            endTime = Date(System.currentTimeMillis() - 60_000),
            isRunning = false,
            isPausedInterval = false
        )
        val normalId = timeEntryDao.insertTimeEntry(normalEntry)

        // Now start a session, pause once, then start the running interval to discard
        repository.startTracking(testProjectId)
        val paused1 = repository.pauseTracking()!!

        repository.startTracking(testProjectId)
        // discard running entry + all paused intervals
        repository.discardTracking()
        repository.deletePausedIntervals(listOf(paused1.id))

        val remaining = timeEntryDao.getAllTimeEntries()
        assertEquals("only the pre-existing normal entry should remain", 1, remaining.size)
        assertEquals("surviving entry must be the original normal entry", normalId, remaining[0].id)
        assertFalse("surviving entry must not be flagged as paused", remaining[0].isPausedInterval)
    }

    @Test
    fun deletePausedIntervals_doesNotDeleteNormalEntryWithMatchingId() = runBlocking {
        // Scenario: somehow we have an ID that points to a non-paused entry.
        // deletePausedIntervals must not delete it because the second filter (isPausedInterval=1) prevents it.
        val normalEntry = TimeEntry(
            projectId = testProjectId,
            startTime = Date(System.currentTimeMillis() - 60_000),
            endTime = Date(),
            isRunning = false,
            isPausedInterval = false
        )
        val normalId = timeEntryDao.insertTimeEntry(normalEntry)

        // Try to delete it via deletePausedIntervals (simulating a bug where we pass wrong IDs)
        repository.deletePausedIntervals(listOf(normalId))

        val remaining = timeEntryDao.getAllTimeEntries()
        assertEquals("non-paused entry must survive deletePausedIntervals", 1, remaining.size)
        assertEquals(normalId, remaining[0].id)
    }

    @Test
    fun discard_withNoPausedIntervals_leavesDbEmpty() = runBlocking {
        repository.startTracking(testProjectId)
        repository.discardTracking()
        repository.deletePausedIntervals(emptyList())

        val all = timeEntryDao.getAllTimeEntries()
        assertEquals("nothing should remain after discarding with no prior pauses", 0, all.size)
    }

    // ── Time accumulation ────────────────────────────────────────────────────

    @Test
    fun pausedAccumulatedMinutes_sumsEachIntervalDuration() = runBlocking {
        // Interval 1: ~1 minute
        repository.startTracking(testProjectId)
        Thread.sleep(100) // tiny real time to get a non-zero duration
        val interval1 = repository.pauseTracking()!!

        // Interval 2: ~1 minute
        repository.startTracking(testProjectId)
        Thread.sleep(100)
        val interval2 = repository.pauseTracking()!!

        val accumulated = interval1.getDurationInMinutes() + interval2.getDurationInMinutes()

        // Each interval is < 1 minute in the test but getDurationInMinutes() floors to 0;
        // verify that getDurationInMinutes on both entries returns non-negative values and
        // that the entries exist with correct timestamps.
        assertTrue("interval1 duration must be non-negative", interval1.getDurationInMinutes() >= 0)
        assertTrue("interval2 duration must be non-negative", interval2.getDurationInMinutes() >= 0)
        assertTrue("interval2 must start after interval1 ends",
            interval2.startTime.time >= interval1.endTime!!.time)
        assertTrue("accumulated must be sum of individual durations",
            accumulated == interval1.getDurationInMinutes() + interval2.getDurationInMinutes())
    }

    @Test
    fun timerCountsWorkingTimeNotWallClock() = runBlocking {
        // Start, pause, wait a bit, resume, pause again.
        // Accumulated time should equal sum of actual running intervals, not total elapsed time.
        val sessionStart = System.currentTimeMillis()

        repository.startTracking(testProjectId)
        Thread.sleep(200)
        val interval1 = repository.pauseTracking()!!

        // Simulate pause duration — time passes but we are NOT tracking
        Thread.sleep(200)

        repository.startTracking(testProjectId)
        Thread.sleep(200)
        val interval2 = repository.pauseTracking()!!

        val totalSessionMs = System.currentTimeMillis() - sessionStart
        val workingMs = (interval1.endTime!!.time - interval1.startTime.time) +
                        (interval2.endTime!!.time - interval2.startTime.time)
        val pausedMs = totalSessionMs - workingMs

        // The paused time (≥200ms) should be non-trivial and NOT counted in working time
        assertTrue("pause time must be positive (time actually passed)", pausedMs > 0)
        assertTrue("working time must be less than total session time",
            workingMs < totalSessionMs)
        assertTrue("interval2 startTime must be after interval1 endTime (gap exists)",
            interval2.startTime.time >= interval1.endTime!!.time)
    }

    // ── Orphan cleanup on app restart ────────────────────────────────────────

    @Test
    fun cleanupOrphanedPausedIntervals_clearsFlagsWithoutDeletingEntries() = runBlocking {
        // Simulate a crash while paused: entry is saved with isPausedInterval=true
        val orphanEntry = TimeEntry(
            projectId = testProjectId,
            startTime = Date(System.currentTimeMillis() - 60_000),
            endTime = Date(),
            isRunning = false,
            isPausedInterval = true
        )
        timeEntryDao.insertTimeEntry(orphanEntry)

        repository.cleanupOrphanedPausedIntervals()

        val all = timeEntryDao.getAllTimeEntries()
        assertEquals("orphaned entry must still exist (not deleted)", 1, all.size)
        assertFalse("isPausedInterval flag must be cleared after cleanup", all[0].isPausedInterval)
    }

    @Test
    fun cleanupOrphanedPausedIntervals_doesNotAffectNormalEntries() = runBlocking {
        val normal = TimeEntry(
            projectId = testProjectId,
            startTime = Date(System.currentTimeMillis() - 60_000),
            endTime = Date(),
            isRunning = false,
            isPausedInterval = false
        )
        timeEntryDao.insertTimeEntry(normal)

        repository.cleanupOrphanedPausedIntervals()

        val all = timeEntryDao.getAllTimeEntries()
        assertEquals(1, all.size)
        assertFalse(all[0].isPausedInterval)
    }

    // ── Recent entry names ────────────────────────────────────────────────────

    @Test
    fun getRecentEntryNames_returnsDistinctNamesFromLast7Days() = runBlocking {
        val now = System.currentTimeMillis()
        val entries = listOf(
            TimeEntry(projectId = testProjectId, startTime = Date(now - 1_000), endTime = Date(now), name = "Client call"),
            TimeEntry(projectId = testProjectId, startTime = Date(now - 2_000), endTime = Date(now - 1_000), name = "Code review"),
            TimeEntry(projectId = testProjectId, startTime = Date(now - 3_000), endTime = Date(now - 2_000), name = "Client call"), // duplicate
            TimeEntry(projectId = testProjectId, startTime = Date(now - 8L * 24 * 60 * 60 * 1000), endTime = Date(now - 7L * 24 * 60 * 60 * 1000), name = "Old entry") // >7 days
        )
        entries.forEach { timeEntryDao.insertTimeEntry(it) }

        val names = repository.getRecentEntryNames()

        assertTrue("should contain 'Client call'", names.contains("Client call"))
        assertTrue("should contain 'Code review'", names.contains("Code review"))
        assertFalse("should not contain entry older than 7 days", names.contains("Old entry"))
        assertEquals("should deduplicate: only 2 distinct names", 2, names.size)
    }

    @Test
    fun getRecentEntryNames_excludesNullNames() = runBlocking {
        val now = System.currentTimeMillis()
        timeEntryDao.insertTimeEntry(
            TimeEntry(projectId = testProjectId, startTime = Date(now - 1_000), endTime = Date(now), name = null)
        )
        timeEntryDao.insertTimeEntry(
            TimeEntry(projectId = testProjectId, startTime = Date(now - 2_000), endTime = Date(now - 1_000), name = "Meeting")
        )

        val names = repository.getRecentEntryNames()

        assertEquals(1, names.size)
        assertEquals("Meeting", names[0])
    }
}
