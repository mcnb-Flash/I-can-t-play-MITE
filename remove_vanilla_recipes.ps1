#!/usr/bin/env powershell
# PowerShell script to remove iron/gold armor and tools from minecraft recipe directory

$mcRecipeDir = "C:\Users\Administrator\Desktop\mite-template-1.21.11\src\main\resources\data\minecraft\recipe"

Write-Host "Removing vanilla iron/gold/diamond/netherite tools and armor recipes..."

# Create file list
$files = Get-ChildItem "$mcRecipeDir\*.json"

# List of tools to remove
$toolsToRemove = @(
    "iron_pickaxe", "iron_axe", "iron_shovel", "iron_hoe", "iron_sword",
    "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
    "golden_pickaxe", "golden_axe", "golden_shovel", "golden_hoe", "golden_sword",
    "golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots",
    "diamond_pickaxe", "diamond_axe", "diamond_shovel", "diamond_hoe", "diamond_sword",
    "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
    "netherite_pickaxe", "netherite_axe", "netherite_shovel", "netherite_hoe", "netherite_sword",
    "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"
)

$removed = 0
$remaining = @()

foreach ($tool in $toolsToRemove) {
    $path = "$mcRecipeDir\$tool.json"
    if (Test-Path $path) {
        Remove-Item $path -Force
        $removed++
        Write-Host "Removed: $tool.json"
    } else {
        $remaining += $tool
    }
}

Write-Host ""
Write-Host "=== SUMMARY ==="
Write-Host "Total files removed: $removed"

if ($remaining.Count -gt 0) {
    Write-Host "Files not found: $($remaining -join ', ')"
} else {
    Write-Host "All vanilla iron/gold/diamond/netherite tools removed successfully"
}

# Verify remaining files
$remainingFiles = Get-ChildItem "$mcRecipeDir\*.json"
Write-Host "Remaining files: $($remainingFiles.Count)"
