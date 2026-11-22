#!/usr/bin/env python3
"""Reuse the author's own Timber icon sprites (packs/Timber/dev/icon/sprites) for the Popping Leaves art: leaves, log, axe,
apple. Run after draw_textures.lua (it draws the originals first; this overwrites them). Prints the palettes the chip sheets use."""
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
SRC = HERE.parents[3] / "packs/Timber/dev/icon/sprites"
DST = HERE / "sprites"
MAP = {"leaves": "leaves_top", "leaves_left": "leaves_left", "leaves_right": "leaves_right",
       "log_side_left": "log_left", "log_side_right": "log_right", "axe_item": "axe_item", "apple_item": "apple_item"}
for a, b in MAP.items():
    Image.open(SRC / f"{a}.png").convert("RGBA").save(DST / f"{b}.png")
top = Image.open(SRC / "mini_log_top.png").convert("RGBA")
top.resize((16, 16), Image.NEAREST).save(DST / "log_top.png")


def pal(name):
    im = Image.open(SRC / f"{name}.png").convert("RGBA")
    cols = {}
    for p in im.getdata():
        if p[3] > 127:
            cols[p[:3]] = cols.get(p[:3], 0) + 1
    return ["#%02X%02X%02X" % c for c in sorted(cols, key=lambda c: sum(c))]


if __name__ == "__main__":
    print("leaves", pal("leaves"))
    print("log", pal("log_side"))
