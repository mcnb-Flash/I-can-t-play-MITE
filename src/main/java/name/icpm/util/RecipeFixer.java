package name.icpm.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 修复所有配方格式 (result用id而非item)
 * 并添加R196风格配方
 */
public class RecipeFixer {

    static final String RECIPE_DIR = "c:\\Users\\Administrator\\Desktop\\icpm-template-1.21.11\\src\\main\\resources\\data\\icpm\\recipe";

    public static void main(String[] args) throws IOException {
        // 1. 修复所有现有配方的 result 字段
        File dir = new File(RECIPE_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                fixRecipe(f);
            }
        }
        System.out.println("Fixed all existing recipes");

        // 2. 添加木质短棍配方
        writeRecipe("wood_cudgel", "{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"pattern\": [\"I\", \"S\"],\n  \"key\": {\"I\": {\"tag\": \"minecraft:planks\"}, \"S\": {\"item\": \"minecraft:stick\"}},\n  \"result\": {\"id\": \"icpm:wood_cudgel\", \"count\": 1}\n}");

        // 3. 添加合金升级锻造台配方
        addAlloyUpgradeRecipes();

        System.out.println("Done!");
    }

    static void fixRecipe(File f) throws IOException {
        String content = new String(Files.readAllBytes(f.toPath()));
        // 修复 result 字段: {"item": "xxx"} -> {"id": "xxx", "count": 1}
        content = content.replaceAll("\"result\":\\s*\\{\\s*\"item\":\\s*\"([^\"]+)\"\\s*\\}", "\"result\": {\"id\": \"$1\", \"count\": 1}");
        Files.write(f.toPath(), content.getBytes());
    }

    static void writeRecipe(String name, String content) throws IOException {
        File f = new File(RECIPE_DIR + "\\" + name + ".json");
        f.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }

    static void addAlloyUpgradeRecipes() throws IOException {
        // 合金升级模板 (用艾德曼锭+下界合金锭合成)
        writeRecipe("alloy_upgrade_template", "{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"pattern\": [\" A \", \"ADA\", \" A \"],\n  \"key\": {\"A\": {\"item\": \"icpm:adamantium_ingot\"}, \"D\": {\"item\": \"minecraft:netherite_ingot\"}},\n  \"result\": {\"id\": \"icpm:alloy_upgrade_template\", \"count\": 1}\n}");

        // 艾德曼装备 -> 合金装备 (需要艾德曼装备+下界合金锭+模板)
        String[] equipment = {"pickaxe", "shovel", "axe", "hoe", "sword", "helmet", "chestplate", "leggings", "boots"};
        for (String eq : equipment) {
            writeRecipe("alloy_" + eq + "_from_adamantium", "{\n  \"type\": \"minecraft:smithing_transform\",\n  \"template\": {\"item\": \"icpm:alloy_upgrade_template\"},\n  \"base\": {\"item\": \"icpm:adamantium_" + eq + "\"},\n  \"addition\": {\"item\": \"minecraft:netherite_ingot\"},\n  \"result\": {\"id\": \"icpm:alloy_" + eq + "\"}\n}");
        }

        System.out.println("Added alloy upgrade recipes (adamantium only)");
    }
}
