package name.icpm.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * ICPM 营养值组件
 * 存储玩家的蛋白质、必需脂肪和植物营养素
 * 基于 ICPM R196 (1.6.4) 的营养系统
 */
public record NutritionComponent(int protein, int essentialFats, int phytonutrients) {
    public static final Codec<NutritionComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("protein").forGetter(NutritionComponent::protein),
            Codec.INT.fieldOf("essential_fats").forGetter(NutritionComponent::essentialFats),
            Codec.INT.fieldOf("phytonutrients").forGetter(NutritionComponent::phytonutrients)
    ).apply(instance, NutritionComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NutritionComponent> STREAM_CODEC =
            StreamCodec.of(NutritionComponent::write, NutritionComponent::read);

    public static final int MAX = 160000;

    public static final NutritionComponent DEFAULT = new NutritionComponent(MAX, MAX, MAX);

    public static NutritionComponent read(RegistryFriendlyByteBuf buf) {
        return new NutritionComponent(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void write(RegistryFriendlyByteBuf buf, NutritionComponent component) {
        buf.writeVarInt(component.protein);
        buf.writeVarInt(component.essentialFats);
        buf.writeVarInt(component.phytonutrients);
    }

    public NutritionComponent withProtein(int protein) {
        return new NutritionComponent(clamp(protein), this.essentialFats, this.phytonutrients);
    }

    public NutritionComponent withEssentialFats(int essentialFats) {
        return new NutritionComponent(this.protein, clamp(essentialFats), this.phytonutrients);
    }

    public NutritionComponent withPhytonutrients(int phytonutrients) {
        return new NutritionComponent(this.protein, this.essentialFats, clamp(phytonutrients));
    }

    public boolean isMalnourished() {
        return this.protein == 0 || this.phytonutrients == 0;
    }

    public boolean isDoubleMalnourished() {
        return this.protein == 0 && this.phytonutrients == 0;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX, value));
    }
}
