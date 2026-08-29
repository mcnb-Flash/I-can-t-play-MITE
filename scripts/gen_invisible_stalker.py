import os
from PIL import Image

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "icpm", "textures", "entity")
src = os.path.join(BASE, "wight.png")
dst = os.path.join(BASE, "invisible_stalker.png")

OPACITY = 0.05  # R196 RenderInvisibleStalker.getModelOpacity() = 0.05

im = Image.open(src).convert("RGBA")
r, g, b, a = im.split()
import PIL.Image as _I
# multiply alpha by OPACITY, keep fully-transparent pixels transparent
a_new = a.point(lambda v: 0 if v == 0 else max(1, int(round(v * OPACITY))))
im.putalpha(a_new)
im.save(dst)
print("wrote", os.path.abspath(dst), "size", im.size)
# report coverage: count non-transparent pixels and their avg alpha
px = list(im.getdata())
visible = [p for p in px if p[3] > 0]
avg_alpha = sum(p[3] for p in visible) / max(1, len(visible))
print("visible_pixels", len(visible), "avg_alpha", round(avg_alpha, 2))
