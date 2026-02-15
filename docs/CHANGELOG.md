# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Project website (Jekyll, in `website/` subdirectory)
- GitHub Pages deployment workflow (currently disabled until Pages is enabled)
- Release documentation (`docs/RELEASE.md`)

### Changed
- Release workflow now builds unsigned APK when signing secrets are not configured
- Release workflow only builds AAB when signing is available (Play Store requires it)
- CI workflow only runs tests (removed debug APK build to save Actions minutes)

### Fixed
- Release workflow heredoc indentation bug that would produce invalid `keystore.properties`
- Incorrect author name across LICENSE, README, and website

### Removed
- 15 internal development notes from `docs/` (BUILD_FIX_APPLIED, ICON_FIX, TEST_*, etc.)
- Old package namespace schemas (`com.howmanyhours.data.database.AppDatabase`)
- Disabled migration test file
- Stray `install.sh.backup`

## [1.0.0] - TBD

### Added
- Initial release
- Time tracking functionality
- Data backup and restore
- CSV export
