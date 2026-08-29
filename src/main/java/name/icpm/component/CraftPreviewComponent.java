package name.icpm.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 合成预览数据组件
 *
 * 仅挂在"工作台结果槽预览物品"上（不进入玩家背包），用于 tooltip 展示：
 * - xpCost：本次合成选定品质所需的经验消耗量（average 及以下为 0）
 * - minQualityOrdinal / maxQualityOrdinal：玩家当前可合成的品质区间（用于"可切换品质"提示）
 *
 * 对齐 R196：品质切换/经验消耗信息原本由客户端 player.crafting_experience_cost 等字段
 * 在 ItemStack.getTooltip 中读取展示；此处改为随预览物品同步，避免依赖客户端全局字段。
 */
public record CraftPreviewComponent(int xpCost, int minQualityOrdinal, int maxQualityOrdinal) {

    public static final Codec<CraftPreviewComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.INT.fieldOf("xp_cost").forGetter(CraftPreviewComponent::xpCost),
                            Codec.INT.fieldOf("min_q").forGetter(CraftPreviewComponent::minQualityOrdinal),
                            Codec.INT.fieldOf("max_q").forGetter(CraftPreviewComponent::maxQualityOrdinal)
                    )
                    .apply(instance, CraftPreviewComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewComponent> STREAM_CODEC =
            StreamCodec.of(
                    (buf, c) -> {
                        buf.writeVarInt(c.xpCost());
                        buf.writeVarInt(c.minQualityOrdinal());
                        buf.writeVarInt(c.maxQualityOrdinal());
                    },
                    buf -> new CraftPreviewComponent(buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
            );
}
