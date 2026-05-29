# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]

### Added
- Archive projects: toggle in project detail hides a project from the main list; archive icon in the Projects header reveals archived projects on demand
- App screenshots in website feature sections with two-column layout
- Open Graph and Twitter Card meta tags for social media link previews
- `isManual` flag on time entries: shown as a badge in period detail view and exported as a `Type` column in CSV
- Overlap detection for manual entries: warns when the calculated start time conflicts with the previous entry, with options to correct the duration or auto-fill time since last entry
- Edit name button on every entry in the period detail view (any entry type)
- Restore backup now shows a confirmation dialog comparing backup vs current data before proceeding; warns when current data is newer than the backup
- Backup list shows the schema version of each backup
- Pause tracking: saves the current interval and suspends the timer; resume continues with accumulated work time; discarding a paused session deletes all saved intervals
- Recent entry name suggestions in all name/rename dialogs: scrollable chip row with distinct names from the last 7 days, tap to fill
- Auto-capitalize first letter in entry name fields
- Delete Project button moved to project detail screen, with confirmation dialog

### Changed
- Current period indicator in history replaced with a colored left-edge bar on the card
- Settings and Backup screens use standard scaffold toolbar instead of custom header
- Back navigation via device back button supported across all screens
- Add entry dialog: slightly larger inputs; entry name is a 2-line wrapping textarea
- Manual time entries now set `endTime = now` and `startTime = now - duration`, reflecting when the work happened rather than when it was entered
- "Add Project" button moved from FAB to the Projects section header row

### Fixed
- Backup restore incorrectly rejected v5 backups as "from a newer version"; also added the missing 4→5 migration step for the manual restore path

## [0.1.0] - 2026-02-16

- Initial release
