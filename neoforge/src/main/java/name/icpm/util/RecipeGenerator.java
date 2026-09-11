package name.icpm.util;

import java.io.*;
import java.nio.file.*;

/**
 * 生成所有ICPM配方 (1.21.11格式: 材料用平铺字符串, result用id)
 * 包含: R196风格工具合成 + 盔甲 + 禁用原版钻石/石质/木质工具与装备
 *
 * 注意: 此工具会清空并重新生成 data/icpm/recipe 下所有配方。
 * 用户已手动修改过的合成表(flint_axe, flint_hatchet, silver_mattock, adamantium_hatchet)
 * 不应再运行此生成器覆盖，如需重新生成请先备份。
 */
public class RecipeGenerator {

    static final String RECIPE_DIR = "c:\\Users\\Administrator\\Desktop\\icpm-template-1.21.11\\src\\main\\resources\\data\\icpm\\recipe";

    public static void main(String[] args) throws IOException {
        // 清空旧配方
        File dir = new File(RECIPE_DIR);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".json")) f.delete();
            }
        }
        dir.mkdirs();

        // ===== 2. 燧石系配方 =====
        flintRecipes();

        // ===== 3. 木质短棍 =====
        writeRecipe("wood_cudgel", shaped(
            new String[]{"P", "S"},
            key('P', tag("minecraft:planks"), 'S', item("minecraft:stick")),
            result("icpm:wood_cudgel", 1)
        ));

        // ===== 4. 金属锭/粒互转 =====
        String[] mats = {"silver", "ancient_metal", "mithril", "adamantium"};
        for (String m : mats) {
            // 9粒 -> 1锭
            writeRecipe(m + "_ingot", shaped(
                new String[]{"NNN", "NNN", "NNN"},
                key('N', item("icpm:" + m + "_nugget")),
                result("icpm:" + m + "_ingot", 1)
            ));
            // 1锭 -> 9粒
            writeRecipe(m + "_nugget", shapeless(
                new String[]{item("icpm:" + m + "_ingot")},
                result("icpm:" + m + "_nugget", 9)
            ));
        }

        // ===== 5. 金属工具/盔甲配方 (R196风格) =====
        for (String m : mats) {
            metalToolRecipes(m);
            metalArmorRecipes(m);
        }

        // ===== 6. 禁用原版钻石/石质/木质工具与装备合成 =====
        disableVanillaRecipes();

        System.out.println("Generated " + new File(RECIPE_DIR).listFiles((d, n) -> n.endsWith(".json")).length + " recipes");
        System.out.println("Done!");
    }

    static void disableVanillaRecipes() throws IOException {
        // 在minecraft命名空间下创建同名配方文件来覆盖原版
        String mcRecipeDir = "c:\\Users\\Administrator\\Desktop\\icpm-template-1.21.11\\src\\main\\resources\\data\\minecraft\\recipe";
        new File(mcRecipeDir).mkdirs();

        // 禁用原版合成 (用不可达成的配方覆盖: 需要基岩, 生存无法获得)
        String[] disabled = {
            "wooden_pickaxe", "wooden_axe", "wooden_shovel", "wooden_hoe",
            "stone_pickaxe", "stone_axe", "stone_shovel", "stone_hoe", "stone_sword",
            "diamond_pickaxe", "diamond_axe", "diamond_shovel", "diamond_hoe", "diamond_sword",
            "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"
        };
        String impossible = "{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"pattern\": [\"A\"],\n  \"key\": {\"A\": \"minecraft:bedrock\"},\n  \"result\": {\"id\": \"minecraft:bedrock\", \"count\": 1}\n}";
        for (String r : disabled) {
            writeFile(mcRecipeDir + "\\" + r + ".json", impossible);
        }

        System.out.println("Disabled " + disabled.length + " vanilla recipes");
    }

    static void flintRecipes() throws IOException {
        String cord = tag("icpm:cords");
        String fragment = item("icpm:flint_fragment");
        String flint = item("minecraft:flint");
        String stick = item("minecraft:stick");

        // 4 燧石碎片 -> 1 完整燧石 (ICPM原版)
        writeRecipe("flint_from_fragments", shapeless(
            new String[]{fragment, fragment, fragment, fragment},
            result("minecraft:flint", 1)
        ));

        // 燧石工具使用完整燧石 + 木棍 + 线/皮革线
        // 燧石小刀 (无序)
        writeRecipe("flint_knife", shapeless(
            new String[]{flint, stick, cord},
            result("icpm:flint_knife", 1)
        ));
        // 黑曜石小刀 (黑曜石碎片+木棍)
        writeRecipe("obsidian_knife", shaped(
            new String[]{"O", "S"},
            key('O', item("icpm:obsidian_shard"), 'S', stick),
            result("icpm:obsidian_knife", 1)
        ));
        // 燧石锹
        writeRecipe("flint_shovel", shaped(
            new String[]{"F", "S", "C"},
            key('F', flint, 'S', stick, 'C', cord),
            result("icpm:flint_shovel", 1)
        ));
        // 燧石短斧
        writeRecipe("flint_hatchet", shaped(
            new String[]{"FS", "CS"},
            key('F', flint, 'S', stick, 'C', cord),
            result("icpm:flint_hatchet", 1)
        ));
        // 燧石斧
        writeRecipe("flint_axe", shaped(
            new String[]{"FF", "FS", "CS"},
            key('F', flint, 'S', stick, 'C', cord),
            result("icpm:flint_axe", 1)
        ));
    }

    static void metalToolRecipes(String mat) throws IOException {
        String I = item("icpm:" + mat + "_ingot");
        String S = item("minecraft:stick");

        // 镐 (3锭+2棍)
        writeRecipe(mat + "_pickaxe", shaped(
            new String[]{"III", " S ", " S "},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_pickaxe", 1)
        ));
        // 锹 (1锭+2棍)
        writeRecipe(mat + "_shovel", shaped(
            new String[]{"I", "S", "S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_shovel", 1)
        ));
        // 斧 (3锭+2棍)
        writeRecipe(mat + "_axe", shaped(
            new String[]{"II", "IS", " S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_axe", 1)
        ));
        // 锄 (2锭+2棍)
        writeRecipe(mat + "_hoe", shaped(
            new String[]{"II", " S", " S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_hoe", 1)
        ));
        // 剑 (2锭+1棍)
        writeRecipe(mat + "_sword", shaped(
            new String[]{"I", "I", "S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_sword", 1)
        ));
        // 短斧 (1锭+1棍)
        writeRecipe(mat + "_hatchet", shaped(
            new String[]{"I", "S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_hatchet", 1)
        ));
        // 匕首 (1锭+1棍)
        writeRecipe(mat + "_dagger", shaped(
            new String[]{"I", "S"},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_dagger", 1)
        ));
        // 战锤 (3锭+2棍, 特殊排列)
        writeRecipe(mat + "_war_hammer", shaped(
            new String[]{"I I", " S ", " S "},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_war_hammer", 1)
        ));
        // 战斧 (3锭+2棍, 特殊排列)
        writeRecipe(mat + "_battle_axe", shaped(
            new String[]{"I I", "ISI", " S "},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_battle_axe", 1)
        ));
        // 镰刀 (2锭+2棍, 特殊排列)
        writeRecipe(mat + "_scythe", shaped(
            new String[]{" II", "  S", " S "},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_scythe", 1)
        ));
        // 鸭嘴锄 (3锭+2棍, 特殊排列)
        writeRecipe(mat + "_mattock", shaped(
            new String[]{"I I", " S ", " S "},
            key('I', I, 'S', S),
            result("icpm:" + mat + "_mattock", 1)
        ));
    }

    static void metalArmorRecipes(String mat) throws IOException {
        String I = item("icpm:" + mat + "_ingot");

        // 靴子 (4锭)
        writeRecipe(mat + "_boots", shaped(
            new String[]{"I I", "I I"},
            key('I', I),
            result("icpm:" + mat + "_boots", 1)
        ));
        // 头盔 (5锭)
        writeRecipe(mat + "_helmet", shaped(
            new String[]{"III", "I I"},
            key('I', I),
            result("icpm:" + mat + "_helmet", 1)
        ));
        // 胸甲 (8锭)
        writeRecipe(mat + "_chestplate", shaped(
            new String[]{"I I", "III", "III"},
            key('I', I),
            result("icpm:" + mat + "_chestplate", 1)
        ));
        // 护腿 (7锭)
        writeRecipe(mat + "_leggings", shaped(
            new String[]{"III", "I I", "I I"},
            key('I', I),
            result("icpm:" + mat + "_leggings", 1)
        ));
    }

    // ===== JSON构建工具 =====

    static String item(String id) { return "\"" + id + "\""; }
    static String tag(String id) { return "\"#" + id + "\""; }
    static String result(String id, int count) {
        return "{\"id\": \"" + id + "\", \"count\": " + count + "}";
    }

    static String key(char k1, String v1) {
        return "{\"" + k1 + "\": " + v1 + "}";
    }

    static String key(char k1, String v1, char k2, String v2) {
        return "{\"" + k1 + "\": " + v1 + ", \"" + k2 + "\": " + v2 + "}";
    }

    static String key(char k1, String v1, char k2, String v2, char k3, String v3) {
        return "{\"" + k1 + "\": " + v1 + ", \"" + k2 + "\": " + v2 + ", \"" + k3 + "\": " + v3 + "}";
    }

    static String shaped(String[] pattern, String key, String result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"type\": \"minecraft:crafting_shaped\",\n");
        sb.append("  \"pattern\": [");
        for (int i = 0; i < pattern.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(pattern[i]).append("\"");
        }
        sb.append("],\n");
        sb.append("  \"key\": ").append(key).append(",\n");
        sb.append("  \"result\": ").append(result).append("\n}");
        return sb.toString();
    }

    static String shapeless(String[] ingredients, String result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"type\": \"minecraft:crafting_shapeless\",\n");
        sb.append("  \"ingredients\": [");
        for (int i = 0; i < ingredients.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(ingredients[i]);
        }
        sb.append("],\n");
        sb.append("  \"result\": ").append(result).append("\n}");
        return sb.toString();
    }

    static void writeRecipe(String name, String content) throws IOException {
        writeFile(RECIPE_DIR + "\\" + name + ".json", content);
    }

    static void writeFile(String path, String content) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }
}
