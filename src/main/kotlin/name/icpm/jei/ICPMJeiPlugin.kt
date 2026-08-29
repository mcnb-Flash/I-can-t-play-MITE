package name.icpm.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.registration.IRecipeCatalystRegistration
import name.icpm.ICPM
import name.icpm.block.ICPMBlocks
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * JEI 兼容插件：将 ICPM 的工作方块注册为对应配方类别的催化剂，
 * 使 JEI 能识别这些方块并展示其作用：
 *   - 工作台 (_workbench)   -> CRAFTING   （可制作的合成配方）
 *   - 熔炉   (_furnace)     -> SMELTING / BLASTING（可烧炼的物品）
 *   - 金属砧 (_anvil)       -> ANVIL      （可修复 / 重命名的工具）
 *
 * JEI 通过 fabric.mod.json 的 "jei" entrypoint 发现本插件（见 JEI 的 FabricPluginFinder）。
 * 依赖运行时玩家已安装 JEI；若未安装 JEI，本类由 Fabric 惰性加载机制保证不会被加载。
 */
@JeiPlugin
object ICPMJeiPlugin : IModPlugin {

    override fun getPluginUid(): Identifier =
        Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "jei_plugin")

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        for (name in ICPMBlocks.BLOCK_NAMES) {
            val id = Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name)
            val item: Item = BuiltInRegistries.ITEM.getOptional(id).orElse(null) ?: continue
            val stack = ItemStack(item)

            when {
                name.endsWith("_workbench") ->
                    registration.addRecipeCatalyst(stack, RecipeTypes.CRAFTING)
                name.endsWith("_furnace") ->
                    registration.addRecipeCatalyst(stack, RecipeTypes.SMELTING, RecipeTypes.BLASTING)
                name.endsWith("_anvil") ->
                    registration.addRecipeCatalyst(stack, RecipeTypes.ANVIL)
            }
        }
    }
}
