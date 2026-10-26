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
- `task_list` (only the hidden Inbox, see below), `tag`, `task_tag`, `reminder`, `completion_history`
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
- **Deleting** a tag hides it everywhere (soft delete) and moves the tags under it up one level.
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

### M6 (widgets and backup)
- **The backup file is its own format** (`data/backup/BackupFile`): versioned JSON records that mirror the tables but are separate classes, so Room can change without breaking old files. Soft-deleted rows are included. Reading checks `format` and `version` before decoding the rest (every record field has a default, so a random JSON object would otherwise pass as an empty backup), ignores unknown fields, and refuses newer versions.
- **Merging is a pure function** (`BackupMerger`): per record the newer `updatedAt` wins, same-named live tags are joined, every imported task goes into the Inbox (older files name lists, which are ignored), and tag links and reminders are taken only for tasks whose record came from the file. Replace is "delete everything, then merge into an empty phone". The whole import is one transaction, and the file is decoded before anything is touched.
- **Writes are upserts**, never delete-and-insert, so cascading foreign keys cannot remove a task's reminders or history while it is rewritten.
- **Files go through the Storage Access Framework** (`BackupFiles`, `DocumentFile`); the app needs no storage permission and no network. The automatic backup keeps a persisted tree permission, writes `feather-auto-backup-DATE.json` (a prefix of its own, so manual exports in the same folder are never pruned), keeps 7, and runs from a daily WorkManager job that exists only while it is switched on and a folder is set.
- **Widgets are Jetpack Glance** (`widget/`): `TodayWidget` reads through a Hilt entry point, `CompleteTaskAction` finishes a task through the same `setCompleted` as everywhere else, `WidgetUpdater` redraws on task changes (debounced), and the system refreshes every 30 minutes, which rolls the list over at midnight. `QuickAddWidget` only opens `QuickAddActivity`.
- **Android's own Auto Backup** is limited by `data_extraction_rules.xml` and `backup_rules.xml` to the task database and the settings file.

### M7 (polish)
- **Display choices live in `DisplayPrefs`** (`ui/DisplayPrefs.kt`): a small object of snapshot state that the app writes from the saved settings at start and on every change, and that formatting, theme and haptics code reads. This keeps the clock format and first weekday out of every function signature, and lets notifications and widgets format times the same way as the screens. The price is global state; it is written in one place only.
- **Light and dark go through `AppCompatDelegate.setDefaultNightMode`**, not a flag passed to the theme, so system bars, dialogs and `isSystemInDarkTheme()` agree. Pure black and Material You are theme parameters that default to `DisplayPrefs`. The saved settings are read once, blocking, in `Application.onCreate` so the first frame already has the right colors.
- **Selection** is ViewModel state (`pickedIds`), always intersected with what is on screen, and cleared when the view changes. Bulk actions go through the same repository calls as single ones.
- **Manual order** swaps the `sortOrder` values the dragged tasks already have, so tasks that are not in the list keep their place. `Created` sorts by `createdAt` only, otherwise dragging would rewrite creation order. The list being dragged is a local copy (`ReorderState`) committed when the finger lifts; TalkBack users get Move up and Move down actions instead of a drag.
- **Saved filters were removed** (schema v7 drops `saved_filters` and `folders`): the menu has five fixed views and the tag tabs narrow them, so a filter had nothing left to do.
- **Links in notes** are found by a pure function (`findLinks`) and shown as chips; the notes field stays plain text.
- **Release.** `proguard-rules.pro` lists only what the libraries do not bring (serializers by name, the two workers); the signing details come from an untracked `keystore.properties`; only English and Arabic library resources are kept.

### M8 (brand)
- **The mascot is an owl.** `ui/brand/OwlIllustration.kt` stacks layered PNGs (one set per pose: resting, sleeping, flying) rendered from the Recraft SVGs in `art/owl` by `art/owl/render.py`; the SVGs are not in the APK. The movement is code: `BirdPose.kt` gives the poses (`birdPose`, `birdFlightPose`) and every screen asks for a mood through `BirdIllustration`. What is still the old blue bird is listed in `art/README.md`.
- **The old bird.** The blue songbird is gone from the app: its drawing code, its shapes, its drawable and the tests that kept its copies in step are removed. Every place that showed it now shows the owl, and Settings shows the owlet.
- **Animation is a pure function plus layers.** `birdPose(mood, phase)` and `birdFlightPose(progress)` return wing angle, body offset, eye opening, scale and tilt (in a 96-unit box, `POSE_BOX_UNITS`); the owl composables in `OwlIllustration.kt` and `SideOwl.kt` move their picture layers by them. The phase is a `State` read while drawing, so a running animation redraws but does not recompose. A phase of 0 is a calm still pose in every mood, which is what is shown when movement is off.
- **Motion is opt-out twice:** `rememberBirdMotionAllowed()` is false when the Settings switch is off or the system `ANIMATOR_DURATION_SCALE` is zero.
- **The launch screen** uses the AndroidX splash-screen library: `MainActivity` has the `Theme.Feather.Starting` theme, calls `installSplashScreen()` first, and the library swaps in `Theme.Feather` afterwards. The animated icon is an animated vector (the wing group rotates).
- **The palette replaces the placeholder** in `Color.kt` and `Theme.kt`; Material You is applied only when the switch is on. The template's bitmap launcher icons were deleted since every supported Android version uses the adaptive icon.

### M9 (Cozy Nest)
- **Theme tokens in `ui/theme`.** `Color.kt` holds the Nest palette and the three Material schemes (light, dark, black); `FeatherColors.kt` holds what Material has no slot for (card, priority tints, list colors, heat ramp, sky washes) and is read as `MaterialTheme.featherColors`; `Tokens.kt` holds shapes, spacing, elevation, durations and easings; `Type.kt` builds the type scale per language (`featherTypography(arabic)`), picked in `FeatherTheme` from the current locale.
- **Contrast is a test.** `NestContrastTest` checks 4.5:1 for text pairs and 3:1 for graphics in all three themes, so a color change that hurts readability fails a build.
- **Plumage is for art only.** Cream text on Plumage is 4.1:1, so buttons and links use `primary` (`#1F6C88`).
- **Fonts are variable.** One file per family serves every weight through `FontVariation.weight`; fine because minSdk is 26.
- **The window background is the paper color** (`nest_paper`), so there is no white flash before the first frame.

#### M9 steps 6 to 11
- **Your rhythm is computed, not stored.** `RhythmRepository.observeCompletions()` joins `completion_history` with `tasks` (title, list) and `rhythmStats` (pure, in `domain/rhythm`) turns the list into everything the screen draws; the screen calls it with its own locale's first weekday. Paused days are the one thing saved (a string set in DataStore).
- **One clock for the charts.** `rememberEntry()` is a single 0 to 1 `Animatable`; each Canvas takes a slice with `segment(entry, startMs, durMs)`, so the whole entrance is 1.1 s and charts never run their own timers.
- **A repeating task rests.** `Task.lastCompletedAt` comes from the latest completion row (`observeTasks` combines two flows); a repeating task finished today whose next date is later is "resting": the list view leaves it out of the open tasks and shows it, ticked, in Done. Unticking it calls `undoAdvance` in the repository.
- **Cards and celebrations are small view models and logs.** `DayCardsViewModel` owns the morning and evening cards (and the nest's quiet dot); `DayCelebrationLog` keeps the once-a-day "all done" in the hints preferences. The header owns the celebration's animation (`flourish`) and tells the screen where the bird is so the screen can throw the warm feathers.
- **The nest is tracked in the data layer.** `NestTracker` (a singleton started with the app) combines completions, tasks, tags and settings into `NestFacts`; whatever `earnedNestItems` says is earned is saved to DataStore once and kept. `NestScene` draws the items in the same scene kit as the empty states.
- **Widgets cannot read the theme,** so they carry their own day and night values (`WidgetColors`, `values/widget_colors.xml`) from the same Nest palette.

#### M9 step 5: quick-add polish
- **Spans from the parser.** `QuickText.runAs(field) { ... }` notes which characters a rule consumed and turns them into `TokenSpan`s over the user's own text (diacritics stay with their letter, like in the title). `ParsedQuickAdd.spans` is a body property of the data class, so it is not part of equality and a `copy` forgets it; whatever shows the text asks the parser again.
- **Tinting is a `VisualTransformation`.** The text field's value stays a plain string; the transformation adds a color and background to the spans and keeps the cursor mapping the identity. Chips and tinted words share one `fieldTint` per field.
- **Tools only edit text.** `QuickAddEdits` (append, remove a field's words, replace them) is the whole interface between the tool buttons and the parser. There is no second copy of the task's settings in the sheet, so what the chips show is always what the text says.
- **Chips animate by slot.** Every part has a fixed slot in the `FlowRow` inside an `AnimatedVisibility`, so appearing and disappearing are separate, interruptible animations; a slot remembers its last words so a chip can shrink away after its part stopped being recognized.
- **Landing.** The list state carries `justAddedId` for about a second; the card tint is an `animateColorAsState` with an 800 ms fade, nothing in the data layer.

#### M9 step 4: the illustration kit
- **Scenes are code, on a board.** Each of the eight scenes (`ui/brand/scenes`) is a 200 by 200 board drawn with Compose: a back layer, the live bird, a front layer. The bird is the animated `BirdIllustration`, placed by one `BirdPlacement` (position, scale, tilt); the props are small `DrawScope` functions in `SceneProps.kt`, shared by the scenes. Changing a scene means changing a few lines of coordinates.
- **One frame for all** (`SceneFrame`): scales the board to the requested size, fades and lifts it in, runs the leaf sway and the first-launch sunrise, and honors reduced motion. Scenes have no semantics; the sentence next to them carries the meaning.
- **Size is a pure rule** (`sceneSizeDp`): 190 dp, 140 dp at 200 percent font, nothing under 320 dp of free height. `EmptyState` measures its space and applies it.
- **Screens choose a scene by view.** `TaskScope.emptyCopy()` maps an empty view to a scene and its words; trash, search, the reliability screen and the backup dialog use theirs directly.

#### M9 step 3: the Today header and richer rows
- **Pure logic first.** Greeting and sky boundaries, the bird's mood for the day, how many leaves are filled (`ui/today/TodayLogic.kt`), the choice of date label (`ui/format/FriendlyDue.kt`), the list color/shape mapping (`ui/theme/ListColors.kt`), barb strokes and leaf progress are plain functions with tests; the composables only draw them.
- **What Today shows comes from the list state.** `TaskListUiState` has `today` (open tasks due today or earlier, tasks finished today) and, for the Today view only, `doneToday`. Nothing is added to the repositories.
- **The header is position-driven, not animated.** Its height is read from a `HeaderCollapseState` in a `layout` modifier and its content fades through `graphicsLayer`, so scrolling never recomposes it. A `NestedScrollConnection` on the parent closes it before the list scrolls and opens it again when the list is at its top.
- **The list screen's body is a local function** (`listBody`) so the same list code sits either alone (every other view, under a top bar) or under the Today header; the scaffold's insets and top bar change with the view.
- **Rows are cards, built by hand** instead of Material's `ListItem`: ring, title and a `FlowRow` of `MetaChip`s. Accessibility is kept: the ring is named after the task and its priority, the card carries the open, select, complete, delete, pin and move actions, and waiting tasks announce "Waiting".
- **List colors** are stored as ARGB as before; `listColorIndex` maps a saved color (current or from an earlier palette) to one of eight positions, and the theme supplies the actual color and a shape for that position, so list colors adapt to light and dark themes.

#### M9 step 2: the completion moment
- **One reduced-motion rule.** `rememberReduceMotion()` is true when the phone's animation scale is zero or Reduce motion is on in Settings. Every animation below has a quiet version for it. Little celebrations is a separate switch (feathers and the bird's hop), because someone may want calm screens but still enjoy the small reward, or the reverse.
- **The ring is one composable** (`CompletionRing`): a 48dp toggle target, a 26dp ring drawn in a Canvas, three `Animatable`s (fill, check, press scale) and one for the burst, all read while drawing so a running animation redraws without recomposing. The check is a path trimmed with a `PathMeasure` that is built once.
- **The strike is drawn, not a text decoration** (`StrikeText`): lines are struck one after another from the reading-direction start (right edge for Arabic), using the text layout's line positions. `strikeFractions` is the pure part.
- **The burst is a `drawBehind` modifier** with fixed particle data (`burstParticles`) and one pre-built leaf path; it draws outside the 48dp box without affecting layout. With reduced motion a halo replaces it.
- **Finished rows are kept by the screen, not by the data.** `TaskListContent` holds a small map of tasks finished in the last 420 ms; `withFinishing` merges them back into what the database emitted. The ViewModel and repositories are unchanged, and an undo tap simply removes the entry. Manual-order lists keep their own draggable copy of the order, so rows leave at once there.
- **Haptics.** The ring uses the light segment tick; the stronger confirm is kept for the once-a-day "all done" moment (step 9).

## Open items
- Donation mechanism (external link vs. Play flow) and Play policy implications
- Exact-alarm permission strategy (see Reminders)
- Animation technology and bird brand direction

### After M9: no lists, folders or saved filters
- **Why `task_lists` and `tasks.listId` are still in the database.** `tasks.listId` is a foreign key to `task_lists`, and SQLite can only remove a foreign key by rebuilding the `tasks` table. Rebuilding a table that tags, reminders and history point to (with cascading deletes) is the one migration that could wipe a person's data, so it is not done. Every task is kept in one hidden list, the Inbox (`INBOX_LIST_ID`, created by `InboxRepository.ensureInbox()` at start); `Task` has no `listId`, `Task.toEntity()` always writes the Inbox, and lists made earlier stay as unused rows.
- **Schema v7** (`AutoMigration(6, 7)` with `@DeleteTable` for `folders` and `saved_filters`) only drops two tables nothing refers to.
- **Tags form a tree** (`tags.parentId`, schema v6): `branchIds` gives a tag and everything under it, `wouldLoop` refuses a move that would make a loop, and the tag tabs, the tag filter and search use the branch.
- **Backup files** no longer carry folders, lists or filters; older files that do are still read (unknown fields are ignored) and their tasks go into the Inbox.
