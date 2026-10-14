# Store graphics

Vector sources, built from the same paths as the app's bird (`app/src/main/java/com/elkrrai/feather/ui/brand/BirdShapes.kt`).

| File | Size | Used for |
|---|---|---|
| `icon.svg` | 512 x 512 | Google Play app icon (export as 32-bit PNG, no rounded corners: the store rounds it) |
| `feature-graphic.svg` | 1024 x 500 | Google Play feature graphic (export as PNG or JPEG, no transparency) |

Export to PNG with any vector tool, for example Inkscape:

```bash
inkscape icon.svg --export-type=png --export-filename=icon-512.png
inkscape feature-graphic.svg --export-type=png --export-filename=feature-graphic.png
```

The feature graphic uses a plain system font for its text; open it in a vector editor and set the brand font before exporting if there is one. An Arabic version needs its own text (right-to-left) and is not made yet.

Screenshots are taken from the running app (see `store/STORE.md`).
