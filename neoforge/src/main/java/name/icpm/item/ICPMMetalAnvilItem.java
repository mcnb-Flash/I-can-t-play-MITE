package name.icpm.item;

import name.icpm.block.BlockMetalAnvil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 金属砧物品（对齐 R196 ItemAnvilBlock）。
 *
 * R196 中 ItemAnvilBlock 是 IDamageableItem，setMaxDamage(getDurability())，
 * 砧的耐久值直接保存在物品自身；掉落时写入 item_damage，放置时从 item_damage 恢复。
 *
 * 本模组金属砧采用「变体方块 + 方块实体」架构（stage 由方块 id 表达），
 * 但耐久权威值仍保存在方块实体的 damage 字段并写入 NBT 持久化。
 * 为让玩家在物品栏直观看到砧的耐久，这里设置 maxDamage = 方块最大耐久，
 * 掉落时把方块实体的 damage 同步进 ItemStack.damage（真实计入物品数据），
 * 放置时由 Fabric 注入的 BLOCK_ENTITY_DATA 还原回方块实体（见 Tile  EntityMetalAnvil.loadAdditional）。
 */
public class ICPMMetalAnvilItem extends BlockItem {

    public ICPMMetalAnvilItem(BlockMetalAnvil block, Item.Properties properties) {
        // 设置最大耐久（与方块实体一致）；堆叠强制为 1
        super(block, properties.durability(block.getMaxDurability()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);

        int maxDurability = ((BlockMetalAnvil) this.getBlock()).getMaxDurability();
        int damage = stack.getDamageValue();

        // 剩余耐久（R196：damage 越接近 maxDurability 越接近损坏）
        int remaining = Math.max(0, maxDurability - damage);

        // 中国惯例：剩余多 → 红（充足），剩余少 → 绿（危险）。此处耐久非涨跌，仅表示充足度：
        // 剩余比例高用红色，低用绿色（与股票涨红跌绿同义：满=红，空=绿）。
        String colorCode;
        float ratio = remaining / (float) maxDurability;
        if (ratio > 0.5f) {
            colorCode = "§c"; // 红：耐久充足
        } else if (ratio > 0.2f) {
            colorCode = "§6"; // 橙黄：中等
        } else {
            colorCode = "§a"; // 绿：即将损坏
        }

        tooltipComponents.accept(Component.literal(
                colorCode + "砧耐久: " + remaining + "/" + maxDurability
        ));
    }

    /**
     * 获取砧物品的当前磨损值（仅为展示/兼容用途）。
     */
    public static int getStoredDamage(ItemStack stack) {
        return stack.getDamageValue();
    }

    /**
     * 把方块实体的 damage 写入掉落物品（真实计入物品数据）。
     * 同时保留 BLOCK_ENTITY_DATA 组件以便放置时还原（见 TileEntityMetalAnvil）。
     */
    public static void applyAnvilDamageToItem(ItemStack drop, int damage) {
        drop.setDamageValue(damage);
    }
}
