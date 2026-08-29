package name.icpm.client.gui

import name.icpm.inventory.MetalAnvilMenu
import name.icpm.network.AnvilRenamePacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/**
 * 金属砧界面
 *
 * 使用类似原版铁砧的UI纹理。移植 R196 GuiRepair：
 * - 命名框（EditBox）：输入实时发送 AnvilRenamePacket → 服务端更新命名并重算结果（纯命名/修复+命名）
 * - 砧耐久条：显示 TileEntityMetalAnvil.damage / maxDurability（仅 ICPM 金属砧；原版铁砧不显示）
 * - 砧损坏阶段叠加层（chipped/damaged 纹理）
 */
class MetalAnvilScreen(
    menu: MetalAnvilMenu,
    private val playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MetalAnvilMenu>(menu, playerInventory, title), MenuAccess<MetalAnvilMenu> {

    companion object {
        // 使用原版铁砧纹理
        private val TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/anvil.png")
    }

    /** 命名框（R196 GuiRepair.itemNameField） */
    private var nameField: EditBox? = null

    override fun init() {
        super.init()
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        val field = EditBox(font, x + 62, y + 24, 103, 12, Component.literal(""))
        field.setMaxLength(40)
        field.setBordered(false) // anvil 纹理自带命名框背景
        field.setCanLoseFocus(true)
        nameField = field
        addRenderableWidget(field)
        // 先同步初始文字（此时尚未设置 responder，不会误发包）
        syncNameFieldFromSlot()
        // 命名框内容变化 → 服务端更新命名并重算结果（R196 MC|ItemName）
        field.setResponder { text ->
            ClientPlayNetworking.send(AnvilRenamePacket(text))
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    /**
     * 键盘输入优先交给命名框处理（未聚焦时 EditBox 返回 false，走原版逻辑）。
     * 避免在命名框输入字母/数字时误触发容器快捷键（如 E 关闭、数字键切快捷栏）。
     */
    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        val field = nameField
        if (field != null && field.keyPressed(keyEvent)) {
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        val field = nameField
        if (field != null && field.charTyped(characterEvent)) {
            return true
        }
        return super.charTyped(characterEvent)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // 渲染背景
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256)

        // 根据砧的损坏阶段渲染损坏纹理
        val damageStage = menu.getAnvilDamageStage()
        if (damageStage > 0) {
            // 渲染损坏叠加层
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x + 2,
                y + 16,
                0.0f,
                (166 + (damageStage - 1) * 28).toFloat(),
                imageWidth - 4,
                28,
                256,
                256
            )
        }

        // 命名框文字同步（换材料时刷新；用户正在编辑时不动）
        syncNameFieldFromSlot()

        // 绘制砧耐久条
        renderAnvilDurability(guiGraphics, x, y)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // 标题
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false)

        // 玩家背包标题
        guiGraphics.drawString(font, playerInventory.displayName, 8, imageHeight - 96 + 2, 0x404040, false)
    }

    /**
     * 命名框文字与输入槽（槽 0）物品显示名同步（仅未聚焦时自动刷新）。
     */
    private fun syncNameFieldFromSlot() {
        val field = nameField ?: return
        val slot = menu.slots.getOrNull(0)
        val name = if (slot != null && slot.hasItem()) slot.item.getHoverName().string else ""
        if (!field.isFocused && field.value != name) {
            field.setValue(name)
        }
    }

    /**
     * 绘制砧耐久条（R196 TileEntityAnvil.damage / BlockAnvil.getDurability）。
     * 仅 ICPM 金属砧显示；原版铁砧（menu.getAnvilDamage()==-1）不显示。
     */
    private fun renderAnvilDurability(guiGraphics: GuiGraphics, x: Int, y: Int) {
        val dmg = menu.getAnvilDamage()
        val maxD = menu.getAnvilMaxDurability()
        if (dmg < 0 || maxD <= 0) return

        val barX = x + 8
        val barY = y + 60
        val barW = 160
        val barH = 4
        val ratio = (1f - dmg.toFloat() / maxD.toFloat()).coerceIn(0f, 1f)

        // 边框 + 背景
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF3A3A3A.toInt())
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF101010.toInt())
        // 剩余耐久进度（绿 → 黄 → 红）
        if (ratio > 0f) {
            val color = when {
                ratio > 0.5f -> 0xFF55FF55.toInt()
                ratio > 0.2f -> 0xFFFFFF55.toInt()
                else -> 0xFFFF5555.toInt()
            }
            guiGraphics.fill(barX, barY, barX + (barW * ratio).toInt().coerceAtLeast(1), barY + barH, color)
        }
        val remaining = (maxD - dmg).coerceAtLeast(0)
        guiGraphics.drawString(font, "砧耐久: $remaining/$maxD", x + 8, y + 66, 0x404040, false)
    }
}
