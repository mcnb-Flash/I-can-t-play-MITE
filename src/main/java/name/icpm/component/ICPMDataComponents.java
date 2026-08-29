package name.icpm.component;

import name.icpm.ICPM;
import net.minecraft.core.component.DataComponentType;

/**
 * ICPM 数据组件引用
 * 实际注册在 ICPM.java 中，此处仅提供便捷引用
 */
public class ICPMDataComponents {
    public static final DataComponentType<QualityComponent> QUALITY = ICPM.QUALITY_COMPONENT;
    public static final DataComponentType<CraftPreviewComponent> CRAFT_PREVIEW = ICPM.CRAFT_PREVIEW_COMPONENT;
}
