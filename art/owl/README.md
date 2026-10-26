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
| `side/` | Flying seen from the side, facing right, on the empty Overdue screen (`ClearSkiesScene`). Beats its wings on a hinge at the shoulder and bobs; clouds drift past. | `owl_side_body`, `owl_side_wing_down`, `owl_side_wing_up` | none |

Code: `app/src/main/java/com/elkrrai/feather/ui/brand/OwlIllustration.kt` (idle, sleeping and the front-facing flight) and `SideOwl.kt` (the side view). The movement comes from
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

## The side view

`side/` holds the Recraft originals: `body.png` and the two wings, `wing_down.png` (curved, hanging)
and `wing_up.png` (swept up), each 1024 x 1024 with the wing drawn on its own. The three layers in
`res` were made once from them, not by `render.py`: they sit on a shared 1536 x 1536 artboard (the
body in the middle, so the raised wing is not cut off), each wing enlarged to 1.35 with its base put
on the body's shoulder at (856, 736) (the base is at about (530, 530) in `wing_down.png` and
(440, 600) in `wing_up.png`), then reduced to 768 x 768. `SideOwl.kt` turns each wing about that
point (`SIDE_SHOULDER`), 35 degrees from hanging to raised: more than that swings the curved down
wing over the body in an arch. The body had a folded wing removed by Recraft (no outline left, the
back reads as plain feathers); its chest mark is a square, not the diamond of the other poses.
