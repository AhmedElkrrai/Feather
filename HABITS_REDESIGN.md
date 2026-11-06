# Habits redesign: create-habit screen, habits in Today, named stages

## Context
Habits today are a weekly-goal tracker kept apart from tasks: a dialog creates them, a day is a plain counter
(`habit_entries.amount`), and nothing links a habit to a time of day. The goal is to make habits part of the day:
a full create screen, optional appearance in Today (as one distinct row, at a time or in a window), and named stages
(Pray: Fajr, Dhuhr, Asr, Maghrib, Isha, each with a window or fixed time), with reminders last.

Decisions already made with the user:
- A staged habit is ONE row in Today. Ordered mode names only the next stage and reveals the next once one is done;
  Show-all mode expands the row into a checklist (like subtasks).
- Habits do NOT count in the Today header (owl, leaves, all-done celebration): tasks only.
- A habit with a goal under 7 days shows every day until the week's goal is met. No weekday picker for now.
- Reminders are included, built last.
- No tab for habits in Today. A habit row has its own look.
- Standing rule: a visual mockup is agreed before any app code for it (M1 form layout, M4 Today row).

## Data design (DB v1 to v2, `@AutoMigration(1, 2)`, commit `app/schemas/.../2.json`)
Existing habits must keep working: every new column has a default, so old habits are "no time, no stages, not in Today".
- `habits` + `showInToday` (0/1, default 0), `timeMode` (0 none, 1 at time, 2 window), `startMinute?`, `endMinute?`
  (for a habit without stages), `stageMode` (0 none, 1 show all, 2 ordered), `todayFrom` (0 all day, 1 only from its start time),
  `remind` (0/1).
- New `habit_stages` (id, habitId FK cascade, name, timeMode, startMinute?, endMinute?, updatedAt): a stage keeps its
  id for life, so renaming or changing its time applies at once and never breaks history.
- New `habit_goal_stages` (habitId, effectiveFrom, stageId, position): which stages, in what order, belong to each
  goal version. Adding, removing or reordering stages is queued for next week exactly like a goal edit
  (`queueGoal` already replaces a pending edit), so past weeks are still judged by the stages they had.
  For a staged habit `dailyTarget` ("needed per day") defaults to the number of stages in that version; the user may lower it, and a day
  counts when `amount >= dailyTarget` (the existing rule, so no change to `HabitWeeks.kt`). Stages can be done in ANY order: the log is per stage.
- New `habit_stage_log` (habitId, date, stageId, updatedAt): which stage was done on which day.
- `habit_entries.amount` stays the single source for all week, tree and rhythm maths. For a staged habit it is
  written in the same transaction as the stage log (amount = stages done that day), so `HabitWeeks.kt`,
  `TreeShape.kt`, `HabitsRhythmTab` and the overview keep working unchanged.
- Fallback if versioned stages prove too heavy: stage list not versioned, edits apply at once, past days keep their amount.
- Backup (`data/backup/`): add `habitStages`, `habitGoalStages`, `habitStageLog` lists (default `emptyList()`) and the new
  optional `HabitRecord` fields (defaults); bump `BackupFile.VERSION` 1 to 2 so an older app refuses the file instead
  of silently dropping stages. Extend `BackupMapping` (both directions), `BackupMerger` (newest `updatedAt` wins; stage-log key
  `habitId|date|stageId`; children only if the habit is known), `MergePlan`, `BackupDao`, `BackupRepositoryImpl` (parents first).
- Repository (`domain/repository/HabitRepository.kt`, `data/repository/HabitRepositoryImpl.kt`): replace the positional
  `addHabit`/`queueGoal` with a `HabitDraft` (name, kind, unit, target, dailyTarget, showInToday, time fields, stageMode,
  remind, stages) and `saveHabit(draft, thisWeekStart)`; add `setStageDone(habitId, date, stageId, done)`.
  Callers to update: `HabitsViewModel.add/edit`, `HabitSampleData.kt`.

## Milestones (each step is a commit; push when a milestone is done)

### M1. Create/edit habit as a screen (no schema change)
1. Mockup of the form for approval.
2. Extract the private form pieces from `feature/taskedit/TaskEditScreen.kt` (`FieldSection`, switch row, chip row,
   `PickerRow`) into `ui/components/FormParts.kt`; reuse `TimeOfDayDialog`.
3. `HabitEditRoute(habitId: String? = null)` in `ui/navigation/Routes.kt`, wired in `FeatherNavHost.kt`, added to
   `isDrillDown()` in `NavTransitions.kt` (or it gets the menu fade). `HabitEditScreen` + `HabitEditViewModel`
   (state-flow pattern of `TaskEditViewModel`, `savedStateHandle.toRoute`). Fields: name, type (days in a week / amount),
   weekly target, times a day or unit, the "takes effect next week" note when editing.
4. FAB and the card's Edit open the screen; delete `HabitEditDialog` and `KindChoice` from `HabitsScreen.kt`. Strings en + ar.

### M2. Data layer
1. Entities, DAO queries, `FeatherDatabase` v2 + auto-migration + `2.json`.
2. Domain: `HabitStage`, stage list per goal week (`forWeek`-style resolver next to `HabitWeeks.kt`), `setStageDone` keeping `amount` in sync.
3. `HabitDraft` / `saveHabit`, migrating `HabitsViewModel` and the debug sample data.
4. Backup fields, merger, DAO, version bump. Unit tests: `HabitStagesTest`, `BackupMergerTest`, `BackupCodecTest`, defaults for old habits.

### M3. Stages in the form and on the Habits screen
1. Form: "Name each time" switch (sets "Times a day" to the stage count; "Needed per day" can then be lowered), stage rows (name, time mode: none / at time / window, one or two
   `TimeOfDayDialog` calls, move up/down, remove), stage display (Show all / One at a time). Max 12 stages.
2. Habits screen: tapping a staged day cell opens a small checklist sheet of that day's stages (`setStageDone`); plain habits unchanged.
3. Rhythm and detail screens need no change (they read `amount`).
4. The edit screen has an "In Today" section, so the person controls how the habit appears there (all optional, sensible defaults):
   - "Show in Today" switch (default on for a new habit; off for habits that existed before).
   - "When it shows": all day (default), or only from its start time (the first stage's time, or the habit's own time).
   - A habit without stages: its own time: none / at a time / in a window.
   - A habit with stages: how the row looks: one stage at a time (default) / all stages as a checklist (missed stages always sit
     under "Earlier" in one-at-a-time mode).
   - "Needed per day" (defaults to all stages = strict; optional to lower).
   - "Remind me" (M5).
   A small live preview of the Today row at the top of this section shows the effect of each choice.

### M4. Habits in Today
1. Mockup of the habit row and its placement for approval (distinct look: leaf-tinted surface, a leaf-shaped check instead of the
   barbed ring, stage dots like `● ● ○ ○ ○`, a time or window chip such as `12–2 PM`, a "Habit" chip; no swipe-to-delete).
   Approved interaction of the habit row (mockup agreed with the user):
   - A press and hold of 0.7 s anywhere on the card finishes the habit for today (every stage; a counted habit fills to its target). A tap never
     finishes it, so a stray touch cannot. While holding: a ring around the leaf fills, a green wash spreads from the leaf, the card sinks
     slightly, light haptic ticks; at the end a stronger haptic, the leaf blooms, the card dims and an Undo snackbar appears. Letting go early
     (or sliding the finger) eases everything back.
   - Teaching the gesture: the ring track is always visible; the first habit row shows a dim "Hold the card to finish today" line (fades after a
     few uses or the first successful hold, stored like the swipe hint); a quick tap plays a small partial fill and a "Hold to finish" label;
     the ring plays the fill once by itself the first time a habit row is seen; TalkBack gets a "Mark done for today" custom action.
   - Stage strip instead of an "Earlier" link: one chip per stage (done / missed, amber dashed, never red / now / later). A single tap on a chip
     ticks or unticks that one stage; ticking the last one finishes the habit. Dots are drawn shapes of one size in every state.
2. Setting "Show habits in Today" (default on) in the "Company" block (`AppearanceSection.kt`) through `AppSettings`,
   `DataStoreSettingsRepository`, `SettingsViewModel`; per-habit "Show in Today" switch in the form (default on for new habits).
3. Pure logic `ui/today/TodayHabits.kt`: which habits show today (showInToday, goal exists this week, week goal not yet met, today not done),
   the visible stage, window state (before / now / after; a missed window is never red), time ordering. Unit tests `TodayHabitsTest`.
   "One at a time" never gates anything; it only decides what the row shows first:
   - Stages with times: the row shows the CURRENT stage (the latest whose start has passed and that is not done, or the next upcoming one
     if none has started). Earlier stages that were not done sit under a quiet "Earlier (2)" expander and can still be ticked late.
   - Stages without times: the row shows the first not-done stage, with a "Skip" action that moves on without logging it.
   - Tapping the row's stage dots or expanding it lets the user tick ANY stage in any order (Fajr and Dhuhr missed, Asr/Maghrib/Isha done).
   - Skipped or missed stages simply do not count toward the day's "needed per day"; nothing turns red and nothing is lost.
4. `TodayHabitsViewModel` modelled on `DayCardsViewModel` (habit repository + settings + minute ticker + clock), so `TaskListViewModel`'s
   task-typed sections stay untouched. `HabitTodayRow` composable; inserted as `item(key = "habit-$id")` after the day card in the
   `LazyColumn` of `TaskListScreen.kt`, ordered by time. Fix the hand-counted index in the scroll-to-just-added logic (+1 per habit row).
   Tap marks the next stage / opens the checklist; long-press opens the habit. A finished habit lingers (`Dur.RowLinger`) then leaves.
5. Explicitly unchanged: `TodayStatus` / header counts, widget, daily summary (tasks only).

### M5. Reminders
1. Pure planner `domain/reminders/HabitTriggers.kt` next to `ReminderTriggers.kt`: next occurrence per stage (window start or fixed time),
   skip done stages and a met week, short catch-up window (about 2 h), fired marker per habit/stage/date (DataStore set, pruned daily).
2. Feed it into `AlarmReminderScheduler.reschedule()` (still one alarm, earliest of tasks and habits); add the habit repository to
   `ReminderSyncer`'s `combine` and to `ReminderEntryPoint`.
3. `HABITS` channel in `ReminderChannels`, `ReminderNotifier.showHabit` with a `habit:` tag prefix. Trap: `cancelStale` currently cancels any
   notification whose tag is not an open task id, so it must skip `habit:`. "Done" action logs the next stage (`ReminderActionReceiver`);
   tap opens Habits (`MainActivity` extra next to `EXTRA_OPEN_TASK_ID`). `ReminderHealth` also checks the `HABITS` channel.
4. Form: "Remind me" switch (asks `POST_NOTIFICATIONS`, shows the existing reliability warning like `TaskEditScreen`). Tests: `HabitTriggersTest`.

### M6. Polish
Debug sample data for staged habits (Pray with 5 prayers, ordered and show-all), Arabic/RTL pass over every new string and layout,
optional wording cleanup ("goal" on habits vs "Goals" for no-date tasks), update `AppSettingsDefaultsTest` and docs.

## Risks
- Room is v1 with no migration code in the project and an exported schema only for v1: add a migration test or at least verify on a device with v1 data.
- Versioned stage lists are the most intricate part; the fallback above is the escape hatch.
- Today's `LazyColumn` has hand-counted indices and `Task`-typed sections: habits go in as separate items to avoid touching them.
- `cancelStale` would silently delete habit notifications; covered in M5.
- Every new string needs `values/` and `values-ar/`; layouts must be RTL-safe.

## Verification
Per milestone: `assembleDebug` + `adb install -r` (no tests unless asked; new unit tests are written but run only on request).
End to end on the phone: create "Pray" (days in a week, 7, 5 stages with windows) in the new form; it appears in Today as one habit row;
mark stages in ordered mode and watch the next appear; the day cell, week bar and tree update; kill and reopen the app; export a backup
and restore it (stages and log survive); upgrade install over a v1 build with existing habits (nothing lost); set a stage time two
minutes ahead and check the notification and its Done action (`adb shell dumpsys alarm | grep feather`).
