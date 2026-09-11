package name.icpm.common;

/**
 * ICPM 共享的 ThreadLocal 工具类
 * 用于在不同 Mixin 之间传递临时数据
 * 避免 Mixin 私有静态方法无法跨类访问的问题
 */
public final class ICPMMixinShared {

    private ICPMMixinShared() {}

    // 工具破坏方块待处理耐久消耗
    private static final ThreadLocal<Integer> PENDING_BREAK_COST = new ThreadLocal<>();

    public static void setPendingBreakCost(int cost) {
        PENDING_BREAK_COST.set(cost);
    }

    public static Integer getPendingBreakCost() {
        return PENDING_BREAK_COST.get();
    }

    public static void clearPendingBreakCost() {
        PENDING_BREAK_COST.remove();
    }
}
