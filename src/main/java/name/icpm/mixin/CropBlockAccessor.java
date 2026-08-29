package name.icpm.mixin;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 暴露 CropBlock.getBaseSeedId（protected → public），供收获附魔使用 */
@Mixin(CropBlock.class)
public interface CropBlockAccessor {
    @Invoker("getBaseSeedId")
    ItemLike icpm$getBaseSeedId();
}
