"""
为所有MITE物品生成 1.21.4+ 所需的 items/*.json 客户端物品模型定义文件
同时生成所有合成表 recipe/*.json
"""
import os
import json

base = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite"
items_dir = os.path.join(base, "items")
recipe_dir = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\data\mite\recipe"

os.makedirs(items_dir, exist_ok=True)
os.makedirs(recipe_dir, exist_ok=True)

# 工具类物品 (handheld)
handheld_items = [
    # 燧石系
    "flint_knife", "flint_shovel", "flint_hatchet", "flint_axe",
    # 银系
    "silver_pickaxe", "silver_shovel", "silver_axe", "silver_hoe", "silver_sword",
    "silver_hatchet", "silver_dagger", "silver_knife", "silver_war_hammer",
    "silver_battle_axe", "silver_scythe", "silver_mattock", "silver_cudgel",
    # 远古金属系
    "ancient_metal_pickaxe", "ancient_metal_shovel", "ancient_metal_axe", "ancient_metal_hoe",
    "ancient_metal_sword", "ancient_metal_hatchet", "ancient_metal_dagger", "ancient_metal_knife",
    "ancient_metal_war_hammer", "ancient_metal_battle_axe", "ancient_metal_scythe", "ancient_metal_mattock",
    # 秘银系
    "mithril_pickaxe", "mithril_shovel", "mithril_axe", "mithril_hoe", "mithril_sword",
    "mithril_hatchet", "mithril_dagger", "mithril_knife", "mithril_war_hammer",
    "mithril_battle_axe", "mithril_scythe", "mithril_mattock", "mithril_cudgel",
    # 艾德曼系
    "adamantium_pickaxe", "adamantium_shovel", "adamantium_axe", "adamantium_hoe", "adamantium_sword",
    "adamantium_hatchet", "adamantium_dagger", "adamantium_knife", "adamantium_war_hammer",
    "adamantium_battle_axe", "adamantium_scythe", "adamantium_mattock", "adamantium_cudgel",
]

# 普通物品 (generated)
generated_items = [
    "flint_fragment",
    "silver_nugget", "silver_ingot",
    "ancient_metal_nugget", "ancient_metal_ingot",
    "mithril_nugget", "mithril_ingot",
    "adamantium_nugget", "adamantium_ingot",
]

all_items = handheld_items + generated_items

# 1. 生成 items/*.json (客户端物品模型定义)
for item_id in all_items:
    item_json = {
        "model": {
            "type": "minecraft:model",
            "model": f"mite:item/{item_id}"
        }
    }
    path = os.path.join(items_dir, f"{item_id}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(item_json, f, indent=2)

print(f"Generated {len(all_items)} items/*.json files")

# 2. 生成合成表 recipe/*.json
# 材料映射
material_map = {
    "silver": "silver_ingot",
    "ancient_metal": "ancient_metal_ingot",
    "mithril": "mithril_ingot",
    "adamantium": "adamantium_ingot",
}

# 工具配方 (3x3 合成台)
# 镐: 3锭 + 2棍
# 锹: 1锭 + 2棍
# 斧: 3锭 + 2棍
# 锄: 2锭 + 2棍
# 剑: 2锭 + 1棍
# 短斧: 1锭 + 1棍
# 匕首: 1锭 + 1棍
# 小刀: 1锭 + 1棍
# 战锤: 3锭 + 2棍 (特殊排列)
# 战斧: 3锭 + 2棍 (特殊排列)
# 镰刀: 2锭 + 2棍 (特殊排列)
# 鸭嘴锄: 3锭 + 2棍 (特殊排列)
# 短棍: 1锭 + 1棍

stick = {"item": "minecraft:stick"}

def make_shaped(pattern, key, result):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"item": f"mite:{result}"}
    }

def make_shapeless(ingredients, result):
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ingredients,
        "result": {"item": f"mite:{result}"}
    }

def write_recipe(name, recipe):
    path = os.path.join(recipe_dir, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(recipe, f, indent=2)

# 燧石碎片 -> 燧石小刀/锹/短斧/斧
write_recipe("flint_knife", make_shapeless(
    [{"item": "mite:flint_fragment"}],
    "flint_knife"
))
write_recipe("flint_shovel", make_shaped(
    ["F", "S", "S"],
    {"F": {"item": "mite:flint_fragment"}, "S": stick},
    "flint_shovel"
))
write_recipe("flint_hatchet", make_shaped(
    ["F", "S"],
    {"F": {"item": "mite:flint_fragment"}, "S": stick},
    "flint_hatchet"
))
write_recipe("flint_axe", make_shaped(
    ["FF", "FS", " S"],
    {"F": {"item": "mite:flint_fragment"}, "S": stick},
    "flint_axe"
))

# 金属工具配方
for mat, ingot in material_map.items():
    ingot_item = {"item": f"mite:{ingot}"}
    
    # 粒 -> 锭 (9粒=1锭)
    write_recipe(f"{mat}_ingot", make_shaped(
        ["NNN", "NNN", "NNN"],
        {"N": {"item": f"mite:{mat}_nugget"}},
        f"{mat}_ingot"
    ))
    
    # 锭 -> 粒 (1锭=9粒)
    write_recipe(f"{mat}_nugget", make_shapeless(
        [{"item": f"mite:{mat}_ingot"}],
        f"{mat}_nugget"
    ))
    
    # 镐
    write_recipe(f"{mat}_pickaxe", make_shaped(
        ["III", " S ", " S "],
        {"I": ingot_item, "S": stick},
        f"{mat}_pickaxe"
    ))
    
    # 锹
    write_recipe(f"{mat}_shovel", make_shaped(
        ["I", "S", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_shovel"
    ))
    
    # 斧
    write_recipe(f"{mat}_axe", make_shaped(
        ["II", "IS", " S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_axe"
    ))
    
    # 锄
    write_recipe(f"{mat}_hoe", make_shaped(
        ["II", " S", " S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_hoe"
    ))
    
    # 剑
    write_recipe(f"{mat}_sword", make_shaped(
        ["I", "I", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_sword"
    ))
    
    # 短斧 (1锭+1棍)
    write_recipe(f"{mat}_hatchet", make_shaped(
        ["I", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_hatchet"
    ))
    
    # 匕首 (1锭+1棍)
    write_recipe(f"{mat}_dagger", make_shaped(
        ["I", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_dagger"
    ))
    
    # 小刀 (1锭+1棍)
    write_recipe(f"{mat}_knife", make_shaped(
        ["I", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_knife"
    ))
    
    # 战锤 (3锭+2棍, 特殊排列)
    write_recipe(f"{mat}_war_hammer", make_shaped(
        ["I I", " S ", " S "],
        {"I": ingot_item, "S": stick},
        f"{mat}_war_hammer"
    ))
    
    # 战斧 (3锭+2棍, 特殊排列)
    write_recipe(f"{mat}_battle_axe", make_shaped(
        ["I I", "ISI", " S "],
        {"I": ingot_item, "S": stick},
        f"{mat}_battle_axe"
    ))
    
    # 镰刀 (2锭+2棍, 特殊排列)
    write_recipe(f"{mat}_scythe", make_shaped(
        [" II", "  S", " S "],
        {"I": ingot_item, "S": stick},
        f"{mat}_scythe"
    ))
    
    # 鸭嘴锄 (3锭+2棍, 特殊排列)
    write_recipe(f"{mat}_mattock", make_shaped(
        ["I I", "ISI", " S "],
        {"I": ingot_item, "S": stick},
        f"{mat}_mattock"
    ))
    
    # 短棍 (1锭+1棍)
    write_recipe(f"{mat}_cudgel", make_shaped(
        ["I", "S"],
        {"I": ingot_item, "S": stick},
        f"{mat}_cudgel"
    ))

print(f"Generated recipes for {len(material_map)} materials")
print("Done!")
