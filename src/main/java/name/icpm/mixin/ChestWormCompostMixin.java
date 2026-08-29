package name.icpm.mixin;

import name.icpm.common.ICPMCompostHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 箱子堆肥（1.6.4 TileEntityChest.checkForWormComposting）
 *
 * 1.21.11 箱子服务端没有 tick（ChestBlock.getTicker 服务端返回 null），
 * 因此在玩家打开/关闭箱子时触发堆肥：箱子里每只活虫尝试吃一份可堆肥的植物类物品，
 * 堆肥值累计满 1.0 时产出 1 个粪便。
 *
 * 堆肥进度（0.0-0.99）由本 mixin 的 @Unique 字段持有，并在 NBT 存取时持久化。
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestWormCompostMixin {

    @Unique
    private float icpm$compost = 0.0f;

    @Inject(method = "startOpen", at = @At("HEAD"))
    private void icpm$onStartOpen(ContainerUser user, CallbackInfo ci) {
        this.icpm$compost = ICPMCompostHelper.tryCompostChest((ChestBlockEntity) (Object) this, this.icpm$compost);
    }

    @Inject(method = "stopOpen", at = @At("HEAD"))
    private void icpm$onStopOpen(ContainerUser user, CallbackInfo ci) {
        this.icpm$compost = ICPMCompostHelper.tryCompostChest((ChestBlockEntity) (Object) this, this.icpm$compost);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void icpm$onLoad(ValueInput input, CallbackInfo ci) {
        this.icpm$compost = input.getFloatOr("mite_compost", 0.0f);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void icpm$onSave(ValueOutput output, CallbackInfo ci) {
        output.putFloat("mite_compost", this.icpm$compost);
    }
}
