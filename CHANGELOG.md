# Feature Orchestrator Changelog

## [Unreleased]
### Added
- Minor bug fixes and improvements
- Improved copy texts
- Added tests for core functionalities

## [1.1.2] - 2025-12-18
### Fixed
- Fixed build failure related to PluginManagerConfigurable
- Fixed initial state issue when backlog is missing

## [1.1.1] - 2025-12-18
### Fixed
- Status initially "Awaiting AI Agent"

## [1.1.0] - 2025-12-18
### Added
- Enchanced UI
- Improved execution log
- Improved automatic validation
- Added more settings options
- Fixed minor bugs

## [1.0.0] - 2025-12-17
### Added
- MVP of Feature Orchestrator plugin for JetBrains IDEs.
- Automatically creates BACKLOG.md file if not present in the project.
- Cycle through features in BACKLOG.md and press Implement feature to generate AI prompts.
- Verify feature completion based on acceptance criteria using tests, build commands, or file presence.
- Mark features as completed in BACKLOG.md when acceptance criteria are met.
- Generate follow-up prompts for incomplete features to guide further AI work.
- Plugin settings to customize behavior and preferences.
- Have your features tracked in Git and easily check which changes are related to which feature.
