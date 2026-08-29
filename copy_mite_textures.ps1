$srcBase = "E:\MITE Resource Pack 1.6.41\assets\minecraft\textures"
$targetItem = "c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\item"
$targetBlock = "c:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\assets\mite\textures\block"

# 用字符串行格式 "name|src"
$itemCopyList = @(
    "flint_fragment|items\shards\flint.png",
    "flint_knife|items\tools\flint_knife.png",
    "flint_shovel|items\tools\flint_shovel.png",
    "flint_hatchet|items\tools\flint_hatchet.png",
    "flint_axe|items\tools\flint_axe.png",
    "silver_nugget|items\nuggets\silver.png",
    "silver_ingot|items\ingots\silver.png",
    "silver_pickaxe|items\tools\silver_pickaxe.png",
    "silver_shovel|items\tools\silver_shovel.png",
    "silver_axe|items\tools\silver_axe.png",
    "silver_hoe|items\tools\silver_hoe.png",
    "silver_sword|items\tools\silver_sword.png",
    "silver_hatchet|items\tools\silver_hatchet.png",
    "silver_dagger|items\tools\silver_dagger.png",
    "silver_knife|items\tools\silver_knife.png",
    "silver_war_hammer|items\tools\silver_war_hammer.png",
    "silver_battle_axe|items\tools\silver_battle_axe.png",
    "silver_scythe|items\tools\silver_scythe.png",
    "silver_mattock|items\tools\silver_mattock.png",
    "ancient_metal_nugget|items\nuggets\ancient_metal.png",
    "ancient_metal_ingot|items\ingots\ancient_metal.png",
    "ancient_metal_pickaxe|items\tools\ancient_metal_pickaxe.png",
    "ancient_metal_shovel|items\tools\ancient_metal_shovel.png",
    "ancient_metal_axe|items\tools\ancient_metal_axe.png",
    "ancient_metal_hoe|items\tools\ancient_metal_hoe.png",
    "ancient_metal_sword|items\tools\ancient_metal_sword.png",
    "ancient_metal_hatchet|items\tools\ancient_metal_hatchet.png",
    "ancient_metal_dagger|items\tools\ancient_metal_dagger.png",
    "ancient_metal_knife|items\tools\ancient_metal_knife.png",
    "ancient_metal_war_hammer|items\tools\ancient_metal_war_hammer.png",
    "ancient_metal_battle_axe|items\tools\ancient_metal_battle_axe.png",
    "ancient_metal_scythe|items\tools\ancient_metal_scythe.png",
    "ancient_metal_mattock|items\tools\ancient_metal_mattock.png",
    "mithril_nugget|items\nuggets\mithril.png",
    "mithril_ingot|items\ingots\mithril.png",
    "mithril_pickaxe|items\tools\mithril_pickaxe.png",
    "mithril_shovel|items\tools\mithril_shovel.png",
    "mithril_axe|items\tools\mithril_axe.png",
    "mithril_hoe|items\tools\mithril_hoe.png",
    "mithril_sword|items\tools\mithril_sword.png",
    "mithril_hatchet|items\tools\mithril_hatchet.png",
    "mithril_dagger|items\tools\mithril_dagger.png",
    "mithril_knife|items\tools\mithril_knife.png",
    "mithril_war_hammer|items\tools\mithril_war_hammer.png",
    "mithril_battle_axe|items\tools\mithril_battle_axe.png",
    "mithril_scythe|items\tools\mithril_scythe.png",
    "mithril_mattock|items\tools\mithril_mattock.png",
    "adamantium_nugget|items\nuggets\adamantium.png",
    "adamantium_ingot|items\ingots\adamantium.png",
    "adamantium_pickaxe|items\tools\adamantium_pickaxe.png",
    "adamantium_shovel|items\tools\adamantium_shovel.png",
    "adamantium_axe|items\tools\adamantium_axe.png",
    "adamantium_hoe|items\tools\adamantium_hoe.png",
    "adamantium_sword|items\tools\adamantium_sword.png",
    "adamantium_hatchet|items\tools\adamantium_hatchet.png",
    "adamantium_dagger|items\tools\adamantium_dagger.png",
    "adamantium_knife|items\tools\adamantium_knife.png",
    "adamantium_war_hammer|items\tools\adamantium_war_hammer.png",
    "adamantium_battle_axe|items\tools\adamantium_battle_axe.png",
    "adamantium_scythe|items\tools\adamantium_scythe.png",
    "adamantium_mattock|items\tools\adamantium_mattock.png"
)

$blockCopyList = @(
    "copper_ore|blocks\copper_ore.png",
    "silver_ore|blocks\silver_ore.png",
    "mithril_ore|blocks\mithril_ore.png",
    "adamantium_ore|blocks\adamantium_ore.png",
    "copper_block|blocks\copper_block.png",
    "silver_block|blocks\silver_block.png",
    "ancient_metal_block|blocks\ancient_metal_block.png",
    "mithril_block|blocks\mithril_block.png",
    "adamantium_block|blocks\adamantium_block.png"
)

$copied = 0
$missing = 0
foreach ($line in $itemCopyList) {
    $parts = $line -split "\|"
    $name = $parts[0]
    $srcRel = $parts[1]
    $src = Join-Path $srcBase $srcRel
    $dst = Join-Path $targetItem "$name.png"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        $copied++
    } else {
        $missing++
        Write-Host "Missing: $name <- $srcRel"
    }
}
Write-Host "Items: Copied=$copied Missing=$missing"

$copied = 0
$missing = 0
foreach ($line in $blockCopyList) {
    $parts = $line -split "\|"
    $name = $parts[0]
    $srcRel = $parts[1]
    $src = Join-Path $srcBase $srcRel
    $dst = Join-Path $targetBlock "$name.png"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        $copied++
    } else {
        $missing++
        Write-Host "Missing: $name <- $srcRel"
    }
}
Write-Host "Blocks: Copied=$copied Missing=$missing"
