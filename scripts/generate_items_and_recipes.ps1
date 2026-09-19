# 为所有MITE物品生成 1.21.4+ 所需的 items/*.json 客户端物品模型定义文件
# 同时生成所有合成表 recipe/*.json

$base = "c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite"
$itemsDir = Join-Path $base "items"
$recipeDir = "c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\data\mite\recipe"

if (-not (Test-Path $itemsDir)) { New-Item -ItemType Directory -Path $itemsDir -Force | Out-Null }
if (-not (Test-Path $recipeDir)) { New-Item -ItemType Directory -Path $recipeDir -Force | Out-Null }

# 工具类物品 (handheld)
$handheldItems = @(
    "flint_knife", "flint_shovel", "flint_hatchet", "flint_axe",
    "silver_pickaxe", "silver_shovel", "silver_axe", "silver_hoe", "silver_sword",
    "silver_hatchet", "silver_dagger", "silver_knife", "silver_war_hammer",
    "silver_battle_axe", "silver_scythe", "silver_mattock", "silver_cudgel",
    "ancient_metal_pickaxe", "ancient_metal_shovel", "ancient_metal_axe", "ancient_metal_hoe",
    "ancient_metal_sword", "ancient_metal_hatchet", "ancient_metal_dagger", "ancient_metal_knife",
    "ancient_metal_war_hammer", "ancient_metal_battle_axe", "ancient_metal_scythe", "ancient_metal_mattock",
    "mithril_pickaxe", "mithril_shovel", "mithril_axe", "mithril_hoe", "mithril_sword",
    "mithril_hatchet", "mithril_dagger", "mithril_knife", "mithril_war_hammer",
    "mithril_battle_axe", "mithril_scythe", "mithril_mattock", "mithril_cudgel",
    "adamantium_pickaxe", "adamantium_shovel", "adamantium_axe", "adamantium_hoe", "adamantium_sword",
    "adamantium_hatchet", "adamantium_dagger", "adamantium_knife", "adamantium_war_hammer",
    "adamantium_battle_axe", "adamantium_scythe", "adamantium_mattock", "adamantium_cudgel"
)

# 普通物品 (generated)
$generatedItems = @(
    "flint_fragment",
    "silver_nugget", "silver_ingot",
    "ancient_metal_nugget", "ancient_metal_ingot",
    "mithril_nugget", "mithril_ingot",
    "adamantium_nugget", "adamantium_ingot"
)

$allItems = $handheldItems + $generatedItems

# 1. 生成 items/*.json (客户端物品模型定义)
foreach ($itemId in $allItems) {
    $json = @"
{
  "model": {
    "type": "minecraft:model",
    "model": "mite:item/$itemId"
  }
}
"@
    $path = Join-Path $itemsDir "$itemId.json"
    Set-Content -Path $path -Value $json -Encoding UTF8
}
Write-Host "Generated $($allItems.Count) items/*.json files"

# 2. 生成合成表 recipe/*.json
$materials = @{
    "silver" = "silver_ingot"
    "ancient_metal" = "ancient_metal_ingot"
    "mithril" = "mithril_ingot"
    "adamantium" = "adamantium_ingot"
}

$stick = '{"item": "minecraft:stick"}'

function Write-Recipe($name, $json) {
    $path = Join-Path $recipeDir "$name.json"
    Set-Content -Path $path -Value $json -Encoding UTF8
}

# 燧石碎片 -> 燧石工具
Write-Recipe "flint_knife" @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {"item": "mite:flint_fragment"}
  ],
  "result": {"item": "mite:flint_knife"}
}
"@

Write-Recipe "flint_shovel" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["F", "S", "S"],
  "key": {"F": {"item": "mite:flint_fragment"}, "S": $stick},
  "result": {"item": "mite:flint_shovel"}
}
"@

Write-Recipe "flint_hatchet" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["F", "S"],
  "key": {"F": {"item": "mite:flint_fragment"}, "S": $stick},
  "result": {"item": "mite:flint_hatchet"}
}
"@

Write-Recipe "flint_axe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["FF", "FS", " S"],
  "key": {"F": {"item": "mite:flint_fragment"}, "S": $stick},
  "result": {"item": "mite:flint_axe"}
}
"@

# 金属工具配方
foreach ($mat in $materials.Keys) {
    $ingot = $materials[$mat]
    $ingotItem = "{`"item`": `"mite:$ingot`"}"
    $nuggetItem = "{`"item`": `"mite:${mat}_nugget`"}"

    # 粒 -> 锭 (9粒=1锭)
    Write-Recipe "${mat}_ingot" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["NNN", "NNN", "NNN"],
  "key": {"N": $nuggetItem},
  "result": {"item": "mite:$ingot"}
}
"@

    # 锭 -> 粒 (1锭=9粒)
    Write-Recipe "${mat}_nugget" @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [{"item": "mite:$ingot"}],
  "result": {"item": "mite:${mat}_nugget"}
}
"@

    # 镐
    Write-Recipe "${mat}_pickaxe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", " S ", " S "],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_pickaxe"}
}
"@

    # 锹
    Write-Recipe "${mat}_shovel" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_shovel"}
}
"@

    # 斧
    Write-Recipe "${mat}_axe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", "IS", " S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_axe"}
}
"@

    # 锄
    Write-Recipe "${mat}_hoe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["II", " S", " S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_hoe"}
}
"@

    # 剑
    Write-Recipe "${mat}_sword" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "I", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_sword"}
}
"@

    # 短斧
    Write-Recipe "${mat}_hatchet" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_hatchet"}
}
"@

    # 匕首
    Write-Recipe "${mat}_dagger" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_dagger"}
}
"@

    # 小刀
    Write-Recipe "${mat}_knife" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_knife"}
}
"@

    # 战锤
    Write-Recipe "${mat}_war_hammer" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I I", " S ", " S "],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_war_hammer"}
}
"@

    # 战斧
    Write-Recipe "${mat}_battle_axe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I I", "ISI", " S "],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_battle_axe"}
}
"@

    # 镰刀
    Write-Recipe "${mat}_scythe" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": [" II", "  S", " S "],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_scythe"}
}
"@

    # 鸭嘴锄
    Write-Recipe "${mat}_mattock" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I I", "ISI", " S "],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_mattock"}
}
"@

    # 短棍
    Write-Recipe "${mat}_cudgel" @"
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["I", "S"],
  "key": {"I": $ingotItem, "S": $stick},
  "result": {"item": "mite:${mat}_cudgel"}
}
"@
}

Write-Host "Generated recipes for $($materials.Count) materials"
Write-Host "Done!"
