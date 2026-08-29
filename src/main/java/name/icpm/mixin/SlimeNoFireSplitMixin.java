package name.icpm.mixin;

import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 忠实移植：史莱姆燃烧时不分裂。
 * <p>
 * 1.21.11 的史莱姆分裂发生在 {@link Slime#remove} 内部：当实体已死且尺寸 > 1 时通过
 * {@code convertTo} 生成更小的史莱姆（convertTo 涉及的 EntityConversionContext 在官方映射中无命名，
 * 无法直接 redirect）。因此这里改为：在 remove() 头部，若史莱姆正在燃烧且尺寸 > 1，
 * 先 {@link Slime#setSize} 设为最小尺寸，使分裂条件 (getSize() > 1) 不成立，从而不再生成子史莱姆。
 */
@Mixin(Slime.class)
public class SlimeNoFireSplitMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void icpm$preventFireSplit(CallbackInfo ci) {
        Slime self = (Slime) (Object) this;
        if (self.isOnFire() && self.getSize() > 1) {
            self.setSize(1, false);
        }
    }
}
