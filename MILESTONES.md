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
| M4 | Recurrence | Rule engine, repeat modes, completion history | In progress (developed with the "flash" workflow: no build or install) |
| M5 | Quick add | Natural-language parser (EN and AR), voice input, share target, Quick Settings tile | |
| M6 | Widgets and backup | Glance widgets, JSON export and import, automatic backup | |
| M7 | Polish | Accessibility pass, performance, drag to reorder, bulk select, store preparation | |
| M8 | Brand | Bird identity and animation | |

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
- **Shared code path.** The list checkbox, search and the notification Complete button all call `TaskRepository.setCompleted`, so all three get recurrence without changes.

### Steps
1. **Rule model and engine.** `Recurrence`, `Frequency`, `RepeatMode`, `NthWeekday`; RRULE string parse and format; `nextOccurrence` and `afterAdvance`; JVM tests.
2. **Schema v4 and mapping.** `tasks.rrule` and `repeatMode`, `completion_history` entity and DAO, `Task.recurrence`, mappers, auto-migration 3 -> 4.
3. **Completing a repeating task.** Repository: history, next date, checklist reset, reminder shift, un-complete; subtask reopening query.
4. **Editor.** Repeat section and `RecurrenceDialog` (frequency, interval, weekdays, monthly pattern, repeat after completion, end condition); ViewModel state; English and Arabic strings with plurals.
5. **Labels and rows.** A readable summary ("Every 2 weeks · Mon, Wed · Until 31 Dec"), a repeat icon on rows.
6. **Tests and docs.** Repository and editor tests (written, not run), FEATURES, ARCHITECTURE and MILESTONES updates.

### Deferred from M4
Showing future occurrences in Upcoming and a calendar, an undo for completing a repeating task, "skip this occurrence", editing a single occurrence, rules with several month days or "every weekday" presets, a screen for the completion history and streaks, and leaving a reminder notification in the tray after the task was completed from inside the app.
