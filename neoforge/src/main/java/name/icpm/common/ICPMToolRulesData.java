package name.icpm.common;

/**
 * ICPM 工具规则数据（从 mixin 包拆出）。
 *
 * NeoForge 下 Sponge Mixin 禁止在 mixin 包（name.icpm.mixin）中定义嵌套类型后被
 * 注入方法引用——织入目标类的字节码会尝试直接加载该嵌套类而抛 IllegalClassLoadError。
 * 故 ToolType/ToolInfo/BlockRequirement 集中放于此普通包，mixin 经 import 引用。
 */
public final class ICPMToolRulesData {

    private ICPMToolRulesData() {
    }

    /** 工具类型枚举 */
    public enum ToolType {
        HAND, PICKAXE, AXE, SHOVEL, HOE
    }

    /** 工具信息记录 */
    public record ToolInfo(ToolType type, float level) {
    }

    /** 方块破坏要求记录 */
    public record BlockRequirement(ToolType toolType, float level, boolean requiresTool) {
    }
}
