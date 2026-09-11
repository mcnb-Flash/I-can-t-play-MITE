package name.icpm.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.icpm.common.EnumQuality;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 物品品质数据组件
 *
 * 用于存储物品的品质信息
 */
public record QualityComponent(EnumQuality quality) {

    /**
     * 禁止 null 品质：null 会导致序列化/网络编码时崩溃
     */
    public QualityComponent {
        java.util.Objects.requireNonNull(quality, "quality must not be null");
    }

    /**
     * Codec 用于序列化/反序列化
     */
    public static final Codec<QualityComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.xmap(EnumQuality::fromName, EnumQuality::getName)
                            .fieldOf("quality")
                            .forGetter(QualityComponent::quality)
            ).apply(instance, QualityComponent::new)
    );

    /**
     * StreamCodec 用于网络传输
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, QualityComponent> STREAM_CODEC =
            StreamCodec.of(
                    (buf, component) -> buf.writeUtf(component.quality().getName()),
                    buf -> new QualityComponent(EnumQuality.fromName(buf.readUtf()))
            );

    /**
     * 获取品质
     */
    public EnumQuality quality() {
        return quality;
    }

    /**
     * 创建默认品质组件（普通品质）
     */
    public static QualityComponent getDefault() {
        return new QualityComponent(EnumQuality.AVERAGE);
    }

    /**
     * 创建指定品质的组件
     */
    public static QualityComponent of(EnumQuality quality) {
        return new QualityComponent(quality);
    }

    /**
     * 创建指定品质名称的组件
     */
    public static QualityComponent of(String qualityName) {
        return new QualityComponent(EnumQuality.fromName(qualityName));
    }
}