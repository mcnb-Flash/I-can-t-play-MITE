package name.icpm.block;

import name.icpm.ICPM;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/**
 * ICPM 方块创造模式标签页
 * 图标: 地幔 (mantle)
 */
public class ICPMBlockGroup {

    /**
     * ICPM 方块列表的 ResourceKey
     */
    public static final ResourceKey<CreativeModeTab> ICPM_BLOCKS_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "blocks"));

    /**
     * 注册 ICPM 方块列表到 Fabric 注册表
     */
    public static void register() {
        CreativeModeTab tab = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup." + ICPM.MOD_ID + ".blocks"))
            .icon(() -> new ItemStack(ICPMBlocks.MANTLE != null ? ICPMBlocks.MANTLE : net.minecraft.world.item.Items.AIR))
            .displayItems((params, output) -> {
                // 添加所有ICPM方块
                for (String name : ICPMBlocks.BLOCK_NAMES) {
                    Identifier id = Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name);
                    BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> {
                        // 仅接受有对应物品的方块：chipped_*/damaged_* 等旧存档兼容方块
                        // 只注册了 Block 未注册 BlockItem（asItem()==Items.AIR），
                        // new ItemStack(AIR) 会得到 count=0 的空栈，触发
                        // CreativeModeTab.Output "Stack size must be exactly 1" 崩溃。
                        if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                            output.accept(block);
                        }
                    });
                }
            })
            .build();

        // 注册到 Fabric 注册表
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ICPM_BLOCKS_KEY, tab);
    }
}