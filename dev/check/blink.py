#!/usr/bin/env python3
"""Blink check for scene `solo`: the leaf region of every frame against the last frame (leaf gone). The leaf may only get
less like the leaf over time (the copy covers the stale mesh and shrinks); a dip to 'gone' followed by a return is a
blink. Usage: blink.py <frames dir with times.csv>. Exit 1 on a blink."""
import csv
import sys
import pathlib
import numpy as np
from PIL import Image

d = pathlib.Path(sys.argv[1])
frames = sorted(d.glob("f*.png"))
last = np.asarray(Image.open(frames[-1]).convert("RGB"), dtype=np.int16)
h, w, _ = last.shape
box = (w // 2 - 120, h // 2 - 120, w // 2 + 120, h // 2 + 120)
series = []
for f in frames:
    a = np.asarray(Image.open(f).convert("RGB").crop(box), dtype=np.int16)
    b = last[box[1]:box[3], box[0]:box[2]]
    series.append(float(np.abs(a - b).mean()))
peak = max(series)
start = next(i for i, v in enumerate(series) if v > peak * 0.5)
worst = 0.0
for i in range(start + 1, len(series)):
    rise = series[i] - min(series[start:i])
    worst = max(worst, rise)
print("frames", len(series), "peak", round(peak, 2), "first leaf frame", start, "largest rise after a fall", round(worst, 2), f"({worst / peak:.0%} of peak)")
print(" ".join(f"{v:.0f}" for v in series[max(0, start - 3):start + 60]))
sys.exit(1 if worst > peak * 0.2 else 0)
