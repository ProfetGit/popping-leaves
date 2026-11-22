#!/usr/bin/env python3
"""Popping Leaves icon, banner and promo tile from the one-leaf-cube loop (frames_logo/, 1536 px, 32 px per unit, 198 frames
= 3.3 s at 60 fps; oak, birch and autumn cubes take turns). Same layout and plate as compose.py (the tree scene).

  python3 compose_logo.py [icon|banner|tile|all|stills]
"""
import sys
from pathlib import Path

from PIL import Image

import compose as C

HERE = Path(__file__).resolve().parent
OUT = C.OUT
FR = HERE / "frames_logo"
N = 108
STILL = 45
ICON_WIN = 36 * 32


def frame(i):
    return Image.open(FR / f"frame_{i:03d}.png").convert("RGBA")


def icon_box():
    x0, y0, x1, y1 = frame(STILL).getchannel("A").getbbox()
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    return (cx - ICON_WIN // 2, cy - ICON_WIN // 2, cx + ICON_WIN // 2, cy + ICON_WIN // 2)


def icon_one(i, box):
    f = C.art(frame(i).crop(box).resize((576, 576), Image.NEAREST), 8)
    c = Image.new("RGBA", (576, 576), C.hexc(C.PLATE))
    c.alpha_composite(f)
    return c


def banner_one(back, i):
    c = back.copy()
    c.alpha_composite(C.art(frame(i).resize((480, 480), Image.NEAREST), 10), (936, 16))
    c.putalpha(C.corner_mask(1536, 512, 8, (4, 2, 1, 1)))
    return c


def tile_one(back, i):
    c = back.copy()
    c.alpha_composite(C.art(frame(i).resize((288, 288), Image.NEAREST), 6), (8, 32))
    c.putalpha(C.corner_mask(768, 352, 4, (3, 2, 1)))
    return c


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    box = icon_box()
    if what == "stills":
        icon_one(STILL, box).save(OUT / "icon" / "icon-576.png")
        banner_one(C.banner_back(), STILL).save(OUT / "banner" / "banner.png")
        tile_one(C.tile_back(), STILL).save(OUT / "promo-tile" / "promo-tile.png")
        sys.exit(0)
    if what in ("icon", "all"):
        fr = [icon_one(i, box) for i in range(N)]
        C.webp(fr, OUT / "icon" / "icon-animated-60fps.webp")
        fr[STILL].save(OUT / "icon" / "icon-576.png")
        C.sheet(fr, OUT / "icon" / "icon-sheet.png", step=7)
    if what in ("banner", "all"):
        back = C.banner_back()
        fr = [banner_one(back, i) for i in range(N)]
        C.webp(fr, OUT / "banner" / "banner-animated-60fps.webp")
        fr[STILL].save(OUT / "banner" / "banner.png")
    if what in ("tile", "all"):
        back = C.tile_back()
        fr = [tile_one(back, i) for i in range(N)]
        C.webp(fr, OUT / "promo-tile" / "promo-tile-60fps.webp")
        fr[STILL].save(OUT / "promo-tile" / "promo-tile.png")
