package name.icpm.entity.monster

/**
 * 土元素材质变体（R196 getType 映射）。
 * PLANK（木，id=5）无 magma 变体且受火焰伤害；其余材质在熔岩态有对应 magma 贴图。
 */
enum class EarthElementalType(val id: Int) {
    STONE(0),
    CLAY(1),
    CLAY_HARDENED(2),
    END_STONE(3),
    NETHERRACK(4),
    PLANK(5),
    OBSIDIAN(6);

    companion object {
        fun fromId(id: Int): EarthElementalType = entries.firstOrNull { it.id == id } ?: STONE
    }
}