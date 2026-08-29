package name.icpm.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * 暴露 EnchantmentMenu 私有字段/方法，供 ICPM 附魔消耗逻辑访问。
 * （项目未加载 refmap，用 Accessor/Invoker 而非 @Shadow）
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Accessor("enchantSlots")
    Container getEnchantSlots();

    @Accessor("access")
    ContainerLevelAccess getAccess();

    @Accessor("random")
    RandomSource getRandom();

    @Accessor("enchantmentSeed")
    DataSlot getEnchantmentSeed();

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> invokeGetEnchantmentList(RegistryAccess registryAccess, ItemStack itemStack, int i, int j);
}
