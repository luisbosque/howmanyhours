# Contributing to HowManyHours

Thank you for your interest in contributing to HowManyHours! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

## Code of Conduct

- Be respectful and constructive
- Welcome newcomers and help them learn
- Focus on the problem, not the person
- Keep discussions technical and on-topic

## Getting Started

### 1. Set Up Development Environment

See [SETUP_GUIDE.md](SETUP_GUIDE.md) for platform-specific instructions.

Quick start:
```bash
git clone https://github.com/luisico/howmanyhours.git
cd howmanyhours
./scripts/setup.sh  # Linux/macOS
./gradlew build
```

### 2. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-number-description
```

Branch naming convention:
- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation only
- `refactor/description` - Code refactoring
- `test/description` - Test improvements

### 3. Make Your Changes

- Follow the coding standards (see below)
- Write tests for new functionality
- Update documentation as needed
- Test thoroughly on real devices

## Development Workflow

### Daily Development

```bash
# Pull latest changes
git pull origin main

# Create feature branch
git checkout -b feature/my-feature

# Make changes and test
./gradlew test
./scripts/install.sh  # Test on device

# Commit with meaningful messages
git add .
git commit -m "Add time tracking pause feature

- Implement pause/resume for running entries
- Add UI buttons for pause functionality
- Update tests for pause scenarios"

# Push to your fork
git push origin feature/my-feature
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "TimeEntryDataReliabilityTest"

# Run on device (instrumented tests)
./gradlew connectedAndroidTest
```

### Code Review Checklist

Before submitting:
- [ ] Code builds without warnings
- [ ] All tests pass
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No debug code or commented-out code
- [ ] Follows project coding standards
- [ ] Tested on physical device

## Coding Standards

### Kotlin Style

Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Good
class TimeTrackingRepository(
    private val database: AppDatabase
) {
    fun getActiveProject(): Project? {
        return database.projectDao().getActiveProject()
    }
}

// Bad
class TimeTrackingRepository(private val database:AppDatabase){
    fun getActiveProject():Project?{
        return database.projectDao().getActiveProject()
    }
}
```

### Naming Conventions

- **Classes**: PascalCase - `TimeTrackingViewModel`
- **Functions**: camelCase - `getTimeEntriesForProject`
- **Constants**: UPPER_SNAKE_CASE - `MAX_PROJECT_NAME_LENGTH`
- **Variables**: camelCase - `projectId`, `startTime`
- **Composables**: PascalCase - `ProjectDetailScreen`

### Documentation

Document public APIs and complex logic:

```kotlin
/**
 * Calculates total minutes for a project within a specific period.
 *
 * @param projectId The project to calculate time for
 * @param periodEnd The end time of the period (inclusive)
 * @return Total minutes tracked, or 0 if no entries
 */
suspend fun getTotalMinutesForPeriod(
    projectId: Long,
    periodEnd: Long
): Int
```

### Compose Guidelines

```kotlin
// Good: Clear parameter names, preview included
@Composable
fun ProjectCard(
    project: Project,
    isActive: Boolean,
    onProjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}

@Preview
@Composable
fun ProjectCardPreview() {
    ProjectCard(
        project = Project(id = 1, name = "Sample"),
        isActive = true,
        onProjectClick = {}
    )
}
```

### Data Safety

**CRITICAL**: Never expose user data in logs or exceptions:

```kotlin
// Good
Log.d("TimeTracking", "Starting entry for project $projectId")

// Bad - exposes user's project name
Log.d("TimeTracking", "Starting entry for ${project.name}")
```

## Testing

### Test Coverage Requirements

All new features and bug fixes must include tests:

1. **Unit Tests** (`app/src/test/`):
   - Repository logic
   - ViewModel logic
   - Utility functions
   - Data transformations

2. **Instrumented Tests** (`app/src/androidTest/`):
   - Database operations
   - DAO queries
   - Migrations
   - Complex integrations

3. **Critical Areas** (see SECURITY_TODO.md):
   - Data reliability tests (mandatory)
   - Period calculation tests (mandatory)
   - Backup/restore tests
   - Crash recovery scenarios

### Writing Tests

```kotlin
@Test
fun `test time entry stores exact start and end times`() = runBlocking {
    // Given
    val projectId = projectDao.insert(Project(name = "Test"))
    val startTime = 1000L
    val endTime = 2000L

    // When
    val entryId = timeEntryDao.insert(TimeEntry(
        projectId = projectId,
        startTime = startTime,
        endTime = endTime,
        isRunning = false
    ))

    // Then
    val entry = timeEntryDao.getTimeEntryById(entryId)
    assertEquals(startTime, entry.startTime)
    assertEquals(endTime, entry.endTime)
}
```

### Test Naming

Use descriptive test names with backticks:

```kotlin
@Test
fun `test period close includes entries at exact boundary time`()

@Test
fun `test cascade delete removes all time entries when project deleted`()
```

## Submitting Changes

### 1. Prepare Your Branch

```bash
# Ensure all tests pass
./gradlew test
./gradlew connectedAndroidTest

# Update from main
git fetch origin
git rebase origin/main

# Fix any conflicts
```

### 2. Create Pull Request

1. Push to your fork
2. Go to GitHub and create Pull Request
3. Fill in the PR template:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Instrumented tests pass
- [ ] Tested on physical device (Android version: ___)
- [ ] No new warnings

## Screenshots (if UI changes)
[Attach before/after screenshots]

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-reviewed the code
- [ ] Commented complex logic
- [ ] Updated documentation
- [ ] No breaking changes (or documented)
```

### 3. Code Review Process

- Maintainer will review within 1-2 weeks
- Address review comments
- Once approved, maintainer will merge

### Commit Message Format

Use conventional commits:

```
type(scope): short description

Longer explanation if needed
- Bullet points for details
- Can span multiple lines

Fixes #123
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `test`: Adding/updating tests
- `refactor`: Code restructuring
- `perf`: Performance improvement
- `chore`: Build/config changes

Examples:
```
feat(tracking): add pause/resume functionality

- Add pause button to active timer UI
- Implement database support for paused state
- Update period calculations to handle paused time

Closes #45

---

fix(periods): correct boundary calculation for period close

Entry at exact period close time was excluded from both periods.
Now correctly includes it in the closed period (inclusive boundary).

Fixes #78
```

## Reporting Bugs

### Before Reporting

1. Check [existing issues](https://github.com/luisico/howmanyhours/issues)
2. Ensure you're on the latest version
3. Test on a physical device if possible

### Bug Report Template

```markdown
## Description
Clear description of the bug

## Steps to Reproduce
1. Open app
2. Start timer for Project A
3. Switch to Project B
4. Observe incorrect behavior

## Expected Behavior
Timer should stop for Project A and start for Project B

## Actual Behavior
Both timers running simultaneously

## Environment
- Device: Pixel 6
- Android Version: 13
- App Version: 1.0.0
- Installation Source: GitHub release / Play Store / Built from source

## Screenshots
[If applicable]

## Logs
```
[Paste relevant logcat output]
```

## Additional Context
Any other relevant information
```

## Feature Requests

### Before Requesting

- Check existing issues and discussions
- Consider if it fits the app's philosophy (simple, privacy-focused, offline)

### Feature Request Template

```markdown
## Problem Statement
What problem does this solve?

## Proposed Solution
How should it work?

## Alternatives Considered
What other solutions did you consider?

## Impact
- Who benefits from this?
- Does it affect privacy/simplicity?

## Additional Context
Mockups, examples from other apps, etc.
```

## Project Philosophy

When contributing, keep these principles in mind:

1. **Privacy First**: No cloud sync, no analytics, no tracking
2. **Simplicity**: Easy to use, minimal UI, focused features
3. **Reliability**: User data must never be lost
4. **Offline**: Must work without internet
5. **Performance**: Fast and lightweight

Features that conflict with these principles may not be accepted.

## Development Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Android Architecture](https://developer.android.com/topic/architecture)
- [Material Design 3](https://m3.material.io/)

## Questions?

- Open a [Discussion](https://github.com/luisico/howmanyhours/discussions)
- Comment on relevant issues
- Check [SETUP_GUIDE.md](SETUP_GUIDE.md) for environment setup

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing to HowManyHours!
