# Feather: v1 Feature List

Feather is an Android-only, feature-rich todo app for everyone. It is fully free with donations only (donations never unlock features), with no ads, no accounts and no tracking. Birds are a core part of the brand (animation and design to be discussed later).

## Product constraints
- Android only, minSdk 26. Play Store first; keep the core free of Google-only dependencies so F-Droid stays possible.
- Local-only data in v1: no backend, no accounts, no sync.
- English and Arabic from day one, with full RTL support.
- No Wear OS and no on-device AI.
- Data model must be sync-friendly for later: UUID keys, `updatedAt`, soft deletes, recurrence stored as an RRULE-style rule.

## Status (see MILESTONES.md)
- **Done in M1:** tasks (title, notes, due date and time, priority), swipe to delete with undo, quick add, English and Arabic with RTL, Material You theme.
- **Done in M2:** lists with colors and one level of folders, tags, subtasks (checklist), pin, move between lists, smart views (Today, Upcoming, Overdue, All, Completed, No date), sort (creation, due date, priority), search, trash with restore and 30-day purge.
- **Done in M3:** multiple reminders per task (at due time, before due, at a specific time), notifications with Complete and Snooze, snooze presets and custom, tap to open the task, reminders re-armed after reboot, app update, clock and time zone changes, a reliability helper with a test reminder, settings (default reminder time, snooze length, daily summary), and the optional daily summary.
- **Deferred to M7:** drag to reorder, bulk select, duplicate task, saved custom filters, tappable links in notes.
- **Not started:** recurring tasks (M4), quick-add parser and voice (M5), widgets and backup (M6), bird brand and animation (M8).

## 1. Tasks
- Title, notes (plain text and tappable links), due date and time, priority (4 levels), completion
- Subtasks and checklists, drag to reorder
- Swipe to complete or delete, with undo
- Duplicate task, move between lists, bulk select (complete, move, tag, delete)
- Trash with restore (auto-purge after 30 days)

## 2. Organization
- Lists, with an optional single level of folders
- Tags (many per task) and colored lists
- Pin or star important tasks
- Filter and sort by date, priority, tag, or manual order
- Full-text search

## 3. Smart views
- Today (due today plus overdue)
- Upcoming (next 7 days, grouped by day)
- Overdue
- All, Completed, No date
- Saved custom filters (e.g. "high priority + #work")

## 4. Recurring tasks
- Daily, weekly (pick days), monthly (by date or "2nd Tuesday"), yearly, custom intervals
- Fixed schedule or "repeat after completion"
- End date or occurrence count

## 5. Reminders
- Multiple reminders per task: at time, X minutes before, or a specific time
- Snooze with presets and a custom option
- Complete, snooze and open actions directly from the notification
- Reliability helper that guides users through battery-optimization and exact-alarm permission
- Reminders reschedule after reboot or timezone change
- Optional daily summary notification

## 6. Quick add
- On-device natural-language parsing (English and Arabic): "Call mom tomorrow 5pm !high #family" fills every field
- Voice input via Android's speech recognition, feeding the same parser
- Share-sheet target ("share text to Feather")
- Quick Settings tile and launcher shortcut
- Widgets: Today list, quick-add button, single-task widget

## 7. Backup and data
- Manual export and import (JSON), CSV export
- Automatic local backups to a user-chosen folder (SAF), including a Drive-synced folder if the user picks one
- Core app needs no network permission

## 8. Settings and appearance
- Material You dynamic color; light, dark and AMOLED black themes
- Week start day, time format, default reminder time
- Haptics on/off
- Language: English, Arabic, or system default

## 9. Internationalization (RTL)
- Full RTL layouts using start/end, never left/right
- All strings in resources; Arabic plurals handled properly
- Locale-aware dates and numbers, optional Arabic-Indic digits
- Mirrored gestures (swipe directions flip in RTL) and direction-aware icons
- Natural-language parser tested in both languages

## 10. Accessibility and polish
- TalkBack labels, large font scaling, minimum touch-target size
- Edge-to-edge UI and predictive back
- Fast cold start, small APK
- Respect the system "reduce motion" setting

## 11. Brand hooks (details later)
- Slots reserved for bird identity: app icon, empty states, completion moment, loading
- An animation layer in the architecture so animation can be added without refactoring

## Out of v1 (v2 candidates)
Sync (Drive/WebDAV), attachments (images and voice notes), calendar integration, location reminders, focus timer, habits and streaks, templates.

## Suggested build order
1. **v0.9 core:** tasks, lists/tags, smart views (sections 1-3), reminders (5), settings/theming (8), RTL foundation (9)
2. **Increments:** recurrence (4), quick add with parser and voice (6), widgets, backup/export (7), accessibility pass (10)
3. **Brand and animation** layered in once the core is stable (11)

## Risks to watch
- **Reminder reliability** across vendors (Xiaomi, Samsung, Oppo); test on real devices.
- **Arabic natural-language parsing** (dialect, Arabic-Indic digits, phrases like "بكرة الساعة ٥"); start rule-based with a strong test suite, and ship an English fallback if Arabic slips.
- **Recurrence logic** is the trickiest part of a todo app; design it early.
- **Feature-rich vs. light:** use progressive disclosure so the default screen stays clean.
