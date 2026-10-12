# Feather: Milestones

Companion to [FEATURES.md](FEATURES.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

## Working agreement
- The **"humbug"** workflow: for each step write the code, commit, push and go straight to the next step, with no compile, tests or install in between. When the milestone is finished, build and install the app on the phone. Unit tests are not run unless asked.
- Commit to git only as part of that workflow or when asked.
- Test device: OnePlus/Oppo CPH2625, Android 16 (confirm vendor-specific reminder behavior in M3).

## Roadmap

| # | Milestone | Main content | Status |
|---|---|---|---|
| M1 | Foundation | Tasks, Room, navigation, English and Arabic (RTL) | Done |
| M2 | Organization and views | Lists, one-level folders, tags, subtasks, search, Today / Upcoming / Overdue, trash | Done (built and installed; the 1 -> 2 database upgrade kept existing data) |
| M3 | Reminders | Scheduler, receivers, notification actions, reliability helper, real-device testing | |
| M4 | Recurrence | Rule engine, repeat modes, completion history | |
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
