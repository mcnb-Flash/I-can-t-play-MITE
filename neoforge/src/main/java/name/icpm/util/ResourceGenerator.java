package name.icpm.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 生成 1.21.4+ 所需的 items/*.json 客户端物品模型定义文件
 * 以及所有合成表 recipe/*.json
 */
public class ResourceGenerator {

    static final String BASE = "c:\\Users\\Administrator\\Desktop\\icpm-template-1.21.11\\src\\main\\resources";
    static final String ASSETS = BASE + "\\assets\\icpm";
    static final String DATA = BASE + "\\data\\icpm";

    static final String[] HANDHELD_ITEMS = {
        "flint_knife", "obsidian_knife", "flint_shovel", "flint_hatchet", "flint_axe",
        "silver_pickaxe", "silver_shovel", "silver_axe", "silver_hoe", "silver_sword",
        "silver_hatchet", "silver_dagger", "silver_war_hammer",
        "silver_battle_axe", "silver_scythe", "silver_mattock",
        "ancient_metal_pickaxe", "ancient_metal_shovel", "ancient_metal_axe", "ancient_metal_hoe",
        "ancient_metal_sword", "ancient_metal_hatchet", "ancient_metal_dagger",
        "ancient_metal_war_hammer", "ancient_metal_battle_axe", "ancient_metal_scythe", "ancient_metal_mattock",
        "mithril_pickaxe", "mithril_shovel", "mithril_axe", "mithril_hoe", "mithril_sword",
        "mithril_hatchet", "mithril_dagger", "mithril_war_hammer",
        "mithril_battle_axe", "mithril_scythe", "mithril_mattock",
        "adamantium_pickaxe", "adamantium_shovel", "adamantium_axe", "adamantium_hoe", "adamantium_sword",
        "adamantium_hatchet", "adamantium_dagger", "adamantium_war_hammer",
        "adamantium_battle_axe", "adamantium_scythe", "adamantium_mattock",
    };

    static final String[] GENERATED_ITEMS = {
        "flint_fragment",
        "silver_nugget", "silver_ingot",
        "ancient_metal_nugget", "ancient_metal_ingot",
        "mithril_nugget", "mithril_ingot",
        "adamantium_nugget", "adamantium_ingot",
    };

    static final String[][] MATERIALS = {
        {"silver", "silver_ingot"},
        {"ancient_metal", "ancient_metal_ingot"},
        {"mithril", "mithril_ingot"},
        {"adamantium", "adamantium_ingot"},
    };

    public static void main(String[] args) throws IOException {
        // 1. 生成 items/*.json
        int count = 0;
        for (String id : HANDHELD_ITEMS) {
            writeFile(ASSETS + "\\items\\" + id + ".json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \"icpm:item/" + id + "\"\n  }\n}");
            count++;
        }
        for (String id : GENERATED_ITEMS) {
            writeFile(ASSETS + "\\items\\" + id + ".json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \"icpm:item/" + id + "\"\n  }\n}");
            count++;
        }
        System.out.println("Generated " + count + " items/*.json files");

        // 2. 生成合成表
        String stick = "{\"item\": \"minecraft:stick\"}";

        // 石工具
        writeRecipe("flint_knife", shapeless("[{\"item\": \"icpm:flint_fragment\"}]", "flint_knife"));
        writeRecipe("obsidian_knife", shaped("[\"O\", \"S\"]", "{\"O\": {\"item\": \"icpm:obsidian_shard\"}, \"S\": " + stick + "}", "obsidian_knife"));
        writeRecipe("flint_shovel", shaped("[\"F\", \"S\", \"S\"]", "{\"F\": {\"item\": \"icpm:flint_fragment\"}, \"S\": " + stick + "}", "flint_shovel"));
        writeRecipe("flint_hatchet", shaped("[\"F\", \"S\"]", "{\"F\": {\"item\": \"icpm:flint_fragment\"}, \"S\": " + stick + "}", "flint_hatchet"));
        writeRecipe("flint_axe", shaped("[\"FF\", \"FS\", \" S\"]", "{\"F\": {\"item\": \"icpm:flint_fragment\"}, \"S\": " + stick + "}", "flint_axe"));

        for (String[] mat : MATERIALS) {
            String m = mat[0];
            String ingot = mat[1];
            String ingotItem = "{\"item\": \"icpm:" + ingot + "\"}";
            String nuggetItem = "{\"item\": \"icpm:" + m + "_nugget\"}";

            // 粒 -> 锭
            writeRecipe(m + "_ingot", shaped("[\"NNN\", \"NNN\", \"NNN\"]", "{\"N\": " + nuggetItem + "}", "icpm:" + ingot));
            // 锭 -> 粒
            writeRecipe(m + "_nugget", shapeless("[{\"item\": \"icpm:" + ingot + "\"}]", "icpm:" + m + "_nugget"));

            // 工具
            writeRecipe(m + "_pickaxe", shaped("[\"III\", \" S \", \" S \"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_pickaxe"));
            writeRecipe(m + "_shovel", shaped("[\"I\", \"S\", \"S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_shovel"));
            writeRecipe(m + "_axe", shaped("[\"II\", \"IS\", \" S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_axe"));
            writeRecipe(m + "_hoe", shaped("[\"II\", \" S\", \" S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_hoe"));
            writeRecipe(m + "_sword", shaped("[\"I\", \"I\", \"S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_sword"));
            writeRecipe(m + "_hatchet", shaped("[\"I\", \"S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_hatchet"));
            writeRecipe(m + "_dagger", shaped("[\"I\", \"S\"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_dagger"));
            writeRecipe(m + "_war_hammer", shaped("[\"I I\", \" S \", \" S \"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_war_hammer"));
            writeRecipe(m + "_battle_axe", shaped("[\"I I\", \"ISI\", \" S \"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_battle_axe"));
            writeRecipe(m + "_scythe", shaped("[\" II\", \"  S\", \" S \"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_scythe"));
            writeRecipe(m + "_mattock", shaped("[\"I I\", \"ISI\", \" S \"]", "{\"I\": " + ingotItem + ", \"S\": " + stick + "}", "icpm:" + m + "_mattock"));
        }

        System.out.println("Generated recipes for " + MATERIALS.length + " materials");
        System.out.println("Done!");
    }

    static String shaped(String pattern, String key, String result) {
        return "{\n  \"type\": \"minecraft:crafting_shaped\",\n  \"pattern\": " + pattern + ",\n  \"key\": " + key + ",\n  \"result\": {\"item\": \"" + result + "\"}\n}";
    }

    static String shapeless(String ingredients, String result) {
        return "{\n  \"type\": \"minecraft:crafting_shapeless\",\n  \"ingredients\": " + ingredients + ",\n  \"result\": {\"item\": \"" + result + "\"}\n}";
    }

    static void writeRecipe(String name, String content) throws IOException {
        writeFile(DATA + "\\recipe\\" + name + ".json", content);
    }

    static void writeFile(String path, String content) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }
}
