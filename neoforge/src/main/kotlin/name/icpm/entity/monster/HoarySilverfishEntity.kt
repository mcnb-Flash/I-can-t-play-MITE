package name.icpm.entity.monster

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.level.Level

/**
 * 灰银鱼（R196 EntityHoarySilverfish 移植）。
 *
 * R196 反编译源码 [EntityHoarySilverfish] 完全为空（仅 `extends EntitySilverfish` + 构造函数），
 * 即行为与原版银鱼完全一致，区别仅在生成群系/权重（R196 灰银鱼在特定山地/石头群系生成）。
 * 故此处直接继承 [Silverfish]，不覆写任何逻辑，仅作为独立实体类型承载生成规则与刷怪蛋。
 *
 * 生成：R196 灰银鱼在石头山地（含主世界地下石层）生成，权重较低。Fabric 1.21.11 简化为
 * 全维度、脚下为石头方块处生成（见 [name.icpm.entity.ICPMEntities]）。
 */
class HoarySilverfishEntity(type: EntityType<out HoarySilverfishEntity>, level: Level) : Silverfish(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
    }
}
