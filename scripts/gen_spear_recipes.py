import json, os

recipe_dir = r"C:/Users/Administrator/Desktop/I can't play MITE/src/main/resources/data/icpm/recipe"

materials = {
    "silver": "icpm:silver_ingot",
    "ancient_metal": "icpm:ancient_metal_ingot",
    "mithril": "icpm:mithril_ingot",
    "adamantium": "icpm:adamantium_ingot",
}

for metal, ingot in materials.items():
    data = {
        "type": "minecraft:crafting_shaped",
        "pattern": ["I", "I", "S"],
        "key": {"I": ingot, "S": "minecraft:stick"},
        "result": {"id": f"icpm:{metal}_spear", "count": 1},
    }
    with open(os.path.join(recipe_dir, f"{metal}_spear.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

upgrade = {
    "addition": "minecraft:netherite_ingot",
    "template": "minecraft:netherite_upgrade_smithing_template",
    "result": {"id": "minecraft:netherite_spear", "count": 1},
    "type": "minecraft:smithing_transform",
    "base": "icpm:adamantium_spear",
}
with open(os.path.join(recipe_dir, "netherite_spear_from_adamantium.json"), "w", encoding="utf-8") as f:
    json.dump(upgrade, f, ensure_ascii=False, indent=4)

print("recipes regenerated")
