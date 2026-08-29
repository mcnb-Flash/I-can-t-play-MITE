package name.icpm.client.gui

import name.icpm.ICPM
import name.icpm.common.EnumQuality
import name.icpm.inventory.ICPMWorkbenchMenu
import name.icpm.network.WorkbenchCraftPacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.MenuAccess
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

/**
 * ICPM 工作台界面
 *
 * 复刻 R196 GuiCrafting 的 UI 元素：
 * - 3x3 合成格 + 结果槽 + 玩家背包
 * - 合成进度条（覆盖结果槽区域，显示合成进度百分比）
 * - 品质指示器（显示当前选中品质，带颜色编码）
 * - 左键结果槽 → 开始合成
 * - 右键结果槽 → 切换品质等级
 *
 * R196 进度条绘制：
 *   drawTexturedModalRect(var4 + 90, var5 + 34, 176, 0,
 *       player.crafting_ticks * 23 / player.crafting_period, 16)
 */
class ICPMWorkbenchScreen(
    menu: ICPMWorkbenchMenu,
    private val playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ICPMWorkbenchMenu>(menu, playerInventory, title), MenuAccess<ICPMWorkbenchMenu> {

    companion object {
        // 使用原版工作台 GUI 纹理（ICPM 命名空间尚无专用纹理）
        private val TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png")
    }

    init {
        imageWidth = 176
        imageHeight = 166
    }

    // ==================== 渲染 ====================

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        // 标准物品提示渲染（自动处理槽位悬停提示）
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        // 绘制背景纹理
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256)

        // 绘制工作台等级标签
        val tier = menu.workbenchTier
        val tierText = "合成等级: $tier"
        guiGraphics.drawString(font, tierText, x + 8, y + 6, 0x404040, false)

        // 绘制品质指示器（结果槽上方）
        val quality = menu.getSelectedQuality()
        val qualityColor = getQualityColor(quality)
        val qualityText = quality.name
        // 品质文字（结果槽上方居中）
        val qualityTextWidth = font.width(qualityText)
        guiGraphics.drawString(font, qualityText,
            x + 124 - qualityTextWidth / 2 + 8, y + 22, qualityColor, false)

        // 绘制合成进度条
        if (menu.isCrafting || menu.isCraftingComplete) {
            val progress = menu.getCraftingProgressFraction()
            val barWidth = (24 * progress).toInt().coerceIn(0, 24)
            val barX = x + 106
            val barY = y + 35

            if (menu.isCraftingComplete) {
                // 合成完成 → 绿色进度条
                guiGraphics.fill(barX, barY, barX + 24, barY + 16, 0x4000FF00)
            } else if (barWidth > 0) {
                // 合成进行中 → 蓝色进度条
                guiGraphics.fill(barX, barY, barX + barWidth, barY + 16, 0x400066FF)
            }

            // 进度百分比文字
            val percentText = "${(progress * 100).toInt()}%"
            val percentWidth = font.width(percentText)
            guiGraphics.drawString(font, percentText,
                x + 124 - percentWidth / 2 + 8, y + 54, 0x808080, false)
        }

        // 冷却提示
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            val player = net.minecraft.client.Minecraft.getInstance().player!!
            if (name.icpm.common.ICPMCraftCooldowns.hasCraftCooldown(player)) {
                val remaining = name.icpm.common.ICPMCraftCooldowns.getCraftCooldownRemaining(player)
                val cdText = "冷却: ${remaining}t"
                guiGraphics.drawString(font, cdText, x + 8, y + imageHeight - 106, 0xCC3333, false)
            }
        }
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, playerInventory.displayName, 8, imageHeight - 96 + 2, 0x404040, false)
    }

    /**
     * 处理鼠标点击
     *
     * 复刻 R196 SlotCrafting.onSlotClicked：
     * - 左键（button 0）→ 开始合成（合成完成时允许原版取走）
     * - 右键（button 1）→ 切换品质
     */
    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val button = mouseButtonEvent.button()
        // 仅当鼠标确实悬停在结果槽上时才拦截自定义合成逻辑（R196 SlotCrafting.onSlotClicked）。
        // 用 hoveredSlot 而非纯坐标判断：JEI 页面点击坐标可能与结果槽区域重叠，
        // 若点击并非真实落在工作台结果槽上，一律交给原版处理，避免误触发 开始合成/取走/切品质。
        if (hoveredSlot === menu.slots.getOrNull(0)) {
            // 合成完成时不拦截，让原版处理取走物品
            if (menu.isCraftingComplete) {
                if (button == 0) {
                    // 左键 → 取走成品
                    ClientPlayNetworking.send(WorkbenchCraftPacket(WorkbenchCraftPacket.Action.TAKE_RESULT))
                    return true
                }
                return super.mouseClicked(mouseButtonEvent, bl)
            }
            if (button == 0) {
                // 左键 → 开始合成
                ClientPlayNetworking.send(WorkbenchCraftPacket(WorkbenchCraftPacket.Action.START_CRAFT))
                return true
            } else if (button == 1) {
                // 右键 → 切换品质
                ClientPlayNetworking.send(WorkbenchCraftPacket(WorkbenchCraftPacket.Action.CYCLE_QUALITY))
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取品质对应的颜色值
     *
     * R196 品质颜色：
     * wretched=深灰, poor=红, average=白, fine=绿,
     * excellent=蓝, superb=紫, masterwork=金, legendary=青
     */
    private fun getQualityColor(quality: EnumQuality): Int {
        return when (quality) {
            EnumQuality.WRETCHED -> 0x555555
            EnumQuality.POOR -> 0xAA0000
            EnumQuality.AVERAGE -> 0xAAAAAA
            EnumQuality.FINE -> 0x00AA00
            EnumQuality.EXCELLENT -> 0x5555FF
            EnumQuality.SUPERB -> 0xAA00AA
            EnumQuality.MASTERWORK -> 0xFFAA00
            EnumQuality.LEGENDARY -> 0x55FFFF
        }
    }
}
