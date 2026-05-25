# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]

### Added
- App screenshots in website feature sections with two-column layout
- Open Graph and Twitter Card meta tags for social media link previews
- `isManual` flag on time entries: shown as a badge in period detail view and exported as a `Type` column in CSV
- Overlap detection for manual entries: warns when the calculated start time conflicts with the previous entry, with options to correct the duration or auto-fill time since last entry
- Edit name button on every entry in the period detail view (any entry type)
- Restore backup now shows a confirmation dialog comparing backup vs current data (projects, entries, last entry date) before proceeding; warns when current data is newer than the backup
- Backup list shows the schema version of each backup

### Changed
- Manual time entries now set `endTime = now` and `startTime = now - duration`, so they reflect when the work happened rather than when it was entered
- "Add Project" button moved from floating action button to the Projects section header row, preventing it from obscuring the delete button on the last list item when scrolled

### Fixed
- Backup restore incorrectly rejected v5 backups as "from a newer version" due to the current schema version being hardcoded as 4 in the validator; also added the missing 4→5 migration step for the manual restore path

## [0.1.0] - 2026-02-16

- Initial release
