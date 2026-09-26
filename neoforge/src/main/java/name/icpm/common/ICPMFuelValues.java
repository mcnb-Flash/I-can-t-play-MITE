package name.icpm.common;

import name.icpm.item.ICPMBucketItem;
import name.icpm.item.ICPMItems;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * R196 燃料表（燃烧时长 + 热量等级）。
 *
 * <p><b>判决源</b>（1.6.4 MITE R196 反编译）：
 * <ul>
 *   <li>{@code Item.getBurnTime} —— Item.java:1453-1479：纸 25 / 粪便 100 / 木棍·箭 100 /
 *       书·书与笔·成书·附魔书 100 / 木门 400 / 烈焰棒 2400 / 任何木材材质物品 200 / 任何纸材质物品 50，其余 0</li>
 *   <li>{@code Item.getHeatLevel} —— Item.java:1481-1486：烈焰棒 4，其余「能烧就是 1」</li>
 *   <li>{@code ItemBlock.getBurnTime} —— ItemBlock.java:167-191：原木 1600 / 木板·木按钮 400 /
 *       木台阶·树苗·枯灌木 200 / 火把·红石火把 800 / 任何木材材质方块 400 / 煤炭块 16000</li>
 *   <li>{@code ItemBlock.getHeatLevel} —— ItemBlock.java:194-197：煤炭块 2</li>
 *   <li>{@code ItemCoal} —— ItemCoal.java:48-58：煤炭与木炭都是 1600；煤炭热量 2、木炭热量 1</li>
 *   <li>{@code ItemTool} —— ItemTool.java:277-279：木材质工具 200，含木柄的其它材质工具 100</li>
 *   <li>{@code ItemBucket} —— ItemBucket.java:255-268：装岩浆的容器 3200、热量 3</li>
 *   <li>{@code ItemBowl} —— ItemBowl.java:121-123：空木碗 200</li>
 *   <li>{@code ItemBoat} —— ItemBoat.java:90-92：船 1200</li>
 * </ul>
 *
 * <p><b>为什么要有这个类</b>：1.21.11 的燃料是代码内置的 {@code FuelValues.vanillaBurnTimes}，
 * 无法用数据包追加，且与 R196 差异极大 —— 最典型三处：原木 300 → R196 <b>1600</b>、
 * 岩浆桶 20000 → R196 <b>3200</b>、火把在原版<b>根本不是燃料</b> → R196 <b>800</b>。
 *
 * <p><b>接线点</b>：{@code FurnaceFuelMixin}（原版熔炉的 {@code AbstractFurnaceBlockEntity.getBurnDuration}）
 * 与 {@code ICPMFurnaceBlockEntity}（ICPM 熔炉的燃烧时长 / 热量等级）。
 *
 * <p><b>两条已记录的偏离</b>（有意为之，勿当 bug「修」）：
 * <ol>
 *   <li>R196 时代不存在的 1.21 新物品（干海带块、竹、脚手架…）原版认为可烧，本类不返回 0 去禁用它们 ——
 *       即只有在 R196 明确给出 &gt; 0 的条目上才覆盖原版值。</li>
 *   <li>R196 的「任何木材材质物品 → 200 / 纸材质 → 50」是按 {@code Material} 体系判定的，
 *       1.21 无对应概念，此处用标签集合（木板 / 原木 / 台阶 / 楼梯 / 栅栏 / 门 / 活板门 / 按钮 /
 *       压力板 / 树苗 / 告示牌 / 船 等）近似，纸材质仅取地图。</li>
 *   <li>双木台阶（R196 400）：1.21 起双台阶不是独立物品（是同方块的 {@code type=double} 状态），
 *       玩家拿不到该物品，故不实现。</li>
 * </ol>
 */
public final class ICPMFuelValues {

    private ICPMFuelValues() {
    }

    /** 是否属于「木材材质」的物品（R196 {@code hasMaterial(Material.wood)} 的 1.21 近似）。 */
    private static boolean isWooden(ItemStack stack) {
        return stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.LOGS)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.WOODEN_SLABS)
                || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(ItemTags.WOODEN_FENCES)
                || stack.is(ItemTags.FENCE_GATES)
                || stack.is(ItemTags.WOODEN_DOORS)
                || stack.is(ItemTags.WOODEN_TRAPDOORS)
                || stack.is(ItemTags.WOODEN_BUTTONS)
                || stack.is(ItemTags.WOODEN_PRESSURE_PLATES)
                || stack.is(ItemTags.BOATS)
                || stack.is(ItemTags.SIGNS)
                || stack.is(ItemTags.HANGING_SIGNS);
    }

    /** ICPM / 原版岩浆容器（R196 {@code ItemBucket.contains(Material.lava)}）。 */
    private static boolean isLavaVessel(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) {
            return true;
        }
        return item instanceof ICPMBucketItem bucket && bucket.getContent() == Fluids.LAVA;
    }

    /** 箭类（R196 {@code instanceof ItemArrow}）。 */
    private static boolean isArrow(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.ARROW || item == Items.SPECTRAL_ARROW || item == Items.TIPPED_ARROW;
    }

    /**
     * 燃烧时长（tick）。返回 0 表示「R196 未定义为燃料」—— 调用方此时应保留原版判定，
     * 不要据此禁用原版燃料（见类注释的偏离 1）。
     */
    public static int burnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();

        // ===== 各类覆写了 getBurnTime 的物品（优先级高于基类表）=====
        // ItemBucket.java:255 —— 装岩浆的容器 3200（R196：与容器材质无关）
        if (isLavaVessel(stack)) {
            return 3200;
        }
        // ItemBoat.java:90 —— 船 1200
        if (stack.is(ItemTags.BOATS)) {
            return 1200;
        }
        // ItemBowl.java:121 —— 空木碗 200（1.21 的碗恒为空；蘑菇煲/兔肉煲是另外的物品）
        if (item == Items.BOWL) {
            return 200;
        }
        // ItemTool.java:277 —— 木材质工具 200，含木柄的其它材质工具 100
        if (ICPMToolProperties.isICPMTool(stack)) {
            ICPMToolProperties.ToolMaterial material = ICPMToolProperties.getToolMaterial(stack);
            return material == ICPMToolProperties.ToolMaterial.WOOD ? 200 : 100;
        }

        // ===== Item.getBurnTime（Item.java:1453）=====
        if (item == Items.PAPER) {
            return 25;
        }
        if (item == ICPMItems.MANURE) {
            return 100;
        }
        if (item == Items.STICK || isArrow(stack)) {
            return 100;
        }
        if (item == Items.BOOK || item == Items.WRITABLE_BOOK
                || item == Items.WRITTEN_BOOK || item == Items.ENCHANTED_BOOK) {
            return 100;
        }
        if (stack.is(ItemTags.WOODEN_DOORS)) {
            return 400;
        }
        if (item == Items.BLAZE_ROD) {
            return 2400;
        }

        // ===== ItemBlock.getBurnTime（ItemBlock.java:167）—— 方块物品 =====
        if (item instanceof net.minecraft.world.item.BlockItem) {
            if (stack.is(ItemTags.LOGS)) {
                return 1600; // R196 Block.wood
            }
            if (stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_BUTTONS)) {
                return 400;  // R196 Block.planks / woodenButton
            }
            if (stack.is(ItemTags.WOODEN_SLABS)
                    || stack.is(ItemTags.SAPLINGS)
                    || item == Items.DEAD_BUSH) {
                return 200;  // R196 Block.woodSingleSlab / sapling / deadBush
            }
            // 注意：挂墙火把与普通火把共用同一个物品（WALL_TORCH / REDSTONE_WALL_TORCH /
            // SOUL_WALL_TORCH 只是方块 id，没有对应的 Items 常量），故物品级只需判这三个。
            if (item == Items.TORCH || item == Items.REDSTONE_TORCH || item == Items.SOUL_TORCH) {
                return 800;  // R196 Block.torchWood / BlockRedstoneTorch（灵魂火把为 1.21 同类延展）
            }
            if (item == Items.COAL_BLOCK) {
                return 16000; // R196 Block.coalBlock
            }
            if (isWooden(stack)) {
                return 400;  // R196 「任何木材材质方块」
            }
            return 0;
        }

        // ===== 物品侧兜底（Item.java:1472-1477）=====
        if (isWooden(stack)) {
            return 200;
        }
        if (item == Items.MAP || item == Items.FILLED_MAP) {
            return 50; // R196 「任何纸材质物品」（1.21 无 Material 体系，仅取地图近似）
        }
        return 0;
    }

    /**
     * 热量等级（R196 {@code getHeatLevel}）：烈焰棒 4 / 岩浆容器 3 / 煤炭与煤炭块 2 / 木炭 1 /
     * 其余能烧即 1 / 不能烧为 0。
     */
    public static int heatLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item == Items.BLAZE_ROD) {
            return 4;
        }
        if (isLavaVessel(stack)) {
            return 3;
        }
        if (item == Items.COAL || item == Items.COAL_BLOCK) {
            return 2;
        }
        if (item == Items.CHARCOAL) {
            return 1;
        }
        return burnTime(stack) > 0 ? 1 : 0;
    }
}
