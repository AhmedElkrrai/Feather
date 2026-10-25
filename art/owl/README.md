# The owl: layered art

The owl is Feather's one character. These are its Recraft sources (SVG, 1024 x 1024 artboard,
every layer of a pose on the same artboard so they stack with no positioning). The app does not
draw SVG: `render.py` turns the layers it uses into the PNGs in
`app/src/main/res/drawable-nodpi`, and the SVGs stay out of the APK.

```bash
pip install resvg_py
python art/owl/render.py            # every set
python art/owl/render.py sleep fly  # only these sets
```

Layers are rendered at 512 x 512 with a transparent background.

## Poses

| Folder | Pose | Layers drawn by the app | Kept only as a source |
|---|---|---|---|
| `idle/` | Upright, three quarters on. Breathes, blinks, tilts its head, preens. | `body_back`, `body`, `wing`, `chest`, `head`, `eyes_open`, `eyes_closed`, `preen` | |
| `sleep/` | Hunched, eyes shut. Breathes slowly, the glow of its eyes pulses, z letters float up (drawn in code). | `full` (as `owl_sleep_body`), `eyes` (the glow, laid over the body) | `head`, `feet` |
| `fly/` | Front-facing, wings spread. Beats its wings, loops over its perch and lands. | `body`, `head`, `eyes`, and each wing in four drawings: `wing_*_down`, `wing_*_curl`, `wing_*_mid`, `wing_*_up` | `full_up`, `full_down` (the whole owl, to check a stack against) |

Code: `app/src/main/java/com/elkrrai/feather/ui/brand/OwlIllustration.kt`. The movement comes from
`BirdPose.kt` (`birdPose`, `birdFlightPose`), so a new pose only needs its layers.

## Notes for the artist

- **Fly eyes** were drawn about 3.5 times too big for the head. `render.py` shrinks them to 0.28
  around the owl's eyes (`FITS`); a re-export at the right size lets that entry go.
- **The fly chest diamond** is drawn in code (it sits under the head layer in the art), so the
  `body` layer does not show it.
- **Wing beat** cross-fades four drawings per wing: lowered, curled, fanned, raised (`OwlIllustration.kt`,
  `wingPoseAlpha`). The `_mid` and `_curl` wings are Recraft PNGs, not SVG: the files here are the
  originals (1024 x 1024); the ones in `res` are the same at 512 x 512 with the faint green fringe of
  the backdrop removed from the edge pixels (alpha below 250: green capped at the larger of red and
  blue), done once, not by `render.py`.
- **Sleep head** and **feet** are separate layers in case the head ever needs to move on its own;
  that would also need a body layer without the head.
- Send new frames as transparent 1024 x 1024 PNG or SVG on the same artboard, one file per layer.
