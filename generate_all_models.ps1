$targetBase = "c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite"

$items = @(
    "flint_knife", "flint_shovel", "flint_hatchet", "flint_axe", "flint_fragment",
    "silver_nugget", "silver_ingot", "silver_pickaxe", "silver_shovel", "silver_axe", "silver_hoe", "silver_sword",
    "silver_hatchet", "silver_dagger", "silver_knife", "silver_war_hammer", "silver_battle_axe", "silver_scythe", "silver_mattock", "silver_cudgel",
    "ancient_metal_nugget", "ancient_metal_ingot", "ancient_metal_pickaxe", "ancient_metal_shovel", "ancient_metal_axe", "ancient_metal_hoe", "ancient_metal_sword",
    "ancient_metal_hatchet", "ancient_metal_dagger", "ancient_metal_knife", "ancient_metal_war_hammer", "ancient_metal_battle_axe", "ancient_metal_scythe", "ancient_metal_mattock", "ancient_metal_cudgel",
    "mithril_nugget", "mithril_ingot", "mithril_pickaxe", "mithril_shovel", "mithril_axe", "mithril_hoe", "mithril_sword",
    "mithril_hatchet", "mithril_dagger", "mithril_knife", "mithril_war_hammer", "mithril_battle_axe", "mithril_scythe", "mithril_mattock", "mithril_cudgel",
    "adamantium_nugget", "adamantium_ingot", "adamantium_pickaxe", "adamantium_shovel", "adamantium_axe", "adamantium_hoe", "adamantium_sword",
    "adamantium_hatchet", "adamantium_dagger", "adamantium_knife", "adamantium_war_hammer", "adamantium_battle_axe", "adamantium_scythe", "adamantium_mattock", "adamantium_cudgel"
)

$blocks = @("copper_ore", "silver_ore", "ancient_metal_ore", "mithril_ore", "adamantium_ore",
            "copper_block", "silver_block", "ancient_metal_block", "mithril_block", "adamantium_block")

# 生成 物品模型到 models/item/ (兼容 1.21)
foreach ($item in $items) {
    if ($item -match "pickaxe|shovel|axe|hoe|sword|hatchet|battle_axe|war_hammer|dagger|scythe|mattock|knife|cudgel|hammer") {
        $parent = "minecraft:item/handheld"
    } elseif ($item -match "_bow$") {
        $parent = "minecraft:item/bow"
    } else {
        $parent = "minecraft:item/generated"
    }
    $content = "{" + [Environment]::NewLine +
    "  `"parent`": `"$parent`"," + [Environment]::NewLine +
    "  `"textures`": {" + [Environment]::NewLine +
    "    `"layer0`": `"mite:item/$item`"" + [Environment]::NewLine +
    "  }" + [Environment]::NewLine +
    "}"
    $path = "$targetBase\models\item\$item.json"
    $dir = Split-Path $path -Parent
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    Set-Content -Path $path -Value $content -Encoding UTF8
}

# 方块 blockstates + block models
foreach ($block in $blocks) {
    $blockstate = '{"variants":{"":{"model":"mite:block/' + $block + '"}}}'
    $path1 = "$targetBase\blockstates\$block.json"
    $dir1 = Split-Path $path1 -Parent
    if (!(Test-Path $dir1)) { New-Item -ItemType Directory -Force -Path $dir1 | Out-Null }
    Set-Content -Path $path1 -Value $blockstate -Encoding UTF8

    $blockmodel = '{"parent":"minecraft:block/cube_all","textures":{"all":"mite:block/' + $block + '"}}'
    $path2 = "$targetBase\models\block\$block.json"
    $dir2 = Split-Path $path2 -Parent
    if (!(Test-Path $dir2)) { New-Item -ItemType Directory -Force -Path $dir2 | Out-Null }
    Set-Content -Path $path2 -Value $blockmodel -Encoding UTF8
}

Write-Host "=== Models generated ==="
Write-Host "Items: $($items.Count), Blocks: $($blocks.Count)"
