# Feather: Milestones

Companion to [FEATURES.md](FEATURES.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

## Working agreement
- Build **step by step**, pausing for review after each step of a milestone.
- Commit to git only when asked.
- Test device: not yet specified (model and Android version to be confirmed before M3, reminders).

## Milestone 1: Add, edit and complete tasks, in English and Arabic

**Definition of done:** tasks can be created, edited, completed and deleted on a phone; they survive an app restart; the app looks right in light and dark themes; it works in English and Arabic (RTL); the logic is tested.

### Steps
1. **Project setup**
   - `minSdk` 26; R8 on for release
   - Version catalog entries: Room, Hilt, DataStore, Navigation Compose, kotlinx.serialization, AppCompat
   - Verify Hilt and Room KSP work with AGP 9's built-in Kotlin (do this first; it is the likeliest source of trouble)
   - Package skeleton; `LocaleConfig` (en, ar)
2. **Data layer**
   - Room schema v1 with `task` and `list` only: UUID ids, `updatedAt`, soft delete, schema export on
   - DAOs, `TaskRepository`, Hilt modules
   - Seed a default "Inbox" list on first launch
   - Tests: DAO tests on an in-memory DB
3. **App shell**
   - Material You dynamic color, light and dark themes
   - Edge-to-edge; type-safe navigation between list and edit screens
4. **Screens**
   - Task list: tap checkbox to complete, swipe to delete with undo snackbar (swipe direction mirrors in RTL), empty state with a placeholder for the future bird
   - Quick add: bottom sheet with a title field (parser comes in M5)
   - Task edit: title, notes, due date and time (floating local time), priority
   - ViewModels expose `StateFlow<UiState>`
5. **Internationalization**
   - All strings in resources, with an Arabic translation
   - Language setting (English, Arabic, system default) via per-app locales
   - `start`/`end` everywhere; verify gestures and icons mirror
6. **Tests**
   - ViewModel unit tests
   - One Compose test: add, complete, delete
   - Screenshot test of the main screen in LTR and RTL

### Not in M1
Reminders, recurrence, tags, folders, subtasks, search, smart views, widgets, parser, voice, backup. The data model leaves room for them, but nothing is built.

### Optional
A throwaway exact-alarm spike at the end of M1 to learn how reminders behave on the test device before M3.

## Roadmap

| # | Milestone | Main content |
|---|---|---|
| M1 | Foundation | See above |
| M2 | Organization and views | Lists, one-level folders, tags, subtasks, search (FTS), Today / Upcoming / Overdue |
| M3 | Reminders | Scheduler, receivers, notification actions, reliability helper, real-device testing |
| M4 | Recurrence | Rule engine, repeat modes, completion history |
| M5 | Quick add | Natural-language parser (EN and AR), voice input, share target, Quick Settings tile |
| M6 | Widgets and backup | Glance widgets, JSON export and import, automatic backup |
| M7 | Polish | Accessibility pass, performance, store preparation |
| M8 | Brand | Bird identity and animation |

Reminders (M3) come before recurrence and the parser because they carry the most real-world risk (vendor battery policies), and it is better to learn about them early.
