import os
from PIL import Image

ROOT = r"C:/Users/Administrator/Desktop/I can't play MITE"
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/icpm/textures/item")
MC_TEX = os.path.join(ROOT, "assets/minecraft/textures/item")

# 造型模板：原版下界合金长矛（保留其 alpha 形状与光影）
TEMPLATE = os.path.join(MC_TEX, "netherite_spear.png")

# 各金属长矛目标主色（按现有同金属工具纹理主色对比，用户指定配色方案）
# silver=银白色, ancient_metal=白绿灰, adamantium=紫青黑
METALS = {
    "silver":        (200, 200, 206),   # 银白（参照 silver_ingot/door 银白偏蓝）
    "ancient_metal": (118, 124, 108),   # 白绿灰（参照 ancient_metal_sword 偏绿）
    "adamantium":    (82, 74, 116),     # 紫青黑（参照 adamantium_sword/pickaxe 紫青）
}


def rgb_to_hsv(r, g, b):
    r, g, b = r / 255.0, g / 255.0, b / 255.0
    mx, mn = max(r, g, b), min(r, g, b)
    df = mx - mn
    h = 0.0
    if df != 0:
        if mx == r:
            h = (60 * ((g - b) / df) + 360) % 360
        elif mx == g:
            h = (60 * ((b - r) / df) + 120) % 360
        else:
            h = (60 * ((r - g) / df) + 240) % 360
    s = 0 if mx == 0 else df / mx
    return h, s, mx


def hsv_to_rgb(h, s, v):
    c = v * s
    x = c * (1 - abs((h / 60) % 2 - 1))
    m = v - c
    if h < 60:
        r, g, b = c, x, 0
    elif h < 120:
        r, g, b = x, c, 0
    elif h < 180:
        r, g, b = 0, c, x
    elif h < 240:
        r, g, b = 0, x, c
    elif h < 300:
        r, g, b = x, 0, c
    else:
        r, g, b = c, 0, x
    return int((r + m) * 255), int((g + m) * 255), int((b + m) * 255)


def extract_metal_color(sword_path):
    img = Image.open(sword_path).convert("RGBA")
    w, h = img.size
    px = img.load()
    rs = gs = bs = n = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 20:
                continue
            _, s, v = rgb_to_hsv(r, g, b)
            # 取偏亮、饱和的像素作为金属主色，避免暗部/阴影拉偏
            if v < 0.12 or s < 0.08:
                continue
            rs += r; gs += g; bs += b; n += 1
    if n == 0:
        return (210, 210, 210)
    return (rs // n, gs // n, bs // n)


template = Image.open(TEMPLATE).convert("RGBA")
tw, th = template.size
tpx = template.load()

# 估算模板金属基础亮度（中高调非透明像素）
base_vs = []
for y in range(th):
    for x in range(tw):
        r, g, b, a = tpx[x, y]
        if a >= 20:
            _, _, v = rgb_to_hsv(r, g, b)
            base_vs.append(v)
base_v = sum(base_vs) / max(len(base_vs), 1)

for metal, target in METALS.items():
    th_, ts_, tv_ = rgb_to_hsv(*target)
    out = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    opx = out.load()
    for y in range(th):
        for x in range(tw):
            r, g, b, a = tpx[x, y]
            if a < 10:
                continue
            _, _, v = rgb_to_hsv(r, g, b)
            # 相对模板基础亮度的比例，保留光影
            k = v / base_v if base_v > 0 else 1.0
            k = max(0.25, min(1.6, k))
            new_v = min(1.0, tv_ * k)
            # 暗部降低饱和，高光略增亮
            new_s = ts_ * (0.6 + 0.4 * min(k, 1.0))
            nr, ng, nb = hsv_to_rgb(th_, new_s, new_v)
            opx[x, y] = (nr, ng, nb, a)
    out_path = os.path.join(TEX_DIR, f"{metal}_spear.png")
    out.save(out_path)
    print(f"wrote {metal}_spear.png  target={target} base_v={base_v:.3f}")

print("done")
