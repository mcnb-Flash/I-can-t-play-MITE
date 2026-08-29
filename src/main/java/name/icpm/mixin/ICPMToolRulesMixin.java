package name.icpm.mixin;

import name.icpm.block.BlockICPMWorkbench;
import name.icpm.block.BlockMetalAnvil;
import name.icpm.block.ICPMFurnaceBlock;
import name.icpm.block.ICPMBlocks;
import name.icpm.block.ICPMTagRegistry;
import name.icpm.common.ICPMExperience;
import name.icpm.item.ICPMItems;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 挖掘等级与工具规则 Mixin
 *
 * 工具等级（沿用镐子等级）：
 * - 空手 / 非工具物品 = 0
 * - 木头工具 = 1
 * - 石质工具 = 2
 * - 铜/金/银工具 = 2
 * - 铁工具 = 3
 * - 远古金属工具 = 3.5
 * - 秘银工具 / 钻石工具 = 4
 * - 艾德曼工具 / 下界合金工具 = 5
 *
 * 方块破坏要求：
 * - 原木类方块：必须用斧，等级 >= 1
 * - 石头/矿物/深板岩/矿物块：必须用镐，等级按之前规则
 * - 其他原版手破坏可掉落方块（泥土、草、沙砾、木板、工作台等）：无工具类型限制，等级 0
 *
 * 当工具类型不匹配或等级不足时，getDestroyProgress 返回 -1。
 * 条件满足时保留原版进度，并应用 ICPM 特殊工具速度倍率。
 */
@Mixin(BlockBehaviour.class)
public class ICPMToolRulesMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true, require = 0)
    private void icpm$checkMiningLevel(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (player == null) {
            return;
        }

        // 创造模式不受影响
        if (player.isCreative()) {
            return;
        }

        Block block = state.getBlock();
        float hardness = state.getDestroySpeed(level, pos);

        // 草/高草丛/蕨等植物家族：原版 hardness=0（瞬间破坏），ICPM 设为 0.02 以体现挖掘过程
        if (icpm$isPlantFamily(state)) {
            hardness = 0.02f;
        }

        if (hardness <= 0.0f) {
            return;
        }

        // 植物家族（草/蕨/高草/大型蕨/藤蔓/甜浆果丛/死灌木等）：
        // MITE 中镰刀/剑/剪刀可加速挖掘，其余工具仍可慢速破坏（不 requiresTool）。
        if (icpm$isPlantFamily(state)) {
            boolean plantTool = icpm$isPlantHarvestTool(player.getMainHandItem());
            cir.setReturnValue(icpm$calculateICPMProgress(state, player, hardness, plantTool));
            cir.cancel();
            return;
        }

        ToolInfo tool = icpm$getToolInfo(player.getMainHandItem());
        BlockRequirement req = icpm$getBlockRequirement(state);

        boolean haveTool = tool.type != ToolType.HAND;
        // 仅当手持工具类型正确且等级达标才算“有效工具”。
        // R196：类型/等级不对的工具 getStrVsBlock<=1，退化为 1.0（慢但可破坏）。
        boolean correct = haveTool && tool.type == req.toolType && tool.level >= req.level;

        // R196：requiresTool 方块（石头/矿石/矿物块等）必须用【正确类型且等级达标】的工具，
        // 否则不可破坏（-1）。原实现"类型/等级不对 -> 慢速 1.0 可破坏"在 1.21.11 实测导致
        // 低等级工具能挖高等级矿石（如铜战锤挖秘银矿），违背 MITE 硬核语义，故收紧为不可破坏。
        if (req.requiresTool && (!haveTool || !correct)) {
            cir.setReturnValue(-1.0f);
            cir.cancel();
            return;
        }

        boolean effective = correct;
        // 必须 cancel，否则原版 getDestroyProgress 体会在注入后继续执行并返回，覆盖掉这里的值
        cir.setReturnValue(icpm$calculateICPMProgress(state, player, hardness, effective));
        cir.cancel();
    }

    @Unique
    private float icpm$calculateICPMProgress(BlockState state, Player player, float hardness, boolean effective) {
        // 有效工具（类型正确且等级达标）用材质基础速度；无效工具（类型/等级不对或空手）按 R196 退化为 1.0（慢但可破坏）。
        float strVsBlock = 1.0f;
        if (effective) {
            strVsBlock = icpm$getToolStrVsBlock(player.getMainHandItem());
            if (strVsBlock <= 1.0f) {
                // 原版金属工具（铁/金/钻/下界合金）不在 ICPM 材质表内（返回 1.0），
                // 但作为“有效工具”，其原生 getDestroySpeed 会返回 Tier 基础速度，用其替代。
                float nativeStr = player.getDestroySpeed(state);
                if (nativeStr > strVsBlock) {
                    strVsBlock = nativeStr;
                }
            }
        }
        // 进度增量除数：原版 getDestroyProgress 用 30（有正确工具）/100（无正确工具）。
        // 本 mod 仅需对“黑曜石系方块 + 工作方块（工作台/金属砧）”恢复原版等价速度（除数 30）；
        // 其余普通方块仍保留慢 17 倍的 512 除数（即 ICPM/R196 的偏慢手感）。
        float divider = icpm$isFastMiningBlock(state) ? 30.0f : 512.0f;
        float progress = strVsBlock / hardness / divider;
        if (effective) {
            progress *= icpm$getToolSpeedMultiplier(state, player.getMainHandItem());
        }

        // R196 惩罚：未着地 / 在水中 / 无食物能量时再减 5 倍
        if (!player.onGround()) {
            progress /= 5.0f;
        }
        if (player.isInWater() && !icpm$hasAquaAffinity(player)) {
            progress /= 5.0f;
        }
        if (player.getFoodData().getFoodLevel() <= 0) {
            progress /= 5.0f;
        }

        // ICPM 等级采集修正（R196 EnumLevelBonus.HARVESTING）：
        // 负等级减速、正等级加速。getDestroyProgress 的返回值是 GUI 破坏进度增量，
        // 直接乘进这里才能真实影响"破坏一个方块所需的 tick 数"。
        int lvl = ICPMExperience.getExperienceLevel(ICPMExperience.getExperience(player));
        if (lvl != 0) {
            float harvestMod = 1.0f + ICPMExperience.getLevelModifier(lvl, ICPMExperience.LevelBonus.HARVESTING);
            if (harvestMod > 0.0f) {
                progress *= harvestMod;
            }
        }

        return progress;
    }

    /**
     * 是否为“应恢复原版挖掘速度”的方块：
     * 黑曜石系（黑曜石 / 哭泣黑曜石）+ 工作方块（各类工作台、各类金属砧）。
     * 这些方块用除数 30（原版等价）；其余方块仍用 512（慢 17 倍）。
     */
    @Unique
    private boolean icpm$isFastMiningBlock(BlockState state) {
        Block block = state.getBlock();

        // 黑曜石系
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return true;
        }

        // 工作台类：ICPM 各等级工作台 + 原版工作台
        if (block instanceof BlockICPMWorkbench || block == Blocks.CRAFTING_TABLE) {
            return true;
        }

        // 金属砧类：ICPM 各金属砧（含 chipped/damaged 变体）+ 原版铁砧三态
        if (block instanceof BlockMetalAnvil
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL) {
            return true;
        }

        // ICPM 熔炉类（粘土/硬化粘土/沙石/黑曜石/地狱岩）：与工作方块同待遇，空手快速破坏
        if (block instanceof ICPMFurnaceBlock) {
            return true;
        }

        return false;
    }

    /**
     * 是否为“工作方块”：各类工作台 / 各类金属砧 / 各类 ICPM 熔炉。
     * 这些方块在 R196 中空手即可快速破坏（不受工具类型/等级限制，requiresTool=false），
     * 且由 icpm$isFastMiningBlock 使其走 divider=30（原版等价速度）。
     */
    @Unique
    private boolean icpm$isWorkBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockICPMWorkbench || block == Blocks.CRAFTING_TABLE) {
            return true;
        }
        if (block instanceof BlockMetalAnvil
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL) {
            return true;
        }
        if (block instanceof ICPMFurnaceBlock) {
            return true;
        }
        return false;
    }

    @Unique
    private float icpm$getToolStrVsBlock(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1.0f;
        }

        // ICPM 专属工具：按材质 + 类型查表（覆盖所有 ICPM 材质）
        ICPMToolProperties.ToolMaterial material = ICPMToolProperties.INSTANCE.getToolMaterial(stack);
        if (material != null) {
            return icpm$materialToBaseStrVsBlock(material);
        }

        Item item = stack.getItem();

        // 原版木工具 2.0
        if (icpm$isWoodenTool(item)) {
            return 2.0f;
        }

        // 原版石工具 4.0
        if (icpm$isStoneTool(item)) {
            return 4.0f;
        }

        // 剑类：MITE 中剑可加速砍植物，按材质给基础挖掘速度（icpm 剑已在上方材质表覆盖，
        // 这里仅处理原版剑：wood/stone/iron/gold/diamond/netherite）
        if (icpm$isSword(item)) {
            return icpm$swordBaseStr(item);
        }

        // 手持非工具物品按空手算
        return 1.0f;
    }

    /**
     * 材质基础挖掘速度（R196 hardness/speed 对应值）
     */
    @Unique
    private float icpm$materialToBaseStrVsBlock(ICPMToolProperties.ToolMaterial material) {
        // 注意：必须用 if/else + == 比较，绝不能用 switch(枚举) 表达式。
        // mixin @Unique 方法里的 switch 枚举表达式会生成挂在目标类(class_4970)下的
        // $Anonymous$<hash> 合成类，运行时该匿名类不存在 → NoClassDefFoundError。
        if (material == ICPMToolProperties.ToolMaterial.FLINT) return 4.0f;
        if (material == ICPMToolProperties.ToolMaterial.COPPER) return 5.0f;
        if (material == ICPMToolProperties.ToolMaterial.WOOD) return 2.0f;
        if (material == ICPMToolProperties.ToolMaterial.SILVER) return 6.0f;
        if (material == ICPMToolProperties.ToolMaterial.GOLD) return 12.0f;
        if (material == ICPMToolProperties.ToolMaterial.IRON) return 6.0f;
        if (material == ICPMToolProperties.ToolMaterial.ANCIENT_METAL) return 7.0f;
        if (material == ICPMToolProperties.ToolMaterial.MITHRIL || material == ICPMToolProperties.ToolMaterial.DIAMOND) return 8.0f;
        if (material == ICPMToolProperties.ToolMaterial.ADAMANTIUM) return 9.0f;
        if (material == ICPMToolProperties.ToolMaterial.NETHERITE) return 10.0f;
        if (material == ICPMToolProperties.ToolMaterial.LEATHER) return 4.0f;
        return 0.0f;
    }

    @Unique
    private boolean icpm$hasAquaAffinity(Player player) {
        var enchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 手持工具信息
     * 优先使用 ICPMToolProperties 的统一映射（覆盖所有 ICPM 材质+类型），
     * 必要时回退到原版/木石工具判断。
     */
    @Unique
    private ToolInfo icpm$getToolInfo(ItemStack stack) {
        if (stack.isEmpty()) {
            return new ToolInfo(ToolType.HAND, 0.0f);
        }

        ToolType type = icpm$getToolType(stack.getItem());
        float level = icpm$getToolMaterialLevel(stack.getItem());
        return new ToolInfo(type, level);
    }

    /**
     * 判断工具挖掘类型（基于 ICPMToolProperties 统一映射）
     */
    @Unique
    private ToolType icpm$getToolType(Item item) {
        // ICPM 专属工具（所有材质均已覆盖）
        ICPMToolProperties.ToolCategory category = ICPMToolProperties.INSTANCE.getToolCategoryByItem(item);
        if (category != null) {
            ToolType mapped = icpm$categoryToToolType(category);
            if (mapped != null) {
                return mapped;
            }
        }

        // 原版工具回退
        if (icpm$isPickaxe(item)) {
            return ToolType.PICKAXE;
        }
        if (item instanceof AxeItem) {
            return ToolType.AXE;
        }
        if (item instanceof ShovelItem) {
            return ToolType.SHOVEL;
        }
        if (item instanceof HoeItem) {
            return ToolType.HOE;
        }

        return ToolType.HAND;
    }

    /**
     * ToolCategory -> 挖掘 ToolType 映射
     */
    @Unique
    private ToolType icpm$categoryToToolType(ICPMToolProperties.ToolCategory category) {
        // 注意：必须用 if/else + == 比较，绝不能用 switch(枚举) 表达式（见 icpm$materialToBaseStrVsBlock 注释）。
        if (category == ICPMToolProperties.ToolCategory.PICKAXE) return ToolType.PICKAXE;
        if (category == ICPMToolProperties.ToolCategory.WAR_HAMMER) return ToolType.PICKAXE;  // 战锤作为镐挖掘石头/矿物
        if (category == ICPMToolProperties.ToolCategory.AXE
                || category == ICPMToolProperties.ToolCategory.HATCHET
                || category == ICPMToolProperties.ToolCategory.BATTLE_AXE) return ToolType.AXE;
        if (category == ICPMToolProperties.ToolCategory.SHOVEL
                || category == ICPMToolProperties.ToolCategory.MATTOCK) return ToolType.SHOVEL;
        if (category == ICPMToolProperties.ToolCategory.HOE
                || category == ICPMToolProperties.ToolCategory.SCYTHE) return ToolType.HOE;
        return null;  // SWORD, DAGGER, KNIFE, CLUB, CUDGEL 不映射为挖掘类型
    }

    /**
     * 根据工具材质判断挖掘等级
     * 等级映射（基于 ICPM R196 镐子等级）：
     *   wood/flint/stone = 1, copper/gold/silver = 2, iron = 3,
     *   ancient_metal = 3.5, mithril/diamond = 4, adamantium/netherite = 5
     * 优先使用 ICPMToolProperties 统一材质映射，回退到原版木石工具。
     */
    @Unique
    private float icpm$getToolMaterialLevel(Item item) {
        // ICPM 专属材质（覆盖所有材质+类型的工具）
        ICPMToolProperties.ToolMaterial material = ICPMToolProperties.INSTANCE.getToolMaterialByItem(item);
        if (material != null) {
            return icpm$materialToLevel(material);
        }

        // 木质工具（含木头短棍）1级
        if (icpm$isWoodenTool(item)) {
            return 1.0f;
        }

        // 石质工具 2级
        if (icpm$isStoneTool(item)) {
            return 2.0f;
        }

        // 原版铜/金工具 2级（1.21.11 含原版铜工具）
        if (item == Items.COPPER_PICKAXE || item == Items.COPPER_AXE
                || item == Items.COPPER_SHOVEL || item == Items.COPPER_HOE
                || item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE
                || item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_HOE) {
            return 2.0f;
        }

        // 原版铁工具 3级
        if (item == Items.IRON_PICKAXE || item == Items.IRON_AXE
                || item == Items.IRON_SHOVEL || item == Items.IRON_HOE) {
            return 3.0f;
        }

        // 原版钻石工具 4级
        if (item == Items.DIAMOND_PICKAXE || item == Items.DIAMOND_AXE
                || item == Items.DIAMOND_SHOVEL || item == Items.DIAMOND_HOE) {
            return 4.0f;
        }

        // 原版下界合金工具 5级
        if (item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_AXE
                || item == Items.NETHERITE_SHOVEL || item == Items.NETHERITE_HOE) {
            return 5.0f;
        }

        // 手持非工具物品按空手算
        return 0.0f;
    }

    /**
     * ToolMaterial -> 挖掘等级映射
     */
    @Unique
    private float icpm$materialToLevel(ICPMToolProperties.ToolMaterial material) {
        // 注意：必须用 if/else + == 比较，绝不能用 switch(枚举) 表达式（见 icpm$materialToBaseStrVsBlock 注释）。
        if (material == ICPMToolProperties.ToolMaterial.WOOD || material == ICPMToolProperties.ToolMaterial.FLINT) return 1.0f;
        if (material == ICPMToolProperties.ToolMaterial.COPPER
                || material == ICPMToolProperties.ToolMaterial.SILVER
                || material == ICPMToolProperties.ToolMaterial.GOLD) return 2.0f;
        if (material == ICPMToolProperties.ToolMaterial.IRON) return 3.0f;
        if (material == ICPMToolProperties.ToolMaterial.ANCIENT_METAL) return 3.5f;
        if (material == ICPMToolProperties.ToolMaterial.MITHRIL || material == ICPMToolProperties.ToolMaterial.DIAMOND) return 4.0f;
        if (material == ICPMToolProperties.ToolMaterial.ADAMANTIUM || material == ICPMToolProperties.ToolMaterial.NETHERITE) return 5.0f;
        return 0.0f;
    }

    @Unique
    private boolean icpm$isWoodenTool(Item item) {
        return item == Items.WOODEN_PICKAXE || item == Items.WOODEN_AXE ||
               item == Items.WOODEN_SHOVEL || item == Items.WOODEN_HOE ||
               item == Items.WOODEN_SWORD || item == ICPMItems.WOOD_CUDGEL;
    }

    @Unique
    private boolean icpm$isStoneTool(Item item) {
        return item == Items.STONE_PICKAXE || item == Items.STONE_AXE ||
               item == Items.STONE_SHOVEL || item == Items.STONE_HOE ||
               item == Items.STONE_SWORD;
    }

    @Unique
    private boolean icpm$isPickaxe(Item item) {
        return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE ||
               item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE ||
               item == Items.COPPER_PICKAXE ||
               item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE ||
               item == ICPMItems.SILVER_PICKAXE || item == ICPMItems.ANCIENT_METAL_PICKAXE ||
               item == ICPMItems.MITHRIL_PICKAXE || item == ICPMItems.ADAMANTIUM_PICKAXE;
    }

    /**
     * 获取方块破坏要求
     */
    @Unique
    private BlockRequirement icpm$getBlockRequirement(BlockState state) {
        Block block = state.getBlock();

        // 工作方块（各类工作台 / 各类金属砧 / 各类 ICPM 熔炉）：
        // R196 中所有工作方块均可空手快速破坏（isFastMiningBlock 已使其走 divider=30），
        // 故 requiresTool=false，空手即破，且不受工具类型/等级限制。
        if (icpm$isWorkBlock(state)) {
            return new BlockRequirement(ToolType.HAND, 0.0f, false);
        }

        // 原木类：必须用斧，等级 1；MITE/R196 中木头需斧砍伐，空手不可破坏（requiresTool=true）
        if (icpm$isLog(state)) {
            return new BlockRequirement(ToolType.AXE, 1.0f, true);
        }

        // 原版特殊矿石（含深层变种）：必须先于深板岩判定，否则深层钻石矿会被误归深板岩=3级。
        // R196: 钻石矿/艾德曼矿=4级；绿宝石矿/金矿/深层铁矿等=3级。
        Float vanillaOreLevel = icpm$getVanillaOreLevel(state);
        if (vanillaOreLevel != null) {
            return new BlockRequirement(ToolType.PICKAXE, vanillaOreLevel, true);
        }

        // 深板岩及深板岩建筑方块：必须用镐，等级 3
        if (icpm$isDeepslate(state)) {
            return new BlockRequirement(ToolType.PICKAXE, 3.0f, true);
        }

        // 黑曜石：必须用镐，等级 3（铁镐即可破坏）
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return new BlockRequirement(ToolType.PICKAXE, 3.0f, true);
        }

        // 矿物块：必须用镐
        Float metalBlockLevel = icpm$getMetalBlockLevel(block);
        if (metalBlockLevel != null) {
            return new BlockRequirement(ToolType.PICKAXE, metalBlockLevel, true);
        }

        // ICPM 矿石：按材质等级
        Float miteOreLevel = icpm$getICPMOreLevel(block);
        if (miteOreLevel != null) {
            return new BlockRequirement(ToolType.PICKAXE, miteOreLevel, true);
        }

        // 石头、矿物：必须用镐，等级 2
        if (icpm$isStoneOrOre(state)) {
            return new BlockRequirement(ToolType.PICKAXE, 2.0f, true);
        }

        // ICPM 自定义需镐方块
        if (ICPMTagRegistry.isPickaxeRequired(block)) {
            return new BlockRequirement(ToolType.PICKAXE, 0.0f, true);
        }

        // 南瓜 / 西瓜 / 可可豆：用斧（空手可慢速破坏）
        if (block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN
                || block == Blocks.JACK_O_LANTERN || block == Blocks.MELON
                || block == Blocks.COCOA) {
            return new BlockRequirement(ToolType.AXE, 1.0f, false);
        }

        // 其余按原版“可挖掘标签”分类（忠实 R196 工具有效性：类型不对 -> 慢但不禁用）
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return new BlockRequirement(ToolType.PICKAXE, 0.0f, true);
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return new BlockRequirement(ToolType.AXE, 1.0f, false);
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return new BlockRequirement(ToolType.SHOVEL, 0.0f, false);
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return new BlockRequirement(ToolType.HOE, 0.0f, false);
        }

        // 其他（红石、TNT、树叶等）：无工具类型限制，等级 0
        return new BlockRequirement(ToolType.HAND, 0.0f, false);
    }

    @Unique
    private boolean icpm$isPlantFamily(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS ||
               block == Blocks.FERN || block == Blocks.LARGE_FERN ||
               block == Blocks.VINE || block == Blocks.TWISTING_VINES ||
               block == Blocks.TWISTING_VINES_PLANT || block == Blocks.WEEPING_VINES ||
               block == Blocks.WEEPING_VINES_PLANT || block == Blocks.CAVE_VINES ||
               block == Blocks.CAVE_VINES_PLANT || block == Blocks.SWEET_BERRY_BUSH ||
               block == Blocks.DEAD_BUSH || block == Blocks.HANGING_ROOTS ||
               block == Blocks.SPORE_BLOSSOM || block == Blocks.NETHER_SPROUTS ||
               block == Blocks.WARPED_ROOTS || block == Blocks.CRIMSON_ROOTS;
    }

    /**
     * 是否为可加速挖掘植物的手持工具：镰刀 / 剑 / 剪刀。
     */
    @Unique
    private boolean icpm$isPlantHarvestTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ICPMToolProperties.ToolCategory category = ICPMToolProperties.INSTANCE.getToolCategory(stack);
        if (category == ICPMToolProperties.ToolCategory.SCYTHE
                || category == ICPMToolProperties.ToolCategory.SWORD) {
            return true;
        }
        if (icpm$isSword(stack.getItem())) {
            return true;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.ShearsItem) {
            return true;
        }
        return false;
    }

    @Unique
    private boolean icpm$isSword(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    @Unique
    private float icpm$swordBaseStr(Item item) {
        if (item == Items.WOODEN_SWORD) return 2.0f;
        if (item == Items.STONE_SWORD) return 4.0f;
        if (item == Items.IRON_SWORD) return 6.0f;
        if (item == Items.GOLDEN_SWORD) return 12.0f;
        if (item == Items.DIAMOND_SWORD) return 8.0f;
        if (item == Items.NETHERITE_SWORD) return 10.0f;
        return 1.0f;
    }

    @Unique
    private boolean icpm$isLog(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG ||
               block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
               block == Blocks.MANGROVE_LOG || block == Blocks.CHERRY_LOG || block == Blocks.PALE_OAK_LOG ||
               block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_SPRUCE_LOG ||
               block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_CHERRY_LOG ||
               block == Blocks.STRIPPED_PALE_OAK_LOG ||
               block == Blocks.OAK_WOOD || block == Blocks.SPRUCE_WOOD || block == Blocks.BIRCH_WOOD ||
               block == Blocks.JUNGLE_WOOD || block == Blocks.ACACIA_WOOD || block == Blocks.DARK_OAK_WOOD ||
               block == Blocks.MANGROVE_WOOD || block == Blocks.CHERRY_WOOD || block == Blocks.PALE_OAK_WOOD ||
               block == Blocks.STRIPPED_OAK_WOOD || block == Blocks.STRIPPED_SPRUCE_WOOD ||
               block == Blocks.STRIPPED_BIRCH_WOOD || block == Blocks.STRIPPED_JUNGLE_WOOD ||
               block == Blocks.STRIPPED_ACACIA_WOOD || block == Blocks.STRIPPED_DARK_OAK_WOOD ||
               block == Blocks.STRIPPED_MANGROVE_WOOD || block == Blocks.STRIPPED_CHERRY_WOOD ||
               block == Blocks.STRIPPED_PALE_OAK_WOOD;
    }

    @Unique
    private boolean icpm$isDeepslate(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE ||
               block == Blocks.POLISHED_DEEPSLATE || block == Blocks.INFESTED_DEEPSLATE ||
               block == Blocks.DEEPSLATE_BRICKS || block == Blocks.CRACKED_DEEPSLATE_BRICKS ||
               block == Blocks.DEEPSLATE_TILES || block == Blocks.CRACKED_DEEPSLATE_TILES ||
               block == Blocks.CHISELED_DEEPSLATE || block == Blocks.REINFORCED_DEEPSLATE ||
               block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.DEEPSLATE_EMERALD_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == Blocks.DEEPSLATE_REDSTONE_ORE || block == Blocks.DEEPSLATE_COPPER_ORE;
    }

    @Unique
    private Float icpm$getMetalBlockLevel(Block block) {
        // 铜/金/银块：镐2级 + 1 = 3
        if (block == Blocks.COPPER_BLOCK || block == Blocks.CUT_COPPER ||
            block == Blocks.CUT_COPPER_STAIRS || block == Blocks.CUT_COPPER_SLAB ||
            block == Blocks.EXPOSED_COPPER || block == Blocks.EXPOSED_CUT_COPPER ||
            block == Blocks.WEATHERED_COPPER || block == Blocks.WEATHERED_CUT_COPPER ||
            block == Blocks.OXIDIZED_COPPER || block == Blocks.OXIDIZED_CUT_COPPER ||
            block == Blocks.WAXED_COPPER_BLOCK || block == Blocks.WAXED_CUT_COPPER ||
            block == Blocks.GOLD_BLOCK || block == Blocks.RAW_GOLD_BLOCK) {
            return 3.0f;
        }

        // 铁块：铁镐3级 + 1 = 4
        if (block == Blocks.IRON_BLOCK || block == Blocks.RAW_IRON_BLOCK) {
            return 4.0f;
        }

        // 钻石块：钻石镐4级 + 1 = 5
        if (block == Blocks.DIAMOND_BLOCK) {
            return 5.0f;
        }

        // 下界合金块：下界合金镐6级 + 1 = 7
        if (block == Blocks.NETHERITE_BLOCK) {
            return 7.0f;
        }

        // ICPM 金属块：对应镐等级 + 1
        // 铜块：铜镐2级 + 1 = 3级（铁镐可挖）
        // 注意：ICPMBlocks.COPPER_BLOCK 需要先注册
        // 暂时使用 copper_block 作为判断条件
        if (block != null && block.toString().contains("copper_block")) {
            return 3.0f;
        }
        // 银块：银镐2级 + 1 = 3级（铁镐可挖）
        if (ICPMBlocks.SILVER_BLOCK != null && block == ICPMBlocks.SILVER_BLOCK) {
            return 3.0f;
        }
        // 金块：金镐2级 + 1 = 3级（铁镐可挖）
        if (block == Blocks.GOLD_BLOCK) {
            return 3.0f;
        }
        // 远古金属块：远古金属镐3级 + 1.5 = 4.5级（艾德曼镐5级可挖，秘银镐4级不可挖）
        if (ICPMBlocks.ANCIENT_METAL_BLOCK != null && block == ICPMBlocks.ANCIENT_METAL_BLOCK) {
            return 4.5f;
        }
        // 秘银块：秘银镐4级 + 1 = 5级（艾德曼镐5级可挖）
        if (ICPMBlocks.MITHRIL_BLOCK != null && block == ICPMBlocks.MITHRIL_BLOCK) {
            return 5.0f;
        }
        // 艾德曼块：艾德曼镐5级 + 1 = 6级（无对应镐可挖）
        if (ICPMBlocks.ADAMANTIUM_BLOCK != null && block == ICPMBlocks.ADAMANTIUM_BLOCK) {
            return 6.0f;
        }

        return null;
    }

    @Unique
    private Float icpm$getICPMOreLevel(Block block) {
        // 银矿（石质）：银镐2级
        if (ICPMBlocks.SILVER_ORE != null && block == ICPMBlocks.SILVER_ORE) {
            return 2.0f;
        }
        // 深板岩银矿：3级（下限提升，铁镐可挖）
        if (ICPMBlocks.DEEPSLATE_SILVER_ORE != null && block == ICPMBlocks.DEEPSLATE_SILVER_ORE) {
            return 3.0f;
        }
        // 秘银矿（铁质）：铁镐3级可挖
        if (ICPMBlocks.MITHRIL_ORE != null && block == ICPMBlocks.MITHRIL_ORE) {
            return 3.0f;
        }
        if (ICPMBlocks.DEEPSLATE_MITHRIL_ORE != null && block == ICPMBlocks.DEEPSLATE_MITHRIL_ORE) {
            return 3.0f;
        }
        // 艾德曼矿（秘银质）：秘银镐4级可挖
        if (ICPMBlocks.ADAMANTIUM_ORE != null && block == ICPMBlocks.ADAMANTIUM_ORE) {
            return 4.0f;
        }
        if (ICPMBlocks.DEEPSLATE_ADAMANTIUM_ORE != null && block == ICPMBlocks.DEEPSLATE_ADAMANTIUM_ORE) {
            return 4.0f;
        }
        return null;
    }

    /**
     * 获取原版特殊矿石的挖掘等级
     *
     * 规则：
     * - 钻石矿/深层钻石矿：需要秘银镐（4级）
     * - 绿宝石矿/深层绿宝石矿：需要铁镐（3级）
     * - 深层煤矿、深层铁矿、深层铜矿、深层金矿等：需要铁镐（3级）
     * - 秘银矿/深层秘银矿：需要铁镐（3级，在ICPM矿石中已处理）
     */
    @Unique
    private Float icpm$getVanillaOreLevel(BlockState state) {
        // 钻石矿与深层钻石矿：需要秘银镐（4级）
        if (state.is(BlockTags.DIAMOND_ORES)) {
            return 4.0f;
        }

        // 绿宝石矿与深层绿宝石矿：需要铁镐（3级）
        if (state.is(BlockTags.EMERALD_ORES)) {
            return 3.0f;
        }

        // 金矿与深层金矿：需要铁镐（3级）- ICPM特例
        if (state.is(BlockTags.GOLD_ORES)) {
            return 3.0f;
        }

        // 深层煤矿、深层铁矿、深层铜矿、深层红石矿、深层青金石矿：需要铁镐（3级）
        Block block = state.getBlock();
        if (block == Blocks.DEEPSLATE_COAL_ORE ||
            block == Blocks.DEEPSLATE_IRON_ORE ||
            block == Blocks.DEEPSLATE_COPPER_ORE ||
            block == Blocks.DEEPSLATE_REDSTONE_ORE ||
            block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return 3.0f;
        }

        return null;
    }

    @Unique
    private boolean icpm$isStoneOrOre(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE ||
            block == Blocks.GRANITE || block == Blocks.DIORITE || block == Blocks.ANDESITE ||
            block == Blocks.NETHERRACK || block == Blocks.END_STONE ||
            block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.BASALT || block == Blocks.SMOOTH_BASALT ||
            block == Blocks.BLACKSTONE || block == Blocks.POLISHED_BLACKSTONE ||
            block == Blocks.TUFF || block == Blocks.CALCITE || block == Blocks.DRIPSTONE_BLOCK ||
            block == Blocks.AMETHYST_BLOCK || block == Blocks.ANCIENT_DEBRIS) {
            return true;
        }
        if (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) ||
            state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.DIAMOND_ORES) ||
            state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.LAPIS_ORES) ||
            state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.COPPER_ORES)) {
            return true;
        }
        return false;
    }

    /**
     * 获取 ICPM 特殊工具的挖掘速度倍率
     * 基于 ICPMToolProperties 的类型分类，自动覆盖全部材质。
     */
    @Unique
    private float icpm$getToolSpeedMultiplier(BlockState state, ItemStack stack) {
        if (stack.isEmpty()) return 1.0f;

        ICPMToolProperties.ToolCategory category = ICPMToolProperties.INSTANCE.getToolCategory(stack);
        if (category == null) return 1.0f;

        // 短斧: 0.5
        if (category == ICPMToolProperties.ToolCategory.HATCHET) {
            return 0.5f;
        }

        // 战锤: 0.75
        if (category == ICPMToolProperties.ToolCategory.WAR_HAMMER) {
            return 0.75f;
        }

        // 战斧: 1.25
        if (category == ICPMToolProperties.ToolCategory.BATTLE_AXE) {
            return 1.25f;
        }

        // 镰刀: 仅在植物家族（草/蕨/藤蔓等）上加速，对齐 MITE“镰刀割草”
        if (category == ICPMToolProperties.ToolCategory.SCYTHE) {
            return icpm$isPlantFamily(state) ? 2.0f : 1.0f;
        }

        // 剑: 仅在植物家族上加速（比空手快、略逊于镰刀），避免剑加速挖掘普通方块
        if (category == ICPMToolProperties.ToolCategory.SWORD) {
            return icpm$isPlantFamily(state) ? 1.5f : 1.0f;
        }

        // 鸭嘴锄: 0.8
        if (category == ICPMToolProperties.ToolCategory.MATTOCK) {
            return 0.8f;
        }

        // 短棍: 0.25
        if (category == ICPMToolProperties.ToolCategory.CUDGEL) {
            return 0.25f;
        }

        // ICPM 斧/镐/锹 对原木应用默认斧伐乘率
        if (category == ICPMToolProperties.ToolCategory.AXE && icpm$isLog(state)) {
            return 0.5f;
        }

        return 1.0f;
    }

    /**
     * 工具类型枚举
     */
    @Unique
    private enum ToolType {
        HAND, PICKAXE, AXE, SHOVEL, HOE
    }

    /**
     * 工具信息记录
     */
    @Unique
    private record ToolInfo(ToolType type, float level) {
    }

    /**
     * 方块破坏要求记录
     * - toolType：需要的工具类型（HAND 表示无类型限制）
     * - level：需要的最低挖掘等级
     * - requiresTool：空手是否“不可破坏”（R196 的 material.requiresTool，如石头/矿石）
     */
    @Unique
    private record BlockRequirement(ToolType toolType, float level, boolean requiresTool) {
    }
}
