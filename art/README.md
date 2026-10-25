# Art sources

Editable sources for artwork the app ships as PNG. Nothing here goes into the APK.

| Folder or file | What it is |
|---|---|
| `owl/` | The owl mascot, layered, one folder per pose. See `owl/README.md`. |
| `goal_tree.svg` | The leafy tree behind the owl in the empty list. Its PNG is `drawable-nodpi/goal_tree.png`. |

Store graphics (icon, feature graphic) are in `store/graphics`.

## Who is the mascot

The owl. The old blue songbird (`ui/brand/BirdShapes.kt`) is retired from every screen: Today,
the empty states, Your rhythm, the tip sheet and the completion hop all show the owl through
`BirdIllustration` / `OwlIllustration`. The launcher icon is the owl too.

### Still the old blue bird

These have no owl art yet. Until they do, the app shows two different characters:

| Where | Files | What the owl version needs |
|---|---|---|
| Widgets | `drawable/ic_bird_mark.xml`, `ic_bird_sleeping.xml`, `ic_bird_happy.xml` (`widget/WidgetStyle.kt`, `QuickAddWidget.kt`) | Small owl head or body in three moods, as PNG. Glance cannot draw the layered owl. |
| Settings, About | `ic_bird_mark` in `SettingsScreen.kt` | The owl mark. |
| Launch screen | `ic_splash_bird.xml`, `avd_splash_bird.xml`, `themes.xml` | A 288 dp owl (centered in a 192 dp circle) and, if it moves, an animated version. |
| Notification icon | `ic_stat_feather.xml` | A white silhouette, 24 dp, transparent background. Android recolours it. |
| Store graphics | `store/graphics/icon.svg`, `feature-graphic.svg` | The owl instead of the songbird. |
| Themed (monochrome) launcher icon | `mipmap-anydpi/ic_launcher*.xml` use `ic_launcher_bird_mono.png` | Already the owl; check it on a themed-icon phone. |
| Animation lab, bird lab (debug only) | `BirdCanvas` in `app/src/debug` | Show the old bird on purpose, for comparison; remove when the widgets are done. |

When the last of these is replaced, delete `BirdShapes.kt`, `BirdCanvas`, `BirdArtworkTest` and the
`ic_bird_*` drawables.

### Not made yet

- An owl with its eyes half open, for falling asleep and waking. Today it cross-fades between the
  sleeping owl and the resting owl.
- Owl art for the scenes that still hold props drawn for the songbird: check each placement in
  `ui/brand/scenes/Scenes.kt` on the phone (the owl fills more of its box than the songbird did).
