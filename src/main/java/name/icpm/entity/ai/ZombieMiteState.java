package name.icpm.entity.ai;

import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.WeakHashMap;

/**
 * 僵尸 MITE 状态表（与 ICPMLivestock 的 LivestockState 同范式，用 WeakHashMap 跨类共享）。
 *
 * <p>R196 EntityZombie 的 {@code is_smart}（聪明）与首领（leader）标记在此持久化，
 * 由 {@link ZombieMiteMixin} 在 finalizeSpawn / 受玩家伤害时写入，供各 AI Goal 读取。
 * 不挂玩家 NBT（避免 ValueInput/ValueOutput 在 1.21.11 的坑），实体重建即重置，符合 MITE 原版语义。
 */
public final class ZombieMiteState {

    private static final WeakHashMap<Zombie, Entry> MAP = new WeakHashMap<>();

    private ZombieMiteState() {
    }

    public static Entry get(Zombie zombie) {
        return MAP.computeIfAbsent(zombie, k -> new Entry());
    }

    /** 僵尸移除时清理（避免弱引用泄漏；WeakHashMap 本身会自动回收，这里只是显式保险） */
    public static void clear(Zombie zombie) {
        MAP.remove(zombie);
    }

    public static final class Entry {
        /** 聪明：1/8 天生概率，或受玩家伤害后置 true。聪明僵尸会挖开路上的方块。 */
        public boolean smart = false;
        /** 首领：随区块张力概率成为首领，获得额外最大生命与增援。 */
        public boolean leader = false;
    }
}
