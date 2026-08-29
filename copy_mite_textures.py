"""复制 MITE 1.6.41 资源包贴图到 1.21 项目"""
import os
import shutil

target_item = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\item"
target_block = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\block"
target_armor_humanoid = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\entity\equipment\humanoid"
target_armor_leggings = r"c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\entity\equipment\humanoid_leggings"
src_base = r"E:\MITE Resource Pack 1.6.41\assets\minecraft\textures"

# 物品贴图映射 (item_id -> 1.6.41资源路径)
item_map = {
    # 碎片
    "flint_fragment": "items/shards/flint.png",
    "obsidian_shard": "items/shards/obsidian.png",
    "emerald_shard": "items/shards/emerald.png",
    "diamond_shard": "items/shards/diamond.png",

    # 粒
    "copper_nugget": "items/nuggets/copper.png",
    "silver_nugget": "items/nuggets/silver.png",
    "ancient_metal_nugget": "items/nuggets/ancient_metal.png",
    "mithril_nugget": "items/nuggets/mithril.png",
    "adamantium_nugget": "items/nuggets/adamantium.png",

    # 锭
    "silver_ingot": "items/ingots/silver.png",
    "ancient_metal_ingot": "items/ingots/ancient_metal.png",
    "mithril_ingot": "items/ingots/mithril.png",
    "adamantium_ingot": "items/ingots/adamantium.png",

    # 燧石工具
    "flint_knife": "items/tools/flint_knife.png",
    "flint_shovel": "items/tools/flint_shovel.png",
    "flint_hatchet": "items/tools/flint_hatchet.png",
    "flint_axe": "items/tools/flint_axe.png",

    # 银工具
    "silver_pickaxe": "items/tools/silver_pickaxe.png",
    "silver_shovel": "items/tools/silver_shovel.png",
    "silver_axe": "items/tools/silver_axe.png",
    "silver_hoe": "items/tools/silver_hoe.png",
    "silver_sword": "items/tools/silver_sword.png",
    "silver_hatchet": "items/tools/silver_hatchet.png",
    "silver_dagger": "items/tools/silver_dagger.png",
    "silver_knife": "items/tools/silver_knife.png",
    "silver_war_hammer": "items/tools/silver_war_hammer.png",
    "silver_battle_axe": "items/tools/silver_battle_axe.png",
    "silver_scythe": "items/tools/silver_scythe.png",
    "silver_mattock": "items/tools/silver_mattock.png",
    "silver_cudgel": "items/tools/wood_cudgel.png",

    # 银盔甲
    "silver_helmet": "items/armor/silver_helmet.png",
    "silver_chestplate": "items/armor/silver_chestplate.png",
    "silver_leggings": "items/armor/silver_leggings.png",
    "silver_boots": "items/armor/silver_boots.png",

    # 远古金属工具
    "ancient_metal_pickaxe": "items/tools/ancient_metal_pickaxe.png",
    "ancient_metal_shovel": "items/tools/ancient_metal_shovel.png",
    "ancient_metal_axe": "items/tools/ancient_metal_axe.png",
    "ancient_metal_hoe": "items/tools/ancient_metal_hoe.png",
    "ancient_metal_sword": "items/tools/ancient_metal_sword.png",
    "ancient_metal_hatchet": "items/tools/ancient_metal_hatchet.png",
    "ancient_metal_dagger": "items/tools/ancient_metal_dagger.png",
    "ancient_metal_knife": "items/tools/ancient_metal_knife.png",
    "ancient_metal_war_hammer": "items/tools/ancient_metal_war_hammer.png",
    "ancient_metal_battle_axe": "items/tools/ancient_metal_battle_axe.png",
    "ancient_metal_scythe": "items/tools/ancient_metal_scythe.png",
    "ancient_metal_mattock": "items/tools/ancient_metal_mattock.png",
    "ancient_metal_cudgel": "items/tools/wood_cudgel.png",

    # 秘银工具
    "mithril_pickaxe": "items/tools/mithril_pickaxe.png",
    "mithril_shovel": "items/tools/mithril_shovel.png",
    "mithril_axe": "items/tools/mithril_axe.png",
    "mithril_hoe": "items/tools/mithril_hoe.png",
    "mithril_sword": "items/tools/mithril_sword.png",
    "mithril_hatchet": "items/tools/mithril_hatchet.png",
    "mithril_dagger": "items/tools/mithril_dagger.png",
    "mithril_knife": "items/tools/mithril_knife.png",
    "mithril_war_hammer": "items/tools/mithril_war_hammer.png",
    "mithril_battle_axe": "items/tools/mithril_battle_axe.png",
    "mithril_scythe": "items/tools/mithril_scythe.png",
    "mithril_mattock": "items/tools/mithril_mattock.png",
    "mithril_cudgel": "items/tools/wood_cudgel.png",

    # 艾德曼工具
    "adamantium_pickaxe": "items/tools/adamantium_pickaxe.png",
    "adamantium_shovel": "items/tools/adamantium_shovel.png",
    "adamantium_axe": "items/tools/adamantium_axe.png",
    "adamantium_hoe": "items/tools/adamantium_hoe.png",
    "adamantium_sword": "items/tools/adamantium_sword.png",
    "adamantium_hatchet": "items/tools/adamantium_hatchet.png",
    "adamantium_dagger": "items/tools/adamantium_dagger.png",
    "adamantium_knife": "items/tools/adamantium_knife.png",
    "adamantium_war_hammer": "items/tools/adamantium_war_hammer.png",
    "adamantium_battle_axe": "items/tools/adamantium_battle_axe.png",
    "adamantium_scythe": "items/tools/adamantium_scythe.png",
    "adamantium_mattock": "items/tools/adamantium_mattock.png",
    "adamantium_cudgel": "items/tools/wood_cudgel.png",

    # 木短棍
    "wood_cudgel": "items/tools/wood_cudgel.png",
}

# 盔甲装备层贴图映射 (1.21.2+ 装备资源格式)
# layer_1 -> textures/entity/equipment/humanoid/<material>.png
# layer_2 -> textures/entity/equipment/humanoid_leggings/<material>.png
armor_layer_1_map = {
    "silver": "models/armor/silver_layer_1.png",
    "ancient_metal": "models/armor/ancient_metal_layer_1.png",
    "mithril": "models/armor/mithril_layer_1.png",
    "adamantium": "models/armor/adamantium_layer_1.png",
}
armor_layer_2_map = {
    "silver": "models/armor/silver_layer_2.png",
    "ancient_metal": "models/armor/ancient_metal_layer_2.png",
    "mithril": "models/armor/mithril_layer_2.png",
    "adamantium": "models/armor/adamantium_layer_2.png",
}

# 方块贴图映射
block_map = {
    "copper_ore": "blocks/copper_ore.png",
    "silver_ore": "blocks/silver_ore.png",
    "mithril_ore": "blocks/mithril_ore.png",
    "adamantium_ore": "blocks/adamantium_ore.png",
    "copper_block": "blocks/copper_block.png",
    "silver_block": "blocks/silver_block.png",
    "ancient_metal_block": "blocks/ancient_metal_block.png",
    "mithril_block": "blocks/mithril_block.png",
    "adamantium_block": "blocks/adamantium_block.png",
    "mantle": "blocks/mantle.png",
}

def copy_files(mapping, target_dir, prefix=""):
    copied = 0
    missing = []
    for name, src_rel in mapping.items():
        src = os.path.join(src_base, src_rel)
        dst = os.path.join(target_dir, f"{name}.png")
        if os.path.exists(src):
            shutil.copy2(src, dst)
            copied += 1
        else:
            missing.append(f"{name} <- {src_rel}")
    print(f"  Copied: {copied}, Missing: {len(missing)}")
    for m in missing:
        print(f"    MISSING: {m}")

print("=== Copying item textures ===")
copy_files(item_map, target_item)

print("=== Copying block textures ===")
copy_files(block_map, target_block)

print("=== Copying armor layer 1 textures (humanoid) ===")
copy_files(armor_layer_1_map, target_armor_humanoid)

print("=== Copying armor layer 2 textures (humanoid_leggings) ===")
copy_files(armor_layer_2_map, target_armor_leggings)

print("=== Done ===")
