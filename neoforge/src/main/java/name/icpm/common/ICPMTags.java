package name.icpm.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class ICPMTags {
    public static final TagKey<Block> BURNING_BLOCKS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("icpm", "burning_blocks")
    );
}
