#!/usr/bin/env python3
"""Compose the Popping Leaves icon, banner and promo tile from the Blockbench renders in frames/ (2880 px, 30 px per unit,
transparent; 216 frames = the 3.6 s loop at 60 fps). Lettering and textures are Aseprite sprites (draw_lettering.lua,
draw_textures.lua).

  python3 compose.py [icon|banner|tile|all|stills|sheet]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
OUT = HERE.parent
FR = HERE / "frames"
SPR = HERE / "sprites"
N = 198
import os
_P = os.environ.get("PL_PLATE", "#5AA9E6,#12263F,#A9D4F5,#3F86C2").split(",")   # plate, ink, stitch, tile border
PLATE, PLATE_DARK, STITCH, BORDER2 = _P
STILL_ICON = 44      # axe at the log, bark chips
STILL_BANNER = 92    # leaves mid-pop, saplings in the air
SHIFT_UNITS = 2      # the tree sits ~2 units below the camera target: lift it so it is centred


def hexc(h, a=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)


def sprite(name):
    return Image.open(SPR / f"{name}.png").convert("RGBA")


def scaled(name, k):
    im = sprite(name)
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def frame_at(i, px_per_unit):
    """The whole 96-unit render at px_per_unit (30 / px_per_unit is an integer: 5 -> 6, 6 -> 5, 3 -> 10), lifted by SHIFT_UNITS."""
    size = 96 * px_per_unit
    im = Image.open(FR / f"frame_{i:03d}.png").convert("RGBA").resize((size, size), Image.NEAREST)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im, (0, -SHIFT_UNITS * px_per_unit))
    return out


def art(im, u):
    """Round ink outline, one unit wide."""
    alpha = np.array(im.getchannel("A")) > 127
    grown = np.zeros_like(alpha)
    for dy in range(-u, u + 1):
        for dx in range(-u, u + 1):
            if dx * dx + dy * dy <= u * u:
                grown |= np.roll(np.roll(alpha, dy, axis=0), dx, axis=1)
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    out.paste(Image.new("RGBA", im.size, hexc(PLATE_DARK)), (0, 0), Image.fromarray((grown * 255).astype(np.uint8)))
    out.alpha_composite(im)
    return out


def plate(w, h, grid, stitch=False):
    im = Image.new("RGBA", (w, h), hexc(PLATE))
    d = ImageDraw.Draw(im)
    if stitch:
        gw, gh = w // grid, h // grid
        inset = 2
        col = hexc(STITCH)
        for x in range(inset + 1, gw - inset - 1, 2):
            for y in (inset, gh - inset - 1):
                d.rectangle((x * grid, y * grid, (x + 1) * grid - 1, (y + 1) * grid - 1), fill=col)
        for y in range(inset + 1, gh - inset - 1, 2):
            for x in (inset, gw - inset - 1):
                d.rectangle((x * grid, y * grid, (x + 1) * grid - 1, (y + 1) * grid - 1), fill=col)
    return im


def corner_mask(w, h, grid, cut):
    gw, gh = w // grid, h // grid
    m = Image.new("L", (gw, gh), 255)
    for row, n in enumerate(cut):
        for x in range(n):
            for p in ((x, row), (gw - 1 - x, row), (x, gh - 1 - row), (gw - 1 - x, gh - 1 - row)):
                m.putpixel(p, 0)
    return m.resize((w, h), Image.NEAREST)


def webp(frames, path):
    dur = [17, 17, 16] * (len(frames) // 3 + 1)
    path.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=dur[:len(frames)], loop=0,
                   lossless=True, quality=100, method=6, exact=True)
    print(f"{path.relative_to(OUT)}: {path.stat().st_size / 1024:.0f} KiB, {len(frames)} frames")


def icon_one(i):
    c = Image.new("RGBA", (576, 576), hexc(PLATE))
    c.alpha_composite(art(frame_at(i, 6), 6))
    return c


def banner_back():
    W, H, G = 1536, 512, 8
    back = plate(W, H, G, stitch=False)   # no dotted border on the banner (director, 2026-11-22)
    back.alpha_composite(scaled("title_both", 6), (72, 167))
    back.alpha_composite(scaled("tagline", 6), (74, 291))
    return back


def banner_one(back, i):
    c = back.copy()
    c.alpha_composite(art(frame_at(i, 5), 5).crop((0, 0, 480, 440)), (1000 - 240 + 80, 34))   # lowered so the apple clears the border; the bottom rows hold only stray chip shadows
    c.putalpha(corner_mask(1536, 512, 8, (4, 2, 1, 1)))
    return c


def tile_back():
    W, H, G = 768, 352, 4
    back = plate(W, H, G)
    d = ImageDraw.Draw(back)
    d.rectangle((0, 0, W - 1, H - 1), outline=hexc(PLATE_DARK), width=4)
    d.rectangle((4, 4, W - 5, H - 5), outline=hexc(BORDER2), width=4)
    back.alpha_composite(scaled("title_popping", 4), (304, 40))
    back.alpha_composite(scaled("title_leaves", 4), (304, 104))
    back.alpha_composite(scaled("tagline", 3), (304, 182))
    chip = scaled("chip_client", 3)
    cw, ch = chip.width + 24, chip.height + 20
    d.rectangle((304, 236, 304 + cw, 236 + ch), fill=hexc("#2B2160"), outline=hexc(PLATE_DARK), width=4)
    back.alpha_composite(chip, (304 + 12, 236 + 10))
    return back


def tile_one(back, i):
    c = back.copy()
    c.alpha_composite(art(frame_at(i, 3), 3).crop((0, 0, 288, 262)), (8, 42))
    c.putalpha(corner_mask(768, 352, 4, (3, 2, 1)))
    return c


def stills():
    icon_one(STILL_ICON).save(OUT / "icon" / "icon-576.png")
    banner_one(banner_back(), STILL_BANNER).save(OUT / "banner" / "banner.png")
    tile_one(tile_back(), STILL_BANNER).save(OUT / "promo-tile" / "promo-tile.png")


def sheet(frames, path, cols=8, step=9, scale=0.25):
    ims = frames[::step]
    w, h = int(ims[0].width * scale), int(ims[0].height * scale)
    rows = (len(ims) + cols - 1) // cols
    s = Image.new("RGBA", (cols * w, rows * h), (20, 20, 30, 255))
    for k, im in enumerate(ims):
        s.paste(im.resize((w, h), Image.LANCZOS), ((k % cols) * w, (k // cols) * h))
    s.save(path)


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    for d in ("icon", "banner", "promo-tile"):
        (OUT / d).mkdir(exist_ok=True)
    empty = [i for i in range(N) if Image.open(FR / f"frame_{i:03d}.png").convert("RGBA").getchannel("A").getbbox() is None]
    print("empty frames:", empty)
    if what == "stills":
        stills()
    if what in ("icon", "all"):
        fr = [icon_one(i) for i in range(N)]
        webp(fr, OUT / "icon" / "icon-animated-60fps.webp")
        fr[STILL_ICON].save(OUT / "icon" / "icon-576.png")
        sheet(fr, OUT / "icon" / "icon-sheet.png")
    if what in ("banner", "all"):
        back = banner_back()
        fr = [banner_one(back, i) for i in range(N)]
        webp(fr, OUT / "banner" / "banner-animated-60fps.webp")
        fr[STILL_BANNER].save(OUT / "banner" / "banner.png")
    if what in ("tile", "all"):
        back = tile_back()
        fr = [tile_one(back, i) for i in range(N)]
        webp(fr, OUT / "promo-tile" / "promo-tile-60fps.webp")
        fr[STILL_BANNER].save(OUT / "promo-tile" / "promo-tile.png")
