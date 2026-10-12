# Feather: Architecture

Companion to [FEATURES.md](FEATURES.md). Decisions below were agreed during planning (2026-10-12); no code exists yet beyond the Android Studio template.

## Starting point (generated project)
- Compose + Material 3, AGP 9.2.1, Kotlin 2.2.10, package `com.elkrrai.feather`
- **To fix at setup:** `minSdk` is 27 and should be **26**; release builds have R8 off (`isMinifyEnabled = false`) and it should be **on**.

## Shape
- **Single Gradle module (`:app`), package-by-feature.** The `domain` package must never import Android classes, so the module can be split later if needed.
- **UI (Compose) -> ViewModel -> Repository -> Room.** Each screen exposes one `StateFlow<UiState>` and receives events from the UI (unidirectional data flow).
- Use cases only where there is real logic (recurrence, completing a task, rescheduling reminders). Plain CRUD goes straight through repositories.

```
com.elkrrai.feather
├── data/        Room (entities, DAOs), DataStore, repositories, backup
├── domain/      models, recurrence, reminder-time logic, NL parser   (pure Kotlin)
├── reminders/   AlarmManager scheduler, receivers, notifications
├── feature/     today/, upcoming/, tasklist/, taskedit/, search/, settings/
├── widget/      Glance widgets
├── ui/          theme, shared components, (later) bird/animation layer
└── di/          Hilt modules
```

## Technology choices
| Area | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3 (Material You) | Already set up; dynamic color support |
| Storage | Room (SQLite) + FTS for search | Relational data, fast full-text search |
| Settings | DataStore | Replacement for SharedPreferences |
| DI | Hilt | Best integration with ViewModel, WorkManager, Glance |
| Navigation | Navigation Compose, type-safe routes | Stable, well documented |
| Serialization | kotlinx.serialization | Backup, export, import |
| Async | Coroutines + Flow | Room and DataStore expose Flow |
| Widgets | Jetpack Glance | Compose-style API over the same repositories |
| Language switching | AndroidX per-app locales (`AppCompatDelegate.setApplicationLocales` + `LocaleConfig`) | Works on API 26+, integrates with system settings on 33+ |
| Dates | `java.time` | Native at minSdk 26, no desugaring needed |

Keep dependencies lean to protect APK size: no Lottie, Firebase, or Retrofit unless a concrete need appears.

## Key design decisions

### 1. Time model
A task due at "5pm" stays at 5pm if the user changes timezone. Due dates are stored as a **local date + optional local time** (floating), not as an epoch instant. Reminder trigger instants are **computed at scheduling time** and recomputed on timezone or clock change.

### 2. Recurrence
- Stored as an **RRULE-style string** plus a `repeatMode` (fixed schedule vs. repeat after completion).
- Completing a recurring task **advances its due date in place** and appends a row to `completion_history` (also the basis for streaks later).
- Use a small RFC 5545 library (e.g. `lib-recur`) or a limited in-house subset. Avoid heavyweight iCal libraries.
- Recurrence is the trickiest logic in the app: design and test it first.

### 3. Reminders
- **AlarmManager** is the primary mechanism, with WorkManager only as a safety net.
- Schedule exact alarms only for each reminder's *next* occurrence (stays far below the per-app alarm limit).
- Receivers for boot, time change and timezone change reschedule everything. A daily WorkManager job re-syncs alarms in case a vendor killed them.
- `SCHEDULE_EXACT_ALARM` needs a user grant on Android 14+. `USE_EXACT_ALARM` is restricted by Play policy to alarm and calendar apps, so **verify eligibility before relying on it**. The in-app reliability helper (battery optimization and exact-alarm guidance) is required either way.
- Hide scheduling behind a `ReminderScheduler` interface so the logic is unit-testable.
- Notification actions: complete, snooze, open.

### 4. Natural-language parser
- Pure Kotlin in `domain/`, no Android dependencies.
- Pipeline: normalize (including Arabic-Indic digits) -> extract date, time, priority, tag, list -> remaining text becomes the title.
- Per-language rule sets (English, Arabic), rule-based to start.
- Table-driven unit tests with hundreds of phrases per language; this is the safety net.

### 5. Voice input
- v1 uses the system `RecognizerIntent`: no `RECORD_AUDIO` permission and no custom UI, with the result fed into the parser.
- **Known trade-off:** the system recognizer may send audio to Google's servers, which conflicts with a strict "no network" claim. Alternative later: a custom `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE`, at the cost of more UI and the mic permission. State this honestly in the store listing.

### 6. Backup
- Versioned JSON (`"version": 1`) with explicit migrations.
- Written through the Storage Access Framework to a user-chosen folder; a periodic WorkManager job handles automatic backups.
- Room migrations are always written by hand. No destructive fallback.

### 7. Data model (sketch)
- `task`: id (UUID), listId, parentTaskId (subtasks), title, notes, dueDate, dueTime, priority, rrule, repeatMode, completedAt, sortOrder, updatedAt, deletedAt
- `list`, `folder` (one level), `tag`, `task_tag`, `reminder`, `completion_history`
- **Every table:** UUID primary key, `updatedAt`, soft delete, so sync can be added later without a rewrite.
- Trash purges soft-deleted rows after 30 days.

### 8. RTL and i18n
- `start`/`end` everywhere, never `left`/`right`; all strings in resources; Arabic `plurals` with all forms.
- Mirror swipe directions and directional icons.
- Locale-aware dates and numbers, optional Arabic-Indic digits.
- Screenshot tests in both LTR and RTL from early on.

### 9. Animation and brand
- Reserve an `ui/` animation layer and slots (icon, empty states, completion moment, loading) so bird animation can be added without refactoring.
- Respect the system "reduce motion" setting. Technology (Compose-native vs. Lottie vs. Rive) is deferred to the brand discussion.

## Testing
- **Unit:** parser, recurrence, reminder-time calculation, backup migration (the bulk of the tests).
- **Room:** in-memory database tests, including migrations.
- **UI:** a few Compose tests for the main flows; screenshot tests for LTR/RTL.
- **Manual:** reminder reliability on real devices across vendors (Xiaomi, Samsung, Oppo). Emulators are not a substitute.

## Setup checklist (before the first feature)
- [ ] Set `minSdk = 26`; enable R8 for release
- [ ] Confirm Hilt and Room KSP plugin versions work with AGP 9's built-in Kotlin
- [ ] Add dependencies: Room, Hilt, DataStore, Navigation Compose, kotlinx.serialization, WorkManager, Glance, AppCompat (per-app locales)
- [ ] Configure `LocaleConfig` with `en` and `ar`
- [ ] Create the package skeleton above
- [ ] Set up a unit-test baseline and an RTL screenshot test

## Open items
- Donation mechanism (external link vs. Play flow) and Play policy implications
- Exact-alarm permission strategy (see Reminders)
- Animation technology and bird brand direction
