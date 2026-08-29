import os
from PIL import Image

TEX_DIR = r"C:/Users/Administrator/Desktop/I can't play MITE/src/main/resources/assets/icpm/textures/item"

# 金属 -> 源剑纹理（用于提取主色）。netherite 无剑纹理，用 adamantium 剑作基色再调暗偏紫。
SRC = {
    "silver": "silver_sword.png",
    "ancient_metal": "ancient_metal_sword.png",
    "mithril": "mithril_sword.png",
    "adamantium": "adamantium_sword.png",
    "netherite": "adamantium_sword.png",
}

# 各金属主色（R,G,B）与暗部（用于立体感）。基于对应剑纹理提取，netherite 单独指定。
METAL_COLORS = {
    "silver":       ((200, 205, 215), (150, 156, 170)),
    "ancient_metal":((195, 175, 140), (150, 132, 100)),
    "mithril":      ((150, 210, 235), (100, 165, 200)),
    "adamantium":   ((225, 200, 150), (180, 150, 105)),
    "netherite":    ((70, 55, 75),    (35, 25, 40)),   # 深色偏紫，下界合金
}

def extract_main_color(img):
    """取非透明像素平均色，作为金属主色参考（netherite 直接用指定色）。"""
    px = img.convert("RGBA").load()
    rs = gs = bs = n = 0
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a > 20:
                rs += r; gs += g; bs += b; n += 1
    if n == 0:
        return (200, 200, 200)
    return (rs // n, gs // n, bs // n)

def lighten(c, f):
    return tuple(min(255, int(v * f)) for v in c)

def draw_spear(main, dark):
    W = H = 16
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    # 杆：x=7,8 两列，从 y=4 到 y=15
    for y in range(4, 16):
        shade = dark if (y % 2 == 0) else main
        px[7, y] = shade + (255,)
        px[8, y] = lighten(main, 1.15) + (255,)  # 高光侧
    # 尖头：y=0..4 的三角形
    tip_top = main
    for y in range(0, 5):
        half = (4 - y)  # y=0 最宽4, y=4 最窄0
        for dx in range(-half, half + 1):
            x = 7 + dx
            if x < 0 or x > 15:
                continue
            col = lighten(tip_top, 1.0 + (y == 0) * 0.1)
            px[x, y] = col + (255,)
    # 护手：y=4 横向 6..9
    for x in range(6, 10):
        px[x, 4] = dark + (255,)
    # 尖头最高处高光点
    px[7, 0] = lighten(main, 1.3) + (255,)
    px[8, 0] = lighten(main, 1.3) + (255,)
    return img

for metal, src in SRC.items():
    src_path = os.path.join(TEX_DIR, src)
    main, dark = METAL_COLORS[metal]
    if metal != "netherite" and os.path.exists(src_path):
        try:
            s = Image.open(src_path)
            avg = extract_main_color(s)
            # 用提取色微调主色明度，保持金属辨识度
            main = tuple(int(0.55 * avg[i] + 0.45 * main[i]) for i in range(3))
            dark = tuple(int(0.6 * c) for c in main)
        except Exception as e:
            print("warn", metal, e)
    out = draw_spear(main, dark)
    out_path = os.path.join(TEX_DIR, f"{metal}_spear.png")
    out.save(out_path)
    print("wrote", out_path, "main=", main)
print("done")
