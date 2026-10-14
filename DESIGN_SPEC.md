# Feather: "The Cozy Nest" implementation spec (for Claude Code)

Visual reference: the Design canvas "Feather — Cozy Nest" (17 boards). This file is the implementation contract; `FeatherTheme.kt` holds the tokens. Stack: Jetpack Compose + Material 3, Android only, local-only data. No Lottie, no video, no raster art.

## 0. Ground rules
- Everything must work in RTL (use start/end, never left/right) and in English + Arabic.
- Min touch target 48dp; support 200% font scale (no fixed heights; `heightIn(min=)`; cap display sizes at 1.3x).
- Decoration (bird, sun/moon, leaves, grain, rhythm dots) has NO semantics (`clearAndSetSemantics {}`); meaning goes in text. Priority is announced ("High priority"); ring and barbs are visual only.
- Three themes: Light, Dark, Black (OLED). Black = DarkScheme with surfaces changed (see tokens).
- Tone: never guilt. "Overdue" is shown as "Waiting since Mon" in neutral colours. No red alarm states, no streak that can reset.
- Reduce motion: `rememberReduceMotion()` = system animator scale 0 OR in-app switch. Every animation below has a fallback (instant or <=160ms fade).
- Contrast: all text pairs >=4.5:1, graphics >=3:1 (values measured; add a unit test that fails if any token pair regresses).
- Findings: cream text on Plumage #2A7F9E is only 4.1:1, so buttons/links use primary #1F6C88 (5.9:1 with white). Brand Plumage is for the bird, charts, large graphics only. Coral chip text on coral tint uses #8E2F1C (6.3:1).

## 1. Identity
- Direction: Cozy Nest. Warm paper, pebble shapes, twig lines, eggs and leaves. Only prop set: nest, branch, leaf, sun/moon. One main character (the bird); no second.
- Shapes: 8 / 12 / 16 / 24 (cards) / 32 (hero, sheet top) / pebble (chips) / FAB radius 20 (60dp). Nest lip = 24dp concave scoop at the bottom of the Today header.
- Elevation is tonal. Shadows (warm brown #3B2A10 at 7-14%) only on sheets, menus, FAB, light theme. Dark/Black: 1dp hairline (#34474F / #26343B) instead.
- Icons: 24dp grid, 2dp stroke, round caps/joins, outlined at rest, filled when selected. Material Symbols Rounded (wght 400, grade 0) plus ~12 custom glyphs (nest/inbox, feather flag, leaf, today sun).
- Feather divider ornament (2dp quill with 3 barbs) between sections only, not between rows.
- Optional grain: ~40 speckle dots as one cached Path at 6% alpha, Today header + illustrations only. Off in low-end mode.

## 2. Typography (all SIL OFL)
| Role | Latin | Arabic | Size/line (sp) |
|---|---|---|---|
| Display (greeting) | Young Serif 400 | Noto Naskh Arabic 700 | 36/42 · AR 36/56 |
| Headline | Young Serif 400 | Naskh 700 | 26/32 · AR 26/39 |
| Title L | Nunito 800 | Readex Pro 600 | 20/26 · AR 20/30 |
| Title M (task title) | Nunito 700 | Readex Pro 500 | 16/22 · AR 16/26 |
| Body | Nunito 400 | Readex Pro 400 | 14/20 · AR 14/23 |
| Label (chips) | Nunito 800, +0.2sp | Readex Pro 500, no tracking | 12/16 · AR 12/18 |
| Stat numeral | Young Serif 48 (tnum, lnum) | Naskh 700 52 | 48/48 |
- Arabic: no letter-spacing, no italics, no caps. Line height x1.5-1.65.
- Fallbacks: Young Serif -> Nunito 800; Readex -> Noto Sans Arabic; Naskh -> Noto Naskh -> system. Subset Latin+Arabic, static weights, lazy-load display faces.
- Numerals setting: Follow language (default) / Western / Eastern Arabic. Use `NumberFormat` with `ar-u-nu-arab` / `ar-u-nu-latn`; never string-replace digits. One `formatCount(n)` for chart labels and a11y text.

## 3. Colour
See `FeatherTheme.kt` (LightScheme, DarkScheme, BlackScheme, FeatherColors). Key contrast ratios: onSurface 14.7 (L) / 14.5 (D) / 17.9 (B); primary text 5.6 (L) / 9.2 (D); secondary text 6.8 / 9.4 / 10.7; outline 4.1 / 5.0 / 5.4.
- Priority: None neutral outline (no marks) · Low (sky-teal, 1 barb) · Medium (amber, 2 barbs) · High (coral, 3 barbs). Barbs are small strokes inside the 26dp checkbox ring.
- List colours: 8 colours, each paired with a shape (circle, sun, drop, leaf, diamond, square, triangle, moon) so colour is never the only channel.
- Heatmap ("nest straw"): amber ramp, 4 levels + empty. Light F3E6C9 / F8D98F / F2A93B / C77D12 (ink text); Dark 243740 / 5F4B25 / B8862F / F5B955 (cream, cream, ink, ink). Today = 2.5dp primary ring. Future days = dashed outline.
- Time-of-day sky wash on the Today header (flat two-stop gradient): dawn 05:00, day 10:00, dusk 17:00, night 20:00 (device local time). Cross-fade 600ms when the phase changes while opening the screen.

## 4. Illustration kit (all vector drawables, 200x200 box, <=4 flat fills, no gradients)
Bird stays your existing 96x96 art; scenes place it with one transform. Ground shadow #F1E2C2 (light) / #0B1114 (dark). Props: nest bowl = twig brown (#B08A5A light/dark) with weave strokes #8A6A43; eggs = cream ellipse 12x15 with 1-2 speckles; branch = stroke 8 round; leaf = pointed oval; sun = circle r14 + 8 rays; moon = crescent.
| Scene | Composition | Copy EN / AR |
|---|---|---|
| Empty list | bird left, empty nest right, one leaf sways +/-3deg (5s) | "Nothing here yet. Add a thought and I'll keep it warm." / "لا شيء هنا بعد. أضف فكرة وسأُبقيها دافئة." |
| All done | happy bird on nest with 3 eggs, sun, 2 sparkles | "Every egg is in the nest. Go be a person." / "كل البيض في العش. اذهب وعِش يومك." |
| Nothing due today | sleeping bird on a branch, cloud, small sun | "A whole day with nothing due. Suspicious, but nice." / "يوم كامل بلا مواعيد. مريب، لكنه لطيف." |
| No search results | bird tilts head, loupe over bare twigs | "Looked under every twig. Nothing matches “{query}”." / "فتّشت تحت كل غصن. لا شيء يطابق «{query}»." |
| Backup saved | woven basket with lid ajar, moss check badge, sky feather | "Tucked away safely. Your tasks, only yours." / "حُفظت بأمان. مهامك، لك وحدك." |
| Reminders blocked | amber bell crossed by a twig (not a red slash), small bird peeks | "Your phone is keeping my bell quiet. Two taps to let it ring." / "هاتفك يُسكت جرسي. نقرتان لأعيد الرنين." |
| First launch | cracked egg with the bird's head, dawn ground, sun rises 24dp over 600ms | "Hello! I'm Feather. Everything stays on your phone." / "أهلًا! أنا ريشة. كل شيء يبقى على هاتفك." |
| Trash empty | woven bin lid off, happy bird on rim, one leaf | "Tidy as a nest in spring." / "مرتّب كعشٍّ في الربيع." |
Illustrations show at 190dp (140dp at 200% font; hidden if free height <320dp). The Arabic copy is a draft for a native speaker to review.

## 5. Screens (reference: canvas boards 06, 09-13)
### Today (board 09)
- Header 236dp, sky wash, display greeting (time-of-day: Good morning / afternoon / evening / night), date line, "N things left · M done", 5 decorative leaves (done of planned, hidden from TalkBack), bird+branch+sun/moon at the end side (150x110dp, art never mirrored), nest-lip scoop at the bottom.
- Bird mood = day state: pending -> idle; 0 left -> happy + eggs; evening with few left -> sleepy + moon; empty day -> sleeping on branch.
- Header collapses 236 -> 64dp on scroll (position-driven; bird parallax 0.4x, scales to 0.6, greeting cross-fades to the title).
- Rows (card radius 20, 8dp gap, padding 4/14): 48dp checkbox well with 26dp ring (priority colour + barbs), title (<=3 lines then ellipsis), chip row (26dp tall, not targets): friendly date ("Tonight · 7:00 PM", "Today", "Tomorrow", "Sat", "Waiting since Mon" neutral), list chip (shape + name), repeat chip, subtask leaves "2 of 4".
- "Done today · N" collapsed pill under the list. FAB 60dp, radius 20, 24dp from bottom, 20dp from end.
### Task list + drawer (board 10)
- Compact 64dp top bar (menu, title in display face, search). Drawer 316dp wide, 32dp end corners, scrim 50% Ink; selected item = 50dp primaryContainer pebble; counts plain numerals; lists with colour+shape; tags and saved filters as chips; Settings pinned at the bottom. Suggested rename "Overdue" -> "Waiting for you".
- Swipe toward the end = Done (moss, 110dp); toward the start = Reschedule (sky). Uses start/end so RTL mirrors. Drag-reorder lift: -4dp, -0.8deg tilt, 1dp primary hairline, handle at the end.
### Quick add (board 11)
- Sheet top radius 32, 20dp padding, 48dp tool buttons (date, priority, tag, repeat, mic), 56dp send (radius 20). Tokens in the text field are tinted in place; a chip row below pops in per recognised token (36dp tall, icon + friendly text). Hint "I'll read the details as you type" disappears after the 5th add. Parser should accept Arabic-Indic and Latin digits and words like غدًا، الليلة، بعد غد.
### Task editor (board 12)
- Title in display face 28sp on bare paper, notes beneath, no boxes; one settings card with >=52dp rows (date + time pebble, reminder, repeat, list + tags), priority 4-way segment of >=48dp pebbles (chosen = ring + barbs + label), Steps header with leaf progress, steps list; Save as a visible 48dp pill.
### Settings (board 13)
- Grouped 24dp cards with small group labels. Theme segment: System / Light / Dark / Black. Language, Numerals, Reduce motion (follows system unless overridden), "Little celebrations" switch, Haptics. Reminders health: working = quiet moss badge; not working = amber card, plain sentence, one primary button; never red. Backup/Restore rows. Support: a tonal card at the bottom, one sentence, one button, never a popup.
### Widgets (board 07)
Quick add 4x1 (72dp), Next task 4x2 (160dp, sky wash, ring to complete), Today 4x4 (340dp, header with greeting + count + 3-4 rows + add button). Radius 28dp. Glance/RemoteViews, vector drawables only, no animation. Bird mood chosen at update time. Ring buttons announce "Mark <title> as done".
### Empty states (board 06)
One illustration, one sentence, at most one action.

## 6. Your rhythm (Insights; board 14, component 14b)
Gentle journal tone: no percentages vs last week, no red, no "you missed".
- Hero: big numeral (Young Serif / Naskh 60) = completions in the last 7 days, one kind sentence, happy bird, sky wash.
- Last 7 days bars (rolling, ending today): 28dp wide, radius 10 top / 5 base, amber, max height 92dp, scale = max(5, week max), no gridlines/axis, value above each bar (tabular nums). Rest day (0) = 16dp curled leaf at 55% opacity, not a bar. Today's bar has a dashed outline. Week order follows reading direction; week start follows locale (Saturday in Egypt).
- Rhythm: 14 dots (7x2): filled amber = showed up, hollow ring = rest day; headline "N of the last 14 days". Paused days are removed from the 14 before counting. No streak number that can reset. "Pause my rhythm" 48dp button.
- Month nest: 7-column grid, square cells >=44dp, radius 13, date inside, 4 straw tones; today ring; future dashed; tap -> bottom sheet with that day's completed tasks. Semantics: "October 12, 4 finished".
- Busiest day (sentence) and best time (4 bars: morning, afternoon, evening, night; peak amber).
- Lists: horizontal rounded bars with list colour + shape, counts end-aligned, scaled to the biggest list. No pie.
- Repeating tasks doing well: only >=4 of last 7 days or 4 weeks running; 7 small leaves. Struggling tasks are not shown.
- New user: before 3 completions show only hero (0 + dashed egg) + "Your rhythm starts here" card; charts appear as data arrives, never empty axes. AR: «إيقاعك يبدأ من هنا. أنهِ مهمة وسيظهر أول بيضة.»
- Data: everything computed locally from the completions table (per day / week counts, on-time vs late, weekday, time of day, per list/tag, repeating consistency). Render each chart in one Canvas (not dozens of composables). Provide a one-line text summary per chart for TalkBack.

## 7. Motion spec (full list is board 15; essentials here)
Curves: Settle (0.2,0,0,1) arrivals · Soft (0.3,0,0.2,1) moves · Leave (0.4,0,1,1) exits · InOut (0.4,0,0.2,1) · Draw (0.5,0,0.1,1) · Nestle spring d0.70 k380 · Pop spring d0.55 k500. Budget: feedback <=200ms, transitions <=400ms, delight <=700ms. Input is never blocked; loops pause offscreen, in battery saver and with reduced motion. Use graphicsLayer (transform/alpha) except row insert/remove.

| Trigger | Animation | Duration | Easing | Reduced motion | Haptic |
|---|---|---|---|---|---|
| Tap ring | squeeze 0.9 (90ms) -> fill primary -> pop 1.12 -> 1; check stroke draws (trimPath 0->1) | 200ms | Pop / Draw | instant filled; check fades 90ms | CLOCK_TICK |
| Same | strike grows from reading-direction start; text alpha 0.6 | 200ms (+100 delay) | Soft | instant | n/a |
| Same | feather burst: 6-7 leaf-feathers (sky, amber, coral, moss), 28-52dp out, spin +/-40deg, fall 10dp, fade over last 40%; one Canvas | 580ms from 120ms (ends 700ms) | Settle | skipped; 120ms halo instead | n/a |
| Same | bird hop -14dp (45%) -> 0, squash 0.92 on landing, happy eyes 800ms | 400ms from 160ms | Settle up/Leave down | swap to happy eyes, 120ms fade | n/a |
| After strike | row fades (first 140ms) + collapses; neighbours slide; Done count pulses +1 | 280ms from 420ms | Soft; Nestle | 120ms fade-out | n/a |
| Last open task of the day (once per calendar day) | header warms +15% amber, halo r44->62, bird double hop, 12 warm feathers, 5 eggs settle 60ms apart, headline -> "All done." | 1100ms, interruptible | Settle; eggs Pop | headline+eggs fade 160ms | CONFIRM, + CLOCK_TICK at 420ms |
| Complete repeating task | row fades 140ms; leaf-feather flies quad-bezier to date chip/Upcoming; collapse; reappears with new chip; snackbar "Back tomorrow" + Undo | 360 + 220 + 200ms | Soft; Settle | 140ms crossfade | CLOCK_TICK |
| Undo | row expands, check erases | 240ms | Settle | instant | CLOCK_TICK |
| Tap FAB | pebble morphs into sheet (radius 20->32 top), scrim 0->45% | 320ms open / 240ms close | Settle / Leave | 160ms crossfade | n/a |
| Token recognised | chip pops in (scale .7->1, alpha 0->1), field text tints 150ms; stagger 40ms | 160ms | Pop | 80ms fade | CLOCK_TICK (debounce 400ms) |
| Token removed | chip shrinks .8 + fades | 120ms | Leave | instant | n/a |
| Mic | 3-bar soft waveform, level-driven | loop 900ms | InOut | static ring | CLOCK_TICK |
| Send | sheet closes; row inserts (height 0->row); primaryContainer 30% highlight fades | 240 / 280 / 800ms | Leave / Nestle / Soft | highlight only | VIRTUAL_KEY |
| List -> editor | shared axis X, 30dp, mirrors in RTL | 300ms | Soft | 160ms crossfade | n/a |
| Drawer | slide from start, scrim 50% | 280 open / 220 close | Settle / Leave | 120ms fade | n/a |
| Destination switch | fade-through, scale .96->1 | 240ms | InOut | 120ms fade | n/a |
| Menu/dialog | scale .92->1 + alpha | 160ms | Settle | 100ms fade | n/a |
| Reorder | lift -4dp, tilt -0.8deg, glow; neighbours spring; drop overshoot | 140 / 180ms | Settle / Nestle | no tilt; instant swaps | CLOCK_TICK pickup + per slot (>=80ms), VIRTUAL_KEY drop |
| Swipe row | follows finger; icon scales .8->1 at 40% (100ms Pop); commit slides off 200ms + collapse 280ms; cancel springs back | 200 + 280 | Leave; Nestle | no icon scale; snap | CLOCK_TICK at threshold, CONFIRM on commit |
| Scroll Today | header collapse by scroll position | scroll-driven | linear | snap at 24dp | n/a |
| Idle bird | blink every 4-7s (120ms), head tilt 2deg every 12-20s (400ms), tail flick ~30s | loop | Soft | static pose | n/a |
| Sleeping bird | scaleY 1->1.03 breath; "z" rises 20dp every 5s | 4000ms | InOut | static + fixed "z" | n/a |
| Phase change | sky cross-fade; sun/moon swaps | 600ms | InOut | instant | n/a |
| Season | static recolour only (leaf amber Oct-Nov; blossom in spring) | none | n/a | same | n/a |
| Empty state | illustration fades + rises 12dp; leaf sways | 320ms | Settle | fade only | n/a |
| Insights opens (first composition) | hero count-up 600ms; bars grow 360ms (40ms stagger); dots pop (20ms stagger); calendar rows fade+scale .85->1 (30ms/row); list bars 420ms (60ms stagger) | <=1100ms total | Settle; dots Pop | final state, 120ms fade | n/a |
| Tap calendar day | cell presses to .94; sheet opens | 120 + 280ms | Settle | sheet fades 160ms | CLOCK_TICK |

Haptics: CLOCK_TICK = selection/threshold/slot; CONFIRM (API 30+, else two CLOCK_TICKs 60ms apart) = success; VIRTUAL_KEY = soft thud. Never REJECT. Respect system touch feedback + in-app switch; debounce >=80ms; none while scrolling.
Sound: none by default (optional future opt-in: ~80ms wooden "tok" on check, 3-note marimba on last task; never on launch/add/delete/reminders; respect silent/DND).

## 8. Engagement (all in-app, each with its own Settings switch)
- Recommended: morning "your day" card (once per day, dismissible; optional notification switch OFF by default); evening wind-down card after 19:00 ("You finished 4 today. Two are still waiting. Nothing is lost. Want them tomorrow?" with Move to tomorrow / Leave them); weekly feather (generated locally: colour = busiest list, barbs = completions, notch for rest days; shown on Your rhythm on the first day of the week); Your nest (cosmetic accessories/decorations earned by ordinary milestones: e.g. scarf at 10 tasks, fairy lights after first backup, bunting after a repeating task kept 4 weeks, lantern after 10 reminders, moss patch after 3 lists; locked: tiny glasses at 25, flower pot after voice add, party hat at 100). Unlocks are permanent, deterministic, never time-limited, never paid, announced by a quiet dot on Settings.
- Rejected: streak counters/freezes/paid repair; daily rewards, XP, levels, leaderboards; re-engagement notifications; red overdue states and launcher badges; random or time-limited items; confetti on every action; sharing cards/invites/comparisons.
- Maybe later: quiet "Your year in feathers" page in December, computed locally.

## 9. Build order (biggest "alive" per effort first)
1. Theme: ColorSchemes + FeatherColors + shapes + fonts (1-2d). No layout changes.
2. `rememberReduceMotion()` + completion moment (ring, strike, collapse, one-Canvas burst, haptics) (3d). Reuse the existing bird hop.
3. Today header + richer rows (greeting, N left, sky wash, bird mood; priority ring+barbs, friendly dates, list shapes, subtask leaves) (3-4d).
4. 8 empty-state illustrations + props as vector drawables (3d).
5. Quick-add polish: token tinting, chip pop-ins, task-landing highlight, FAB->sheet (start with simple scale+fade) (2-3d).
6. Your rhythm v1 (hero, bars, month nest, rhythm dots; one Canvas each), then entry animations; new query layer over completions (1-1.5w).
7. Widgets restyle with Glance (3d).
8. Morning/evening cards (2d).
9. Last-task celebration + repeating flight (3-4d).
10. Ambient life + seasons (2d).
11. Weekly feather + Your nest (1w).

## 10. Low-end phone risks and cheaper alternatives
| Idea | Cost | Cheaper way |
|---|---|---|
| Grain texture | fill-rate/memory | cached 40-dot Path at 6% alpha, Today header only; off in low-end mode |
| Blur/glow | RenderEffect is GPU-heavy, API 31+ | flat radial-alpha circle; 1dp primary border for lifted row |
| Confetti as composables | per-particle recomposition | one Canvas, <=7 pre-built Paths, one Animatable; 3 particles in low-end mode |
| Shadows on every row | overdraw | hairline + tonal surface; shadows on sheets/menus/FAB only |
| FAB container transform | shared-element measure/draw twice | scale+fade of sheet from FAB position |
| Always-on bird loop | wakes compositor, battery | timer-driven blink/tilt tied to lifecycle; AnimatedVectorDrawable one-shots; nothing offscreen |
| Check draw via PathMeasure | per-frame allocation | cache the Path segment or AVD with trimPathEnd |
| Heatmap as 35-42 animated composables | many nodes | one Canvas, one row-reveal progress |
| 4 font families | APK size, cold start | subset static weights; lazy-load display faces |
| Sky cross-fade | two brushes drawn | lerp colours once, or instant in low-end mode |
| Animating all neighbours on collapse | re-measure | `animateItem()` on visible keys only; skip spring above ~100 rows |
Low-end mode (automatic): `isLowRamDevice()`, battery saver, or >10% janky frames over 3s on Today -> disable grain, ambient loops, sky cross-fade; 3 feathers instead of 7. Ship a Baseline Profile; macrobenchmarks for "scroll Today" and "complete a task" on a 2GB device.

## 11. Acceptance checklist
- [ ] All three themes pass the contrast test (4.5:1 text, 3:1 graphics).
- [ ] Every screen verified in RTL and at 200% font scale; every target >=48dp.
- [ ] TalkBack: decoration silent; priority, chart summaries and heatmap cells read as sentences.
- [ ] Reduced motion: every animation in section 7 has its fallback; celebrations switch independent of reduced motion.
- [ ] No network permission added; no new tracking; all stats computed from the local completions table.
- [ ] Arabic copy reviewed by a native speaker; digits go through `NumberFormat`, never string replacement.
