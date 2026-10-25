# Feather: things to do later

Small and medium items that are not part of a milestone yet. The big plan is in `MILESTONES.md`.

## Rearranging tasks: the UX feels odd
Dragging rows into your own order works in the code (the list can be reordered in Today, Overdue and Goals, and dropping a row switches the sort to Manual so the order stays), but the first version did not feel right, so the drag grip was taken off the task cards for now. What is still there: TalkBack's "Move up" and "Move down" actions, and the Manual sort in the sort menu.

What felt odd, and ideas to try:
- **The grip (two lines) sat next to the "+" on every card.** It was always visible, crowded the card and looked like a button. Idea: show it only while a "Rearrange" mode is on (a menu item next to the sort choices), or only in the Manual sort again.
- **Dragging changes the sort behind your back.** Dropping a row silently switches the sort to Manual. Idea: say so (a small message with Undo), or ask the first time.
- **Long press already means "select".** A press-and-drag on the card would be the natural way to move it, but it clashes with selecting. Idea: select with a tap on the ring area plus a "Select" menu action, and keep long press for dragging.
- **Pinned tasks, tag tabs and the Today header.** Moving a row inside a tag tab only orders that tab's rows; pinned tasks always stay on top. Decide what a manual order means across these.
- **Upcoming and Completed cannot be rearranged** (they are grouped under date headings). That is probably right, but it should be explained.

## Tags
- Look at the tag tabs on the phone: on the first launch the tab row looked scrolled to the far right with "#Work" highlighted, although "All" should have been the selected, left-most tab. Check whether that is a real bug.
- Tags with no tasks in the current view are hidden from the tabs. Check that this feels right when switching between views (the tabs change from view to view).
- Show a task count on each tab?
- Tag colours (like list colours used to be)?

## Tips (Google Play Billing)
- Tapping "Leave a tip" in the menu closed the menu but the sheet did not show on the Huawei. Not understood yet. The same sheet worked from Settings.
- Create the three products in Play Console and test with a licence tester once the app is on a testing track (see `store/STORE.md`).

## Release
- Baseline profile and macrobenchmarks (one `:baselineprofile` module).
- Release keystore, store graphics and screenshots, privacy contact address, Play policy forms.
- Arabic copy is a draft for a native speaker to read through (everything added since M9).

## Counter tasks (after the open items above are fixed)
A task can count toward a target, for example "do 100 pushups" done in sets of 25. Decided:
- **A fixed step per task.** The card shows a progress bar ("40 / 100") and one "+25" button, one tap per set. No typing a number each time.
- **Only the number is kept**, not a log of each set with its time (a log can be added later without changing the number).
- **A repeating task resets** its count to 0 every time it comes round (like a checklist un-ticks today).
- **No separate unit label**: the task title says what is counted.
- **Reaching the target finishes the task** (ring tick and feathers). Ticking the ring early means "I am done": the count jumps to the target.
- **Counter or checklist, not both** on one task.
- **Mistakes:** long-press the button (or edit the task) to subtract or set an exact number.
- **Data:** new nullable columns on tasks (target, done count, step), a database migration, the same fields in backup files, a counter tool in the quick-add sheet, strings in English and Arabic, tests.
- **Left out for now:** quick-add syntax (such as "pushups x100 by 25"), a log of sets, feeding the counts into Your rhythm.

## The owl (branch codex/cartoon-bird-redesign)
- Owl art for the surfaces still on the old blue bird (widgets, About, launch screen, notification icon, store graphics): list and brief in `art/README.md`.
- Check the owl on the phone: Today (resting, sleeping at night when nothing is left, the lap when the last task is done), the empty-state scenes (the owl fills more of its box than the songbird did, so placements in `Scenes.kt` may need nudging), the completion hop, the tip sheet thank-you.
- Mid-flap wing frames and a half-open-eye frame would smooth the wing beat and the falling asleep.
