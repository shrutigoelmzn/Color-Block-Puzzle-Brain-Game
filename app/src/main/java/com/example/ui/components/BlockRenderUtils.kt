package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.BlockThemeColors

object BlockRenderUtils {

    /**
     * Draws a tactile, glossy 3D block with top highlights, bottom shadow,
     * rounded corners, and soft inner bevel.
     */
    fun drawBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float = size.width * 0.18f,
        alpha: Float = 1.0f,
        scale: Float = 1.0f
    ) {
        if (scale <= 0.01f || alpha <= 0.01f) return

        val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
        val w = size.width * scale
        val h = size.height * scale
        val actualTopLeft = Offset(center.x - w / 2f, center.y - h / 2f)
        val inset = w * 0.04f

        val rect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, h - inset * 2)
        )
        val roundRect = RoundRect(rect, CornerRadius(cornerRadius * scale, cornerRadius * scale))

        // 1. Dark bottom shadow / border
        val shadowPath = Path().apply { addRoundRect(roundRect) }
        drawScope.drawPath(
            path = shadowPath,
            color = colors.dark.copy(alpha = alpha),
            style = Fill
        )

        // 2. Main Face gradient
        val faceHeight = (h - inset * 2) * 0.90f
        val faceRect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, faceHeight)
        )
        val faceRoundRect = RoundRect(faceRect, CornerRadius(cornerRadius * scale, cornerRadius * scale))
        val facePath = Path().apply { addRoundRect(faceRoundRect) }

        val faceBrush = Brush.verticalGradient(
            colors = listOf(
                colors.light.copy(alpha = alpha),
                colors.main.copy(alpha = alpha)
            ),
            startY = actualTopLeft.y,
            endY = actualTopLeft.y + h
        )
        drawScope.drawPath(path = facePath, brush = faceBrush)

        // 3. Top Glossy Highlight pill
        val highlightW = (w - inset * 2) * 0.70f
        val highlightH = (h - inset * 2) * 0.18f
        val highlightX = actualTopLeft.x + inset + ((w - inset * 2) - highlightW) / 2f
        val highlightY = actualTopLeft.y + inset + (h * 0.08f)
        val highlightRect = Rect(Offset(highlightX, highlightY), Size(highlightW, highlightH))
        val highlightPath = Path().apply {
            addRoundRect(RoundRect(highlightRect, CornerRadius(highlightH / 2, highlightH / 2)))
        }
        drawScope.drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.45f * alpha)
        )

        // 4. Subtle crisp border outline
        drawScope.drawPath(
            path = shadowPath,
            color = colors.light.copy(alpha = 0.35f * alpha),
            style = Stroke(width = w * 0.035f)
        )
    }

    /**
     * Draws an empty grid slot on the board
     */
    fun drawEmptySlot(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        slotBg: Color,
        borderColor: Color,
        cornerRadius: Float = size.width * 0.14f
    ) {
        val inset = size.width * 0.06f
        val rect = Rect(
            offset = Offset(topLeft.x + inset, topLeft.y + inset),
            size = Size(size.width - inset * 2, size.height - inset * 2)
        )
        val roundRect = RoundRect(rect, CornerRadius(cornerRadius, cornerRadius))
        val path = Path().apply { addRoundRect(roundRect) }

        drawScope.drawPath(path = path, color = slotBg, style = Fill)
        drawScope.drawPath(
            path = path,
            color = borderColor.copy(alpha = 0.5f),
            style = Stroke(width = size.width * 0.03f)
        )
    }

    /**
     * Draws a preview cell for dragging or hints
     */
    fun drawPreviewCell(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        isValid: Boolean,
        pulseAlpha: Float = 0.75f
    ) {
        val inset = size.width * 0.05f
        val rect = Rect(
            offset = Offset(topLeft.x + inset, topLeft.y + inset),
            size = Size(size.width - inset * 2, size.height - inset * 2)
        )
        val roundRect = RoundRect(rect, CornerRadius(size.width * 0.18f, size.width * 0.18f))
        val path = Path().apply { addRoundRect(roundRect) }

        if (isValid) {
            drawScope.drawPath(
                path = path,
                color = colors.main.copy(alpha = 0.40f * pulseAlpha),
                style = Fill
            )
            drawScope.drawPath(
                path = path,
                color = colors.light.copy(alpha = pulseAlpha),
                style = Stroke(width = size.width * 0.07f)
            )
        } else {
            drawScope.drawPath(
                path = path,
                color = Color(0x66FF1744),
                style = Fill
            )
            drawScope.drawPath(
                path = path,
                color = Color(0xFFFF5252),
                style = Stroke(width = size.width * 0.07f)
            )
        }
    }
}
