package name.icpm.client.hud

import name.icpm.component.NutritionComponent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.max

/**
 * ICPM 营养值 HUD 渲染器
 *
 * 绘制在快捷栏正上方、左右两侧：
 *  - 左侧：蛋白质（条从屏幕左边缘延伸到快捷栏左边缘，文本在条上方左对齐）
 *  - 右侧：植物素（条从快捷栏右边缘延伸到屏幕右边缘，文本在条上方右对齐）
 *  - 显示格式：current/MAX (pct%)
 *
 * 最大值固定为 NutritionComponent.MAX（当前为 160000），不参与实际运算的参考值不会使用。
 */
object NutritionHUD {

    private const val MAX_NUTRITION = NutritionComponent.MAX

    // 颜色定义（AARRGGBB）
    private const val COLOR_PROTEIN = 0xFFC2762E.toInt()      // 蛋白质：棕橙色（贴近参考图）
    private const val COLOR_PHYTO = 0xFF55AA2A.toInt()        // 植物素：绿色
    private const val COLOR_BG = 0xB0000000.toInt()           // 半透明黑色背景

    // 布局
    private const val BAR_HEIGHT = 5
    private const val HOTBAR_WIDTH = 182                      // 原版快捷栏宽度
    private const val HOTBAR_TOP_OFFSET = 22                  // 快捷栏上缘距屏幕底边（快捷栏高 22）
    private const val BAR_ABOVE_HOTBAR = 2                    // 条与快捷栏的间距
    private const val MARGIN_SCREEN = 4                       // 条到屏幕左右边缘的最小距离
    private const val TEXT_ABOVE_BAR = 10                     // 文本基线到条顶部的距离

    private var clientNutrition: NutritionComponent = NutritionComponent.DEFAULT

    fun setClientNutrition(nutrition: NutritionComponent) {
        clientNutrition = nutrition
    }

    fun render(guiGraphics: GuiGraphics, screenWidth: Int, screenHeight: Int) {
        val protein = clientNutrition.protein
        val phyto = clientNutrition.phytonutrients

        val hotbarLeft = (screenWidth - HOTBAR_WIDTH) / 2
        val hotbarRight = hotbarLeft + HOTBAR_WIDTH
        // 条下缘紧贴快捷栏上方，不与经验条重叠
        val barY = screenHeight - HOTBAR_TOP_OFFSET - BAR_ABOVE_HOTBAR - BAR_HEIGHT
        val textY = barY - TEXT_ABOVE_BAR

        val font = Minecraft.getInstance().font

        // 左侧：蛋白质
        val proteinPct = protein * 100 / MAX_NUTRITION
        val proteinText = "$protein/$MAX_NUTRITION ($proteinPct%)"
        drawBarWithTopText(
            guiGraphics,
            barY,
            textY,
            left = MARGIN_SCREEN,
            right = hotbarLeft - MARGIN_SCREEN,
            value = protein,
            color = COLOR_PROTEIN,
            text = proteinText,
            alignRight = false
        )

        // 右侧：植物素
        val phytoPct = phyto * 100 / MAX_NUTRITION
        val phytoText = "$phyto/$MAX_NUTRITION ($phytoPct%)"
        drawBarWithTopText(
            guiGraphics,
            barY,
            textY,
            left = hotbarRight + MARGIN_SCREEN,
            right = screenWidth - MARGIN_SCREEN,
            value = phyto,
            color = COLOR_PHYTO,
            text = phytoText,
            alignRight = true
        )
    }

    /** 条占满指定区域，文本绘制在条上方 */
    private fun drawBarWithTopText(
        guiGraphics: GuiGraphics,
        barY: Int,
        textY: Int,
        left: Int,
        right: Int,
        value: Int,
        color: Int,
        text: String,
        alignRight: Boolean
    ) {
        val font = Minecraft.getInstance().font
        val width = max(0, right - left)
        if (width <= 0) return

        // 条（占满全部可用宽度）
        drawBar(guiGraphics, left, barY, width, value, color)

        // 文本在条上方
        val textWidth = font.width(text)
        val x = if (alignRight) right - textWidth else left
        guiGraphics.drawString(font, text, x, textY, color, true)
    }

    private fun drawBar(
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        value: Int,
        color: Int
    ) {
        val clampedValue = value.coerceAtMost(MAX_NUTRITION)
        val pct = (clampedValue * 100 / MAX_NUTRITION).coerceIn(0, 100)
        val fillWidth = (width * pct / 100).coerceIn(0, width)

        // 背景
        guiGraphics.fill(x, y, x + width, y + BAR_HEIGHT, COLOR_BG)
        // 填充
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, color)
        }
    }
}
