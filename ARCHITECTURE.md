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
| Storage | Room (SQLite); `LIKE` search | Relational data. FTS was dropped for M2 (see MILESTONES.md); add it later only if search gets slow. |
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

## Decisions log

### M2 (organization and views)
- **Schema v2** adds `folders`, `tags`, `task_tags`, `tasks.parentTaskId`/`isPinned` and `task_lists.folderId`/`colorArgb`, migrated with `@AutoMigration(1, 2)`. Subtasks are rows of `tasks` that point at their parent; they are grouped under it when read and are soft-deleted and restored together with it.
- **Views are pure functions.** The repository exposes every non-deleted top-level task with its tags and subtasks; `domain/view` (`TaskScope`, `inScope`, `comparator`, `groupedByDay`, `search`, `openCountIn`) turns that into Today, Upcoming, list, tag and search results. Nothing in it touches Android or the database.
- **Transactions** go through `TransactionRunner` (Room's `withTransaction` underneath) so repositories stay testable.
- **Deleting** a list moves its tasks to the Inbox; deleting a folder keeps its lists; deleting a tag hides it everywhere (soft delete). The Inbox is protected.
- **"Today"** is computed in the user's time zone (`Clock` is UTC, so the ViewModel converts).

### M3 (reminders)
- **Schema v3** adds `reminders` (kind: before due, at a time, snooze; offset or local date and time; `firedForTrigger`; bookkeeping), migrated with `@AutoMigration(2, 3)`. Reminders hang off a task and are saved with it in `saveTaskDetails`; a pending snooze is never touched by the editor.
- **All timing decisions are pure functions** in `domain/reminders` (`triggerAt`, `planReminders`, `nextDailySummary`); the Android side only arms alarms and posts notifications. Local times are interpreted in the user's current zone each time, so a time zone change just needs a re-evaluation.
- **One alarm for reminders, one for the daily summary.** `AlarmReminderScheduler.reschedule()` is the single entry point and is serialized with a mutex; it re-reads the tasks, shows what is due (including catch-up within 24 hours), marks it fired, and arms the next alarm. It is called by the alarm, by `ReminderSyncer` (on every task or settings change), by the system-event receiver (boot, app update, time, time zone, exact-alarm permission) and by a 12-hour WorkManager job.
- **Receivers and the worker get their dependencies through a Hilt entry point**, not `@AndroidEntryPoint`, because Kotlin cannot call the abstract `BroadcastReceiver.onReceive` that Hilt's generated base class relies on.
- **Notifications** are one per task (task id as the tag), built with the app language even before Android 13 (`withAppLocale`), and cleaned up when the task is finished elsewhere.
- **The reliability helper reads real state** (notification permission and channel, exact-alarm permission, battery optimization) and links to the matching system screens; for phone makers with their own limits it explains the manual steps and links to dontkillmyapp.com. It deliberately does not use the restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission.

### M4 (recurrence)
- **Schema v4** adds `tasks.rrule` and `tasks.repeatMode` and the `completion_history` table, migrated with `@AutoMigration(3, 4)`. A repeating task stays one row whose `dueDate` moves forward; nothing is copied.
- **The rule engine is pure Kotlin** in `domain/recurrence` (`Recurrence`, `nextOccurrence`, `afterAdvance`), with no library. The rule string is an RFC 5545 subset (`FREQ`, `INTERVAL`, `BYDAY`, `BYMONTHDAY`, `BYMONTH`, `UNTIL`, `COUNT`) that the parser reads case-insensitively and rejects when it does not understand it (the task then simply does not repeat). `COUNT` is the number of occurrences still to come, so no start date is stored.
- **Completion lives in the repository.** `TaskRepositoryImpl.setCompleted` runs in one transaction: history row, next date (or final completion), checklist reset, and reminders moved with the date (specific-time reminders shift by the same number of days; a pending snooze is dropped; all become eligible to fire again with a new `updatedAt` so a trigger already in the past is not replayed). The list, search and the notification action all go through it.
- **"Today" for a completion is the user's day** (`Clock` is UTC, converted with the system zone), the same convention as the task list.

### M5 (quick add)
- **The parser is pure Kotlin in `domain/quickadd`.** `QuickAddParser.parse(text, now, listNames, disabled)` returns a `ParsedQuickAdd`; `now` and the list names are passed in, so nothing depends on Android or on a clock. Language packs implement `LanguageRules` (repeat, time, date); English and Arabic both run on the same text.
- **Matching is done on a normalized copy** of the text (`QuickText`): lower case, ASCII digits, one spelling for alef, ya and ta marbuta, no diacritics. Recognized words are blanked in that copy and removed from the user's original text by an index map, so the title keeps the user's own letters, hamza and diacritics.
- **Symbols are language-neutral:** `#tag`, `@list` (only for an existing list; spaces as `_`) and `!priority`. Everything else is words.
- **Switching a field off re-runs the parser without that field** (`disabled`), so its words stay in the title. A date that is only implied (a time alone, a repeat alone) has no chip of its own.
- **Saving is shared:** `QuickAddTask` turns a `ParsedQuickAdd` into a task through the repositories. The task list ViewModel and `QuickAddViewModel` both use it; the text overrides what the current view would suggest.
- **One see-through `QuickAddActivity`** hosts the same `QuickAddSheet` for the share sheet (`ACTION_SEND` `text/plain`, collapsed to one line), the Quick Settings tile (`TileService`; `PendingIntent` on Android 14+) and a static launcher shortcut. Its write is wrapped in `NonCancellable` so it finishes while the screen closes.
- **Voice** is `RecognizerIntent` (no microphone permission); offline is requested but not required, so the system recognizer may use the network.

## Open items
- Donation mechanism (external link vs. Play flow) and Play policy implications
- Exact-alarm permission strategy (see Reminders)
- Animation technology and bird brand direction
