# Art sources

Editable sources for artwork the app ships as PNG. Nothing here goes into the APK.

| Folder or file | What it is |
|---|---|
| `owl/` | The owl mascot, layered, one folder per pose. See `owl/README.md`. |
| `scenes/` | Recraft pictures for the empty Overdue scene: `hills_far.webp` and `hills_near.webp`, 2000 x 496, as received. Their PNGs in `drawable-nodpi` are the same at 1000 x 248 with stray near-transparent pixels removed and the outermost 8 columns of each side cut off (they have a faint dark rim that showed as a line where tiles join). `hills_near` does not match at its left and right edges (the crest differs by about 10 px), so the scene tiles both flipped, one way then the other. |
| `scenes/nests/` | The four nests the person can choose on Your nest (`domain/nest/NestStyle.kt`): `braided` (the light one, the default), `twigs`, `straw`, `rope`, as received from Recraft. Each is cut into a back picture (the whole nest) and a front picture (only its front rim, cut along a hand-drawn line) in `drawable-nodpi/nest_<name>_back.webp` and `_front.webp`, so the bird sits between them. `NestScene.kt` holds each nest's proportions and where the bird's feet land. `twigs`, `straw` and `rope` are dark brown like the owl and need lighter versions (same prompt as the braided one) to stand out. |
| `perch_branch.png` | The branch the owl sits on (Today header, "nothing due" scene, Your nest). Its PNG in `drawable-nodpi` is the same at 768 x 384. |
| `goal_tree.svg` | The leafy tree behind the owl in the empty list. Its PNG is `drawable-nodpi/goal_tree.png`. |

Store graphics (icon, feature graphic) are in `store/graphics`.

## Who is the mascot

The owl, everywhere: Today, the empty states, Your rhythm, the tip sheet, the completion hop, the
nest, the home-screen widgets, the launch screen, the launcher icon, the notification icon and the
store graphics. The old blue songbird is gone from the app; the Settings screen, which kept it for a
while as a tribute, now shows the owlet (`settings_chick`, from `art/scenes/chick/settings.png`).

| Where | Made from |
|---|---|
| Widgets (`widget_owl_idle`, `_sleeping`, `_happy`) | The owl's layers stacked and cropped to 256 px |
| Launch screen (`splash_owl`) | The launcher owl (`ic_launcher_bird`) at 768 px, inside the middle circle of the 288 dp icon |
| Launcher icon (`ic_launcher_bird`) and themed icon (`ic_launcher_bird_mono`) | The young owl from `art/owl/launcher.png`, 62 percent of the height of the 1024 artboard so it stays inside the safe zone of any icon shape; the themed one is its outline in white, alpha only |
| Notification icon (`ic_stat_feather`, in each density) | The resting owl's outline in white |
| Store icon and feature graphic (`store/graphics`) | The resting owl on the launch screen's sky; the title in Young Serif |

These were made once from the layers in `drawable-nodpi`, not by a script. A re-drawn owl for any of
them (a flat mark for the widgets, say) would replace the stacked one.

### Not made yet

- Clouds drawn in the owl's style for the empty Overdue scene (`scenes/ClearSkiesScene.kt`). Today the clouds, sun and moon are simple shapes drawn in code; the hills are Recraft art.
- An owl with its eyes half open, for falling asleep and waking. Today it cross-fades between the
  sleeping owl and the resting owl.
- Owl art for the scenes that still hold props drawn for the songbird: check each placement in
  `ui/brand/scenes/Scenes.kt` on the phone (the owl fills more of its box than the songbird did).
