"""Renders the owl's SVG layers in this folder to the PNGs the app draws.

    pip install resvg_py
    python art/owl/render.py            # every set
    python art/owl/render.py sleep fly  # only these sets

Every SVG is a 1024 x 1024 artboard and every PNG is that artboard at 512 x 512 with a transparent
background, so the layers of a set stack on top of one another with no positioning.
"""
import sys
from pathlib import Path

import resvg_py

HERE = Path(__file__).resolve().parent
RES = HERE.parent.parent / "app" / "src" / "main" / "res" / "drawable-nodpi"
SIZE = 512

# set -> {svg in this folder: png name in res/drawable-nodpi}. Layers that are only kept as a source
# (the flattened poses and the sleeping head and feet) are left out, so the app ships no unused art.
SETS = {
    "idle": {
        "idle/body_back.svg": "owl_body_back",
        "idle/body.svg": "owl_body",
        "idle/wing.svg": "owl_wing",
        "idle/chest.svg": "owl_chest",
        "idle/head.svg": "owl_head",
        "idle/eyes_open.svg": "owl_eyes_open",
        "idle/eyes_closed.svg": "owl_eyes_closed",
        "idle/preen.svg": "owl_preen",
    },
    "sleep": {
        "sleep/full.svg": "owl_sleep_body",
        "sleep/eyes.svg": "owl_sleep_eyes",
    },
    "fly": {
        "fly/body.svg": "owl_fly_body",
        "fly/head.svg": "owl_fly_head",
        "fly/eyes.svg": "owl_fly_eyes",
        "fly/wing_left_up.svg": "owl_fly_wing_left_up",
        "fly/wing_left_down.svg": "owl_fly_wing_left_down",
        "fly/wing_right_up.svg": "owl_fly_wing_right_up",
        "fly/wing_right_down.svg": "owl_fly_wing_right_down",
    },
}


# Layers drawn at a different scale or place than the pose they belong to, fitted here instead of
# editing the Recraft source. The flying eyes came out about 3.5 times too big for the head: this
# shrinks them to 0.28 around their own centre and puts that centre between the owl's eyes.
FITS = {
    "fly/eyes.svg": "translate(511.5 495.5) scale(0.28) translate(-511.5 -483.5)",
}


def read_svg(svg: str) -> str:
    text = (HERE / svg).read_text(encoding="utf-8")
    fit = FITS.get(svg)
    if fit is None:
        return text
    open_end = text.index(">", text.index("<svg")) + 1
    close_start = text.rindex("</svg>")
    return f'{text[:open_end]}<g transform="{fit}">{text[open_end:close_start]}</g>{text[close_start:]}'


def main(names: list[str]) -> None:
    for name in names or SETS:
        for svg, png in SETS[name].items():
            data = resvg_py.svg_to_bytes(svg_string=read_svg(svg), width=SIZE, height=SIZE)
            (RES / f"{png}.png").write_bytes(bytes(data))
            print(f"{svg} -> {png}.png ({len(data) // 1024} KB)")


if __name__ == "__main__":
    main(sys.argv[1:])
