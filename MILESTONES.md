# Feather: Milestones

Companion to [FEATURES.md](FEATURES.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

## Working agreement
- The **"humbug"** workflow: for each step write the code, commit, push and go straight to the next step, with no compile, tests or install in between. When the milestone is finished, build and install the app on the phone. Unit tests are not run unless asked.
- Commit to git only as part of that workflow or when asked.
- The **"flash"** workflow: plan the milestone, develop it with one commit per step, push once when it is finished, then brief the user on the next milestone. Development only: no build, install or tests unless asked.
- Test device: OnePlus/Oppo CPH2625, Android 16 (confirm vendor-specific reminder behavior in M3).

## Roadmap

| # | Milestone | Main content | Status |
|---|---|---|---|
| M1 | Foundation | Tasks, Room, navigation, English and Arabic (RTL) | Done |
| M2 | Organization and views | Lists, one-level folders, tags, subtasks, search, Today / Upcoming / Overdue, trash | Done (built and installed; the 1 -> 2 database upgrade kept existing data) |
| M3 | Reminders | Scheduler, receivers, notification actions, reliability helper, real-device testing | Built; install and real-device testing still pending |
| M4 | Recurrence | Rule engine, repeat modes, completion history | Code complete (developed with the "flash" workflow: not built, installed or tested) |
| M5 | Quick add | Natural-language parser (EN and AR), voice input, share target, Quick Settings tile | Code complete (flash workflow: not built, installed or tested) |
| M6 | Widgets and backup | Glance widgets, JSON export and import, automatic backup | Code complete (flash workflow: not built, installed or tested) |
| M7 | Polish | Accessibility pass, performance, drag to reorder, bulk select, store preparation | Code complete (flash workflow: not built, installed or tested) |
| M8 | Brand | Bird identity and animation | In progress (flash workflow) |

Reminders (M3) come before recurrence and the parser because they carry the most real-world risk (vendor battery policies).

## Milestone 1: Foundation (done)
Tasks with title, notes, due date and time, priority; Room persistence with an Inbox list; task list with swipe-to-delete and undo; quick-add sheet; task editor; Material You theme; type-safe navigation; English and Arabic with a language picker and RTL-safe layouts. Verified by a clean build, an install on the phone, and 11 passing JVM unit tests.

## Milestone 2: Organization and views

**Definition of done:** tasks can be organized into lists (optionally inside folders), tagged, given subtasks and pinned; the user can browse Today, Upcoming, Overdue, All, Completed and No-date views, filter by list or tag, search, sort, and restore deleted tasks from a trash; existing M1 data survives the upgrade; everything works in English and Arabic.

### Decisions made for M2
- **Schema v2 with an automatic migration** (`@AutoMigration(1, 2)`), because the phone already holds real data. No destructive fallback.
- **Search is an in-memory, case-insensitive "contains"** over title, notes, tag names and subtask titles, not FTS. Room's external-content FTS needs sync triggers that auto-migrations do not create, and a personal todo list is small. FTS can be added later if search ever feels slow.
- **Filtering happens in memory.** The repository exposes all non-deleted tasks (with tags); smart views, list views, tag views and search are pure functions over that list in `domain/`, so they are cheap to unit test. Fine for thousands of tasks.
- **Subtasks are checklist items**: one level, title and done state only (no due date, priority or tags of their own). Deleting or restoring a parent applies to its subtasks together.
- **Deleting a list moves its tasks to the Inbox** (nothing is lost). Deleting a folder moves its lists out of the folder. The Inbox cannot be deleted or renamed.
- **Pinned tasks sort first** within a view.
- **Sort options** (created order, due date, priority) are remembered for the session only; persistent settings come later.

### Steps
1. **Schema v2 and domain models**
   - New entities: `FolderEntity`, `TagEntity`, `TaskTagCrossRef`. New columns: `tasks.parentTaskId`, `tasks.isPinned`; `task_lists.folderId`, `task_lists.colorArgb`.
   - `AutoMigration(1 -> 2)`; DAOs for folders, tags, and the extended task and list queries (including `@Relation` task-with-tags).
   - Domain: `Tag`, `Folder`, extended `Task` (parentTaskId, isPinned, tags) and `TaskList` (folderId, colorArgb); mappers; DI.
2. **Repositories and view logic**
   - List and folder CRUD, tag CRUD, task tags, subtask replacement, pin, move to list, trash (observe, restore, delete forever, purge after 30 days at app start), `LIKE` search.
   - `TaskScope` (Today, Upcoming, Overdue, All, Completed, NoDate, InList, WithTag) and pure selection, sorting and grouping functions.
3. **Navigation drawer and list organization**
   - Drawer: smart views with counts, folders and lists, tags; create, rename, recolor, move and delete lists, folders and tags.
   - The task screen follows the selected scope; quick add uses the selected list (and today's date in Today).
   - Upcoming grouped by day.
4. **Task details**
   - Editor: list picker, tag picker (create inline), subtask checklist, pin toggle.
   - List rows: tag chips, subtask progress ("2/5"), pin marker.
5. **Search and sort**
   - Search screen with live results; sort menu (created, due date, priority).
6. **Trash**
   - Trash screen: restore, delete forever, empty trash; 30-day purge.
7. **Translations, tests, docs**
   - Arabic strings for everything new; unit tests for scope and sorting logic, repository tests, UI tests (written, run only when asked); update FEATURES.md and ARCHITECTURE.md.

### Known gaps after M2
- There is no automated test for the 1 -> 2 migration yet. It needs the exported schemas (`app/schemas`) added to the instrumented-test assets. The migration was verified manually on 2026-10-12 by installing over the M1 database on the phone (version 2, new tables and columns present, existing task kept).
- The new unit and instrumented tests were written but not run (the humbug workflow runs neither).

### Deferred from M2
Drag to reorder, bulk select, duplicate task and saved custom filters (moved to M7 polish), list color picker beyond a fixed palette.

## Milestone 3: Reminders

**Definition of done:** a task can have several reminders; each one shows a notification on time (including after a reboot, a time or timezone change, or an app update); the notification offers Complete, Snooze and Open; the user can snooze for a preset or custom time; a settings screen and a "reliability helper" show what is blocking reminders on the phone (notifications, exact alarms, battery optimization, vendor auto-launch) and can send a test reminder; an optional daily summary works; everything works in English and Arabic.

### Decisions made for M3
- **Reminder kinds.** `BEFORE_DUE` (minutes before the due moment, 0 = at due time; needs a due date), `AT_TIME` (a specific local date and time), and a hidden `SNOOZE` (a one-shot local time created by snoozing; never shown in the editor and deleted after it fires). Date-only tasks use the **default reminder time** (setting, 09:00) as their due moment.
- **Floating time.** Reminders store local dates and times; the trigger instant is computed when scheduling, in the user's current time zone, and recomputed on a time zone change.
- **One alarm at a time.** The scheduler keeps a single alarm for the earliest pending reminder. When it fires, every due reminder is shown, marked as fired, and the next alarm is set. This stays far below the system alarm limit. Safety nets: a receiver for boot, app update, time change, time zone change and exact-alarm permission change, a daily WorkManager job, and a re-schedule at app start and on every task change.
- **Fired state** is stored per reminder (`firedForTrigger`), so a reminder fires once per trigger time. Reminders missed while the phone was off fire as a catch-up (within 24 hours) when the phone is back, but a reminder the user just created or edited into the past does not fire.
- **Exact alarms.** Use the user-granted `SCHEDULE_EXACT_ALARM` (not `USE_EXACT_ALARM`, which Play restricts to alarm and calendar apps) with `setExactAndAllowWhileIdle`; without the permission fall back to `setAndAllowWhileIdle` (may be late) and nudge the user through the reliability helper. **Open item:** confirm the current Play policy and Permissions Declaration requirements before publishing.
- **No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** (restricted on Play); the helper opens the system battery-optimization list instead, plus app settings and a vendor guide link for Oppo, OnePlus, Xiaomi, Huawei, Samsung and others.
- **No INTERNET permission** is added.
- **Snooze** uses a transparent activity with presets (10 min, 30 min, 1 hour, 3 hours, tomorrow morning) and a custom amount; the notification also has a one-tap snooze using the default snooze setting.
- **Reminders are hard-coded to one notification per task**, even when several reminders of that task fire together.
- WorkManager is used only as a safety net, through a Hilt entry point (no WorkManager Hilt integration needed).

### Steps
1. **Schema v3 and trigger logic.** `reminders` table (auto-migration 2 -> 3), `ReminderDao`, domain `Reminder` and `ReminderKind`, `Task.reminders`, and pure functions in `domain/reminders` that compute trigger instants and sort pending, due and catch-up reminders.
2. **Settings and reminder repository.** DataStore-backed settings (default reminder time, snooze minutes, daily summary on/off and time); `saveTaskDetails` saves reminders; `getActiveTasks`, `snoozeTask`, `markReminderFired`, `deleteReminder`.
3. **Scheduling engine.** Notification channels, notifier, `ReminderScheduler` (single next alarm, serialized, catch-up), alarm, boot, time and package receivers, task-change syncer, WorkManager safety net, manifest permissions.
4. **Notification actions.** Complete, quick snooze and Snooze-for-longer (transparent activity with presets and custom); tapping a notification opens that task in the editor.
5. **Editor and rows.** Reminders section in the editor (presets, custom offset, specific date and time, remove), notification permission request when adding the first reminder, a warning banner when reminders may not fire, a bell on rows with reminders, and localized (plural-aware) descriptions.
6. **Settings and reliability helper.** Settings screen (reminders, language) and the reliability helper with live status, fix buttons, vendor hints and "send test reminder".
7. **Daily summary.** Optional morning notification with the number of tasks due today and overdue.
8. **Tests and docs.** Unit tests for the trigger logic; instrumented tests for the reminder repository; Arabic strings with plurals; FEATURES, ARCHITECTURE and MILESTONES updates.

### Deferred from M3
Per-task custom notification sounds, repeating reminders ("every 10 minutes until done"), reminders for subtasks, location reminders, and a calendar view.

### Known gaps after M3
- **Needs real-device testing, which only a person with the phone can do:** a reminder firing with the screen off, after a reboot, and after changing the time zone; the Complete, Snooze and More buttons; the notification tap opening the right task; the daily summary at its time; and how OPPO/OnePlus background rules treat the alarms. Use the reliability helper's test reminder first.
- The Play policy for exact alarms is still to be confirmed before publishing (see decisions above).
- There is no per-task choice of notification sound, and reminders do not repeat until the task is done (deferred).
- Reminders for a task that is marked complete and then reopened after their time can show as a catch-up within 24 hours.
- Unit and instrumented tests for M3 are written but not run (humbug).

## Milestone 4: Recurrence

**Definition of done:** a task can repeat daily, weekly (chosen days), monthly (a date, the last day, or "the 2nd Tuesday"), yearly or at a custom interval, on a fixed schedule or counted from the day it is finished, with an optional end date or number of times. Completing a repeating task (from the list, search or a notification) moves it to its next date instead of finishing it, resets its checklist and re-arms its reminders. Every completion is recorded in a history. Works in English and Arabic.

### Decisions made for M4
- **Storage.** `tasks.rrule` holds an RFC 5545 style rule limited to what the editor can express (for example `FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;UNTIL=20261231`); `tasks.repeatMode` is 0 for a fixed schedule and 1 for "repeat after completion". Schema v4, auto-migration 3 -> 4 (two new columns and the `completion_history` table).
- **In-house rule engine, no library.** The supported subset is small and fully tested in pure Kotlin (`domain/recurrence`); `lib-recur` would bring more surface than we use.
- **A repeating task is one row that moves forward** (as planned in ARCHITECTURE.md). Completing it appends a row to `completion_history` and advances `dueDate`; it never creates copies.
- **Next date, fixed schedule.** The first occurrence strictly after the later of the current due date and the day of completion. Finishing early does not skip ahead further than the next occurrence after the due date; finishing very late jumps to the next future occurrence instead of replaying every missed one.
- **Next date, repeat after completion.** The day of completion plus the interval (days, weeks, months or years). Weekday and month patterns are ignored in this mode.
- **Short months.** A monthly rule on day 29, 30 or 31 uses the last day of shorter months; a yearly rule on 29 February uses 28 February in other years. Day 31 therefore doubles as "last day of the month". The editor stores the day explicitly, so the pattern is not lost after a clamped month.
- **End conditions.** `UNTIL` is inclusive. `COUNT` is stored as the number of occurrences still to come, including the current one, and goes down on each completion, so no start date has to be remembered. When the last occurrence is finished (or the next one would be after `UNTIL`), the task completes for real.
- **What moves with the due date.** "Before due" reminders follow automatically; "at a specific time" reminders are shifted by the same number of days; a pending snooze is dropped; all reminders become eligible to fire again. The checklist is reset to unchecked.
- **A repeating task needs a due date.** Choosing a repeat rule on a task without one sets it to today; clearing the due date clears the rule.
- **Completion history.** One row per completion (task, when, which due date it was for). Un-completing a finished task removes its row. No screen uses it yet; it is the base for streaks later.
- **No undo yet.** Finishing a repeating task by mistake means editing its date back; the completion history row for that finish stays.
- **Shared code path.** The list checkbox, search and the notification Complete button all call `TaskRepository.setCompleted`, so all three get recurrence without changes.

### Steps
1. **Rule model and engine.** `Recurrence`, `Frequency`, `RepeatMode`, `NthWeekday`; RRULE string parse and format; `nextOccurrence` and `afterAdvance`; JVM tests.
2. **Schema v4 and mapping.** `tasks.rrule` and `repeatMode`, `completion_history` entity and DAO, `Task.recurrence`, mappers, auto-migration 3 -> 4.
3. **Completing a repeating task.** Repository: history, next date, checklist reset, reminder shift, un-complete; subtask reopening query.
4. **Strings, summary and editor state.** English and Arabic strings with plurals; a readable summary ("Every 2 weeks · Mon, Wed · Until 31 Dec"); the editor ViewModel carries the rule.
5. **Editor and rows.** Repeat section and `RecurrenceDialog` (frequency, interval, weekdays, monthly pattern, repeat after completion, end condition); a repeat icon on rows.
6. **Tests and docs.** Repository, editor ViewModel and dialog tests (written, not run), FEATURES, ARCHITECTURE and MILESTONES updates.

### Deferred from M4
Showing future occurrences in Upcoming and a calendar, an undo for completing a repeating task, "skip this occurrence", editing a single occurrence, rules with several month days or "every weekday" presets, a screen for the completion history and streaks, and leaving a reminder notification in the tray after the task was completed from inside the app.

### Known gaps after M4
- **Nothing in M4 has been compiled or run.** The first build may show compile errors, and the new schema (v4) has not been tried on the phone; the 3 -> 4 upgrade should be checked by installing over the M3 build and confirming existing tasks survive.
- **Not yet verified on a device:** the repeat dialog (RTL layout, keyboard, scrolling on small screens), the Arabic wording of the summaries, and a repeating task finished from the reminder notification.
- A notification that is still in the tray when a repeating task is finished inside the app stays there until it is swiped away (finishing from the notification itself clears it).
- Upcoming and Today show a repeating task once, at its next date.
- No automated migration test yet (same gap as M2 and M3).
- The unit tests (`RecurrenceTest`) and instrumented tests added for M4 are written but not run (flash).

## Milestone 5: Quick add

**Definition of done:** typing (or dictating) "Call mom tomorrow 5pm !high #family @Home every monday" into quick add fills the title, due date, time, priority, tags, list and repeat rule, shows what it understood as chips that can be tapped away, and creates the task. The same quick add opens from the share sheet, a Quick Settings tile and a launcher shortcut. English and standard Arabic.

### Decisions made for M5
- **Rule-based parser in pure Kotlin** (`domain/quickadd`), no network and no model. It takes the text, the current local date and time and the known list names, and returns a `ParsedQuickAdd`. Table-driven JVM tests with many phrases per language are the safety net.
- **Symbols are the same in both languages:** `#tag` (any number), `@list` (must match an existing list, spaces written as `_`; otherwise it stays in the title), `!high` `!medium` `!low`, `!1` `!2` `!3` and `!!!` for priority; Arabic words are accepted after the `!`.
- **Standard Arabic only** (the user's decision): no dialect words such as "بكرة". Arabic-Indic digits, diacritics and alef/ya/ta-marbuta spelling variants are normalized before matching, and the title keeps the user's original letters.
- **English and Arabic rules run together** on the same text, so mixed phrases work and no language switch is needed.
- **What is understood.** Dates: today, tomorrow, the day after tomorrow, weekdays (`friday`, `next friday`), `in 3 days/weeks/months`, `next week/month`, weekend, `Oct 20`, `20 October`, `the 15th`, ISO `2026-10-20`; Arabic equivalents. Times: `5pm`, `5:30 pm`, `17:30`, `at 5`, noon, midnight, morning/afternoon/evening/tonight, `in 2 hours`, `in 30 minutes`; Arabic equivalents (`الساعة 5 مساء`). Repeats: `daily`, `every day`, `every 2 weeks`, `every other week`, `every monday and friday`, `every weekday`, `monthly`, `yearly`; Arabic equivalents.
- **Rules that avoid surprises.** A bare weekday means the next such day after today; `next <weekday>` is that day in the following week. A time without a date means today if it is still ahead, otherwise tomorrow. `at 5` guesses PM for 1 to 6 and AM for 7 to 11. A repeat rule without a date starts at its first matching day. A date that has passed this year (`Jan 5` in October) means next year. If nothing is left for a title, the whole text is the title.
- **Tap a chip to undo one interpretation.** The parser takes a set of disabled fields; a disabled field leaves its words in the title.
- **Not parsed in M5:** reminders ("remind me"), end conditions ("until", "for 5 times"), list names with spaces other than `_`, and several tasks from one text. A share of several lines makes **one** task (the user's decision).
- **Voice input** uses the system speech recognizer through `RecognizerIntent` (no microphone permission, no custom UI). The dictated text goes into the same field and is parsed like typed text. The system recognizer may send audio to Google, which the store listing has to say.
- **One quick-add screen for outside entry points.** A see-through `QuickAddActivity` (like the snooze dialog) hosts the same sheet for the share target (`text/plain`), the Quick Settings tile and the launcher shortcut, adds the task to the Inbox (or the `@list`), shows a short confirmation and closes.
- **Shared creation code.** `QuickAddTask` turns a `ParsedQuickAdd` into a saved task (tags created or reused, list resolved, repeat rule attached) and is used by both the task list screen and the activity.

### Steps
1. **Parser core.** Text normalization with an index map back to the original, the `ParsedQuickAdd` result, the field set, and the symbol rules (tags, list, priority) with tests.
2. **English rules.** Repeat, time and date extraction.
3. **Arabic rules.** The same set in standard Arabic.
4. **Parser tests.** Phrase tables for both languages, edge cases and the disabled-field behavior.
5. **Quick add in the app.** `QuickAddTask`, the sheet with live chips and tap-to-undo, the task list ViewModel hook, strings in both languages.
6. **Voice input.** A microphone button in the sheet.
7. **Outside entry points.** `QuickAddActivity`, share target, Quick Settings tile, launcher shortcut, manifest, strings.
8. **Tests and docs.** `QuickAddTask` and ViewModel tests; FEATURES, ARCHITECTURE and MILESTONES updates.

### Deferred from M5
Reminders and end conditions in the parser, dialect Arabic, list names with spaces, creating a list from `@name`, localized relative phrases for other languages, an on-device model, offline-only speech recognition (a custom `SpeechRecognizer` with its own UI and the microphone permission), and a home-screen widget (M6).

### Known gaps after M5
- **Nothing in M5 has been compiled or run**, including the parser tests. The regular expressions are the likeliest place for surprises; the phrase tables in `QuickAddParserTest` are the first thing to run.
- **Needs a real device:** dictation in English and Arabic (and what the recognizer returns for numbers), the share sheet entry, the Quick Settings tile (it has to be added to the panel by hand) and the launcher shortcut, the see-through screen with the keyboard, and RTL layout of the chips.
- Arabic is standard Arabic only: no dialect words ("بكرة"), no Levantine month names, and the week-start for "next week" is Monday in both languages.
- `at 5` with no am or pm is a guess (1 to 6 afternoon, 7 to 11 morning); `5:30` with a colon but no am or pm is read as 24-hour.
- Not parsed: reminders, "until" and "for N times", other Latin or Arabic-script languages, list names with spaces other than `_`.
- The share sheet shows the activity label "Add to Feather"; the tile has to be added by the user from the Quick Settings editor.
- The share target accepts only plain text, not images or files.

## Milestone 6: Widgets and backup

**Definition of done:** the whole database can be exported to one JSON file and imported again (merging with what is on the phone, or replacing it), tasks can be exported as CSV, a backup can be written automatically every day into a folder the user picked, and two home-screen widgets exist: a Today list whose tasks can be ticked off and opened, and a one-tap "add task" button. English and Arabic. No network permission is added.

### Decisions made for M6
The user did not answer the open questions, so these are the recommended defaults; any of them can be changed later.
- **Import merges by id; "replace everything" is a separate, confirmed choice.** For each record the newer `updatedAt` wins (ties keep what is on the phone), so importing an old backup never undoes newer edits and importing on a second device never loses data. Tags with the same name as an existing tag are joined to it instead of duplicated. A task whose list does not exist falls back to the Inbox.
- **The file is a versioned JSON document** (`"format": "feather-backup"`, `"version": 1`) that mirrors the tables (folders, lists, tags, tasks with subtasks, task-tag links, reminders, completion history), soft-deleted rows included so deletions travel too. Unknown fields are ignored, a newer `version` than the app knows is refused with a clear message, and the file format is independent of Room's classes.
- **Merging is a pure function** (`BackupMerger`) from "what is on the phone" and "what is in the file" to "what to write", so it is unit tested without a database. Child rows (tag links, reminders) are only taken from the file for tasks whose own record was taken from it; completion history is a union.
- **CSV is for reading elsewhere, not for restoring.** One row per open or finished top-level task: title, notes, list, due date and time, priority, status, completion time, tags, repeat rule, subtasks. English column names, UTF-8 with a byte order mark so spreadsheets show Arabic correctly.
- **Automatic backup is daily and keeps the last 7 files**, written through the Storage Access Framework into the chosen folder (a Drive-synced folder works). One file per day (`feather-backup-2026-10-14.json`), a second run on the same day replaces it. WorkManager runs it; "Back up now" runs the same code. If the folder permission is lost the backup fails quietly and the settings row says so.
- **Widgets use Jetpack Glance** (as planned in ARCHITECTURE.md). A Today list widget (open tasks due today or earlier, tick to complete, tap to open, a header button to add) and a quick-add button widget. The single-task widget is left for M7. Widgets refresh when tasks change and every half hour (so they roll over at midnight).
- **Android's own cloud backup is left as it is** (the database is included in Auto Backup and device-to-device transfer). The rules files now say so explicitly. This is an operating-system feature, not something the app does, but the store listing's privacy text should mention it.
- **No encryption of backup files.** They are plain JSON in a place the user chose.

### Steps
1. **Format and pure logic.** Backup records, JSON codec with version checks, `BackupMerger`, CSV builder, with JVM tests.
2. **Backup repository.** `BackupDao`, entity-to-record mapping, `BackupRepositoryImpl` (export, merge import, replace import, CSV) and DI.
3. **Settings: manual backup.** Backup section in Settings (export, import with merge or replace, CSV), file reading and writing, result messages in English and Arabic.
4. **Automatic backup.** Settings for on/off, folder and last backup; `AutoBackup`, the daily worker and scheduling; UI rows.
5. **Today widget.** Glance dependency, widget, tick-off and open actions, refresh on changes.
6. **Quick-add widget and wrap-up.** The button widget, manifest entries, backup rules documented.
7. **Tests and docs.** Repository tests (instrumented, written not run), FEATURES, ARCHITECTURE and MILESTONES updates.

### Deferred from M6
The single-task widget, widget configuration screens (choosing a list), encrypted backups, restoring from a backup at first launch, backup of settings (language, reminder defaults), a CSV or calendar import, and sync.

### Known gaps after M6
- **Nothing in M6 has been compiled or run.** The Glance code is the likeliest place for compile errors (it is a separate UI toolkit with its own imports), followed by the file-picker wiring in Settings. Glance 1.1.1 was chosen from memory of the release list and has not been resolved by Gradle yet.
- **Needs a real device:** exporting and importing through the system file picker (including a Drive folder), choosing a backup folder and letting the daily job run (WorkManager is deferred by the battery manager on some phones, so "Back up now" is the reliable check), adding both widgets to the home screen, ticking a task in the widget, and the widgets in dark mode, in Arabic and at different sizes.
- Importing does not ask before merging; replacing asks once. A very large file is read into memory in one piece.
- A backup does not carry settings (language, reminder defaults, the backup folder), only tasks and what belongs to them.
- Reminders are rescheduled after an import by the normal task-change path, not by the import itself.
- The widgets use the system accent colors on Android 12 and up and fixed colors below.
- The CSV is for reading elsewhere; there is no CSV import.
- The tests for the merge rules, the codec, the CSV builder and the backup file names are JVM tests (`BackupMergerTest`, `BackupCodecTest`, `TaskCsvTest`, `AutoBackupNamesTest`); `BackupRepositoryTest` is instrumented. All are written but not run (flash).

## Milestone 7: Polish

**Definition of done:** everything the v1 feature list still owes is in (appearance and format settings, manual order with drag, selecting several tasks, duplicating, links in notes, saved filters, a "next task" widget), the main screens work with TalkBack and large text, and the repository holds what a store submission needs (release rules, listing texts in both languages, privacy text, F-Droid metadata, a release guide).

### Decisions made for M7
The user did not answer the open questions, so these are the recommended defaults.
- **Appearance settings now, not in M8.** Theme (follow system, light, dark), a "pure black" option for dark mode (AMOLED), and Material You colors on or off. The bird brand in M8 changes the palette, not these switches.
- **Format settings.** Time format (system, 12-hour, 24-hour), first day of the week (system, Monday, Saturday, Sunday) and haptic feedback on or off. The first day of the week orders the weekday chips of the repeat dialog; the system date picker keeps following the system.
- **Donations are an external link** in Settings ("Support Feather"; donations never unlock anything). The link is a constant that is empty until the owner fills it in, and the row stays hidden while it is empty, so no placeholder address ships. No billing library, nothing that blocks F-Droid. The Play policy for such links must be checked before publishing.
- **Manual order is a sort option** ("Manual") with a drag handle on each row; it permutes the `sortOrder` values the visible tasks already have, so the tasks that are not visible keep their relative place. "Created" now sorts by creation time only (it used to share `sortOrder`), which is the same order until someone drags.
- **Selecting several tasks** starts with a long press on a row; the top bar then offers complete, move to a list, add a tag, delete (with undo) and select all.
- **Duplicate** lives in the editor's menu: it copies title, notes, date, priority, list, tags, a repeat rule, reminders and a fresh unchecked checklist, and opens the copy.
- **Links in notes** appear as chips under the notes field (the field itself stays plain text) and open in the browser.
- **Saved filters** are simple on purpose: a name plus optional list, tag, minimum priority and a due window (any, overdue, today, next 7 days, no date), with "hide finished". They are a new Room table (schema v5, auto-migration), show in the drawer, and are part of the backup file (an optional field, so version 1 files still read).
- **The "Next task" widget** shows the single most urgent open task with a checkbox; no configuration screen.
- **Accessibility is an audit of what exists:** one TalkBack announcement per row (title, due, priority, state), the checkbox named after its task, actions for complete, delete and pin as TalkBack custom actions (swipe is not reachable otherwise), section titles marked as headings, and 48 dp touch targets.
- **Performance is the cheap, safe part:** stable keys, no work in composition that can be done once, and nothing on the main thread at start. A baseline profile and macrobenchmarks need a separate module and a device, so they are left out.
- **Release hardening without a release build:** keep rules written from the libraries' documentation, a version name and code in one place, and a release guide. They are untested until someone builds the release variant.

### Steps
1. **Appearance and format settings.** Theme, AMOLED, Material You, time format, week start, haptics: stored, applied, shown in Settings.
2. **Accessibility.** The audit above for the list, rows, editor and dialogs.
3. **Select several tasks.** Selection mode and its actions.
4. **Manual order.** The Manual sort and the drag handle.
5. **Duplicate and links in notes.**
6. **Saved filters.** Schema v5, domain, repository, drawer and editor dialog, backup.
7. **Next task widget.**
8. **Performance and release rules.**
9. **Store preparation.** Listing texts, privacy text, F-Droid metadata, release guide, the donation row.
10. **Tests and docs.**

### Deferred from M7
Macrobenchmarks and a baseline profile, screenshot tests in LTR and RTL, snapping a dragged task between two sections, a filter with several lists or tags, widget configuration screens, a calendar view, and the bird brand (M8).

### Known gaps after M7
- **Nothing in M3 to M7 has been compiled or run.** Five milestones of code are stacked on the last build (M2). The first build will probably show compile errors, mostly in the newest and most Compose-heavy files (Glance widgets, the drag state, the list screen, the settings screen), and the database now has to upgrade 2 -> 5 on the phone. A stabilization pass (build, fix, run the unit tests, install over the old build, walk through `store/RELEASE.md`) is the right next step before any new feature.
- **Needs a real device:** dragging (feel, auto-scroll at the edges, rows of different heights), long-press selection, TalkBack on a task row and in the drawer, large font sizes, pure black on an OLED screen, switching theme while the app is open (the activity is recreated), the 12 and 24 hour clock in the time picker, Arabic in the filter dialog and the chips.
- Dragging works only in a single list of tasks (not in Upcoming, which is grouped by day) and only in the Manual sort. There is no auto-scroll while the finger is still.
- The week start orders the weekday chips of the repeat dialog; the system date picker and the parser's "next week" keep their own rules (the picker follows the system, the parser uses Monday).
- Saved filters have one list and one tag at most, and no sort of their own.
- `SupportLinks.DONATION_URL` is empty, so "Support Feather" is not shown until the owner sets it; the Play policy on donation links and the exact-alarm declaration still need to be checked (`store/STORE.md`).
- The launcher icon is the template's; the bird identity is M8. The release build, R8 rules and signing have never been run.
- Macrobenchmarks, a baseline profile and screenshot tests (LTR and RTL) were left out; the view code still builds all lists in memory on the main thread, which is fine for thousands of tasks but is the first thing to measure.
- New tests (written, not run): `SavedFilterTest`, `TaskSortOrderTest`, `LinksTest`, `DisplayFormatTest`, additions to `TaskListViewModelTest`, `BackupMergerTest`, `BackupCodecTest`, `RepositoryTest`, `BackupRepositoryTest` and the new `SavedFilterRepositoryTest`.

## Milestone 8: Brand

**Definition of done:** Feather has a face. A small bird (drawn in code, as a first identity that a designer can later replace) is the app icon, the launch screen, the picture in empty states, the little celebration when a task is finished, and a mark on the widgets; the colors are the app's own; the bird moves, but never when the person has asked for less motion.

### Decisions made for M8
The user did not answer the open questions, so these are the recommended defaults.
- **The bird.** A flat, friendly songbird in profile with a feather-shaped crest, built from a handful of smooth shapes (body, wing, tail, belly, beak, eye, crest). One set of path data (`ui/brand/BirdShapes.kt`) is the source; the vector drawables for the icon, the launch screen, the widgets and the store graphics repeat the same paths, and a unit test checks that they have not drifted apart. It is a placeholder identity in the sense that a designer's artwork can replace the paths without touching any screen.
- **Palette.** Plumage teal, deep blue (wing and tail), cream (belly), amber (beak and crest), coral (cheek), ink (eye). They become the app's light and dark color schemes. **Material You is now off by default** (it stays one switch away in Settings): a brand needs its own colors. People who already chose a setting keep it.
- **Animation is Compose-native.** The bird is drawn on a `Canvas` from a pure function (`birdPose(mood, phase)`) that returns wing angle, body offset, eye opening, scale and tilt, so it needs no animation library, adds nothing to the APK, and can be tested without a device. Four moods: idle (a slow bob and an occasional blink), sleeping (eyes closed, breathing, floating z's), happy (one hop with flapping wings) and flying.
- **Less motion is honored twice:** when the system animation scale is zero the bird is drawn still, and a new switch in Settings ("Bird animations") turns the movement off inside the app. The static picture stays.
- **Where the bird appears.** The adaptive launcher icon (with a single-color version for themed icons), the launch screen (Android 12 splash with a wing-flap animated icon; a still bird before Android 12), the empty states (idle for "no tasks yet", sleeping for "all clear" and the trash), a short hop over the list when a task is finished, the empty state of the two list widgets, the About row, and a bird silhouette as the small notification icon. The bell stays as the "has a reminder" marker on a task row.
- **Voice.** The copy stays plain and warm. Only the "nothing here" screen gets a line about the bird napping. There are no sounds, no streaks and no characters that talk; donations never unlock anything, so there is nothing to "evolve".
- **Store graphics** are delivered as SVG (icon and feature graphic) in `store/graphics/`, built from the same paths; exporting them to the PNG sizes the stores ask for needs a vector tool.

### Steps
1. **Palette and bird shapes.** Brand colors in the light and dark schemes, Material You off by default, the shared path data and the full-color bird drawable.
2. **The animated bird.** `birdPose`, the `Canvas` drawing, the reduced-motion check, the in-app switch.
3. **Icon, launch screen and notification icon.**
4. **The bird in the app.** Empty states, the finish-a-task hop, the widgets, the About row, the napping copy in both languages.
5. **Store graphics.** SVG icon and feature graphic.
6. **Tests and docs.**

### Deferred from M8
A designer's artwork, more moods (waiting, sad), the bird reacting to streaks or counts, sounds, a Lottie or Rive pipeline, an animated widget, dark-mode variants of the artwork, and localized store graphics.
