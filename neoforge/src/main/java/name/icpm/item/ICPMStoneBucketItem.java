package name.icpm.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * ICPM 石桶（contents=stone）。
 *
 * <p>R196 忠实移植（ItemBucket.java useOnBlock）：
 * {@code if (this.contains(Material.stone)) return false;} —— 石桶**不可右键倒出/放置任何内容**，
 * 它只是「岩浆桶遇水冷却」后的产物（见 PlayerMixin/ItemEntityMixin 的冷却转换），
 * 可经砧/分解配方拆回空桶，或直接丢弃。
 *
 * <p>历史上本类曾是"右键放置圆石"的搬运工具，与 R196 语义冲突，已于 1.0.7 移除该行为。
 */
public class ICPMStoneBucketItem extends Item {
    private final String metal;

    public ICPMStoneBucketItem(String metal, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.metal = metal;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // R196: contains(stone) → 右键无任何行为
        return InteractionResult.FAIL;
    }
}
