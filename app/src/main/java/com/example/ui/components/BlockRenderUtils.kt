package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.BlockStyle
import com.example.domain.model.BlockThemeColors

object BlockRenderUtils {

    /**
     * Draws a tactile block according to the specified [BlockStyle] (e.g. GLOSSY, WOODEN,
     * FROSTED_GLASS, CYBER_NEON, STONE, GOLDEN, JEWEL).
     */
    fun drawBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        style: BlockStyle = BlockStyle.GLOSSY,
        cornerRadius: Float = size.width * 0.18f,
        alpha: Float = 1.0f,
        scale: Float = 1.0f
    ) {
        if (scale <= 0.01f || alpha <= 0.01f) return

        when (style) {
            BlockStyle.WOODEN -> drawWoodenBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
            BlockStyle.FROSTED_GLASS -> drawGlassBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
            BlockStyle.CYBER_NEON -> drawNeonBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
            BlockStyle.STONE -> drawStoneBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
            BlockStyle.GOLDEN -> drawGoldenBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
            BlockStyle.JEWEL, BlockStyle.GLOSSY -> drawGlossyBlock(drawScope, topLeft, size, colors, cornerRadius, alpha, scale)
        }
    }

    /**
     * 1. GLOSSY / JEWEL 3D Block: Rich top highlights, bottom shadow, rounded bevel
     */
    private fun drawGlossyBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
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

        // Dark bottom shadow / border
        val shadowPath = Path().apply { addRoundRect(roundRect) }
        drawScope.drawPath(
            path = shadowPath,
            color = colors.dark.copy(alpha = alpha),
            style = Fill
        )

        // Main Face gradient
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

        // Top Glossy Highlight pill
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

        // Crisp border outline
        drawScope.drawPath(
            path = shadowPath,
            color = colors.light.copy(alpha = 0.35f * alpha),
            style = Stroke(width = w * 0.035f)
        )
    }

    /**
     * 2. WOODEN BOX Block: Realistic handcrafted wood crate styling with border frame,
     * wooden plank grain lines, and corner nail/rivet accents.
     */
    private fun drawWoodenBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
        val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
        val w = size.width * scale
        val h = size.height * scale
        val actualTopLeft = Offset(center.x - w / 2f, center.y - h / 2f)
        val inset = w * 0.04f

        val cr = (w * 0.12f)
        val rect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, h - inset * 2)
        )
        val roundRect = RoundRect(rect, CornerRadius(cr, cr))
        val mainPath = Path().apply { addRoundRect(roundRect) }

        // 1. Dark drop shadow base
        drawScope.drawPath(
            path = mainPath,
            color = colors.dark.copy(alpha = alpha),
            style = Fill
        )

        // 2. Wood plank face
        val faceInset = inset + w * 0.04f
        val innerRect = Rect(
            offset = Offset(actualTopLeft.x + faceInset, actualTopLeft.y + faceInset),
            size = Size(w - faceInset * 2, h - faceInset * 2)
        )
        val innerPath = Path().apply {
            addRoundRect(RoundRect(innerRect, CornerRadius(cr * 0.7f, cr * 0.7f)))
        }

        // Natural warm wood gradient
        val woodBrush = Brush.verticalGradient(
            colors = listOf(
                colors.light.copy(alpha = alpha),
                colors.main.copy(alpha = alpha),
                colors.dark.copy(alpha = alpha * 0.95f)
            ),
            startY = actualTopLeft.y,
            endY = actualTopLeft.y + h
        )
        drawScope.drawPath(path = innerPath, brush = woodBrush)

        // 3. Wooden plank seams / horizontal grain grooves
        val plankY1 = innerRect.top + innerRect.height * 0.33f
        val plankY2 = innerRect.top + innerRect.height * 0.66f
        val grainColorDark = colors.dark.copy(alpha = 0.55f * alpha)
        val grainColorLight = colors.light.copy(alpha = 0.35f * alpha)

        drawScope.drawLine(
            color = grainColorDark,
            start = Offset(innerRect.left + 2f, plankY1),
            end = Offset(innerRect.right - 2f, plankY1),
            strokeWidth = w * 0.035f
        )
        drawScope.drawLine(
            color = grainColorLight,
            start = Offset(innerRect.left + 2f, plankY1 + (w * 0.03f)),
            end = Offset(innerRect.right - 2f, plankY1 + (w * 0.03f)),
            strokeWidth = w * 0.02f
        )

        drawScope.drawLine(
            color = grainColorDark,
            start = Offset(innerRect.left + 2f, plankY2),
            end = Offset(innerRect.right - 2f, plankY2),
            strokeWidth = w * 0.035f
        )
        drawScope.drawLine(
            color = grainColorLight,
            start = Offset(innerRect.left + 2f, plankY2 + (w * 0.03f)),
            end = Offset(innerRect.right - 2f, plankY2 + (w * 0.03f)),
            strokeWidth = w * 0.02f
        )

        // 4. Wooden crate border frame
        drawScope.drawPath(
            path = mainPath,
            color = colors.dark.copy(alpha = 0.85f * alpha),
            style = Stroke(width = w * 0.06f)
        )

        // 5. Four brass corner rivet / nail dots
        val rivetRadius = w * 0.045f
        val rivetInset = faceInset + rivetRadius + 1f
        val rivetColor = Color(0xFFFFD54F).copy(alpha = 0.75f * alpha)
        val rivetCenterShadow = colors.dark.copy(alpha = 0.8f * alpha)

        val corners = listOf(
            Offset(actualTopLeft.x + rivetInset, actualTopLeft.y + rivetInset),
            Offset(actualTopLeft.x + w - rivetInset, actualTopLeft.y + rivetInset),
            Offset(actualTopLeft.x + rivetInset, actualTopLeft.y + h - rivetInset),
            Offset(actualTopLeft.x + w - rivetInset, actualTopLeft.y + h - rivetInset)
        )
        for (corner in corners) {
            drawScope.drawCircle(color = rivetColor, radius = rivetRadius, center = corner)
            drawScope.drawCircle(color = rivetCenterShadow, radius = rivetRadius * 0.45f, center = corner)
        }
    }

    /**
     * 3. FROSTED GLASS Block: Translucent crystalline look with bright internal refraction,
     * crisp glass border, and diagonal specular gleam.
     */
    private fun drawGlassBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
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
        val mainPath = Path().apply { addRoundRect(roundRect) }

        // 1. Semi-translucent colored glass base
        val glassBrush = Brush.linearGradient(
            colors = listOf(
                colors.light.copy(alpha = 0.75f * alpha),
                colors.main.copy(alpha = 0.60f * alpha),
                colors.dark.copy(alpha = 0.80f * alpha)
            ),
            start = Offset(actualTopLeft.x, actualTopLeft.y),
            end = Offset(actualTopLeft.x + w, actualTopLeft.y + h)
        )
        drawScope.drawPath(path = mainPath, brush = glassBrush)

        // 2. Diagonal sharp crystal specular gleam
        val gleamPath = Path().apply {
            moveTo(actualTopLeft.x + inset + w * 0.15f, actualTopLeft.y + inset)
            lineTo(actualTopLeft.x + inset + w * 0.45f, actualTopLeft.y + inset)
            lineTo(actualTopLeft.x + inset, actualTopLeft.y + inset + h * 0.45f)
            lineTo(actualTopLeft.x + inset, actualTopLeft.y + inset + h * 0.15f)
            close()
        }
        drawScope.drawPath(
            path = gleamPath,
            color = Color.White.copy(alpha = 0.50f * alpha)
        )

        // 3. Inner refractive glow ring
        val innerInset = inset + w * 0.06f
        val innerRect = Rect(
            offset = Offset(actualTopLeft.x + innerInset, actualTopLeft.y + innerInset),
            size = Size(w - innerInset * 2, h - innerInset * 2)
        )
        val innerPath = Path().apply {
            addRoundRect(RoundRect(innerRect, CornerRadius(cornerRadius * 0.7f * scale, cornerRadius * 0.7f * scale)))
        }
        drawScope.drawPath(
            path = innerPath,
            color = Color.White.copy(alpha = 0.20f * alpha),
            style = Stroke(width = w * 0.04f)
        )

        // 4. Crisp high-clarity outer border
        drawScope.drawPath(
            path = mainPath,
            color = Color.White.copy(alpha = 0.65f * alpha),
            style = Stroke(width = w * 0.04f)
        )
    }

    /**
     * 4. CYBER NEON Block: Dark hi-tech core with glowing neon laser borders,
     * high-contrast electric edges, and pulsing neon outline.
     */
    private fun drawNeonBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
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
        val mainPath = Path().apply { addRoundRect(roundRect) }

        // 1. Dark cyber matrix background core
        drawScope.drawPath(
            path = mainPath,
            color = Color(0xFF0D0B18).copy(alpha = alpha),
            style = Fill
        )

        // 2. Electric center radial tint
        val coreBrush = Brush.radialGradient(
            colors = listOf(
                colors.main.copy(alpha = 0.45f * alpha),
                Color.Transparent
            ),
            center = center,
            radius = w * 0.55f
        )
        drawScope.drawPath(path = mainPath, brush = coreBrush)

        // 3. Thick soft outer neon glow
        drawScope.drawPath(
            path = mainPath,
            color = colors.main.copy(alpha = 0.40f * alpha),
            style = Stroke(width = w * 0.10f)
        )

        // 4. Bright vivid neon laser stroke
        drawScope.drawPath(
            path = mainPath,
            color = colors.main.copy(alpha = 0.95f * alpha),
            style = Stroke(width = w * 0.045f)
        )

        // 5. Pure white energetic highlight core inside the stroke
        drawScope.drawPath(
            path = mainPath,
            color = Color.White.copy(alpha = 0.60f * alpha),
            style = Stroke(width = w * 0.018f)
        )

        // 6. Corner tech crosshairs/notches
        val notchLen = w * 0.16f
        val notchStroke = w * 0.035f
        val notchColor = colors.light.copy(alpha = 0.85f * alpha)
        // Top-left
        drawScope.drawLine(notchColor, Offset(actualTopLeft.x + inset, actualTopLeft.y + inset + notchLen), Offset(actualTopLeft.x + inset, actualTopLeft.y + inset), notchStroke, StrokeCap.Round)
        drawScope.drawLine(notchColor, Offset(actualTopLeft.x + inset, actualTopLeft.y + inset), Offset(actualTopLeft.x + inset + notchLen, actualTopLeft.y + inset), notchStroke, StrokeCap.Round)
        // Bottom-right
        drawScope.drawLine(notchColor, Offset(actualTopLeft.x + w - inset - notchLen, actualTopLeft.y + h - inset), Offset(actualTopLeft.x + w - inset, actualTopLeft.y + h - inset), notchStroke, StrokeCap.Round)
        drawScope.drawLine(notchColor, Offset(actualTopLeft.x + w - inset, actualTopLeft.y + h - inset), Offset(actualTopLeft.x + w - inset, actualTopLeft.y + h - inset - notchLen), notchStroke, StrokeCap.Round)
    }

    /**
     * 5. ANCIENT STONE Block: Heavy chiseled mineral stone with beveled masonry borders,
     * rugged textured flecks, and rock carved bevel.
     */
    private fun drawStoneBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
        val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
        val w = size.width * scale
        val h = size.height * scale
        val actualTopLeft = Offset(center.x - w / 2f, center.y - h / 2f)
        val inset = w * 0.04f

        val stoneCr = cornerRadius * 0.5f * scale
        val rect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, h - inset * 2)
        )
        val mainPath = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(stoneCr, stoneCr))) }

        // 1. Deep chiseled shadow base
        drawScope.drawPath(
            path = mainPath,
            color = colors.dark.copy(alpha = alpha),
            style = Fill
        )

        // 2. Chiseled face with matte texture
        val faceHeight = (h - inset * 2) * 0.88f
        val faceRect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, faceHeight)
        )
        val facePath = Path().apply { addRoundRect(RoundRect(faceRect, CornerRadius(stoneCr, stoneCr))) }
        val stoneBrush = Brush.verticalGradient(
            colors = listOf(
                colors.light.copy(alpha = alpha),
                colors.main.copy(alpha = alpha)
            ),
            startY = actualTopLeft.y,
            endY = actualTopLeft.y + h
        )
        drawScope.drawPath(path = facePath, brush = stoneBrush)

        // 3. Inner chiseled bevel groove
        val bevelInset = inset + w * 0.08f
        val bevelRect = Rect(
            offset = Offset(actualTopLeft.x + bevelInset, actualTopLeft.y + bevelInset),
            size = Size(w - bevelInset * 2, h - bevelInset * 2)
        )
        val bevelPath = Path().apply { addRoundRect(RoundRect(bevelRect, CornerRadius(stoneCr * 0.6f, stoneCr * 0.6f))) }
        drawScope.drawPath(
            path = bevelPath,
            color = colors.dark.copy(alpha = 0.40f * alpha),
            style = Stroke(width = w * 0.035f)
        )

        // 4. Subtle masonry fleck highlights (rough stone minerals)
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.35f * alpha),
            radius = w * 0.035f,
            center = Offset(actualTopLeft.x + w * 0.32f, actualTopLeft.y + h * 0.30f)
        )
        drawScope.drawCircle(
            color = Color.Black.copy(alpha = 0.25f * alpha),
            radius = w * 0.025f,
            center = Offset(actualTopLeft.x + w * 0.68f, actualTopLeft.y + h * 0.55f)
        )
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.25f * alpha),
            radius = w * 0.03f,
            center = Offset(actualTopLeft.x + w * 0.45f, actualTopLeft.y + h * 0.72f)
        )

        // 5. Heavy stone masonry outline
        drawScope.drawPath(
            path = mainPath,
            color = colors.dark.copy(alpha = 0.85f * alpha),
            style = Stroke(width = w * 0.05f)
        )
    }

    /**
     * 6. GOLDEN INGOT Block: Luxurious metallic gold bar with beveled facets,
     * diagonal luster sheen, and radiant rim illumination.
     */
    private fun drawGoldenBlock(
        drawScope: DrawScope,
        topLeft: Offset,
        size: Size,
        colors: BlockThemeColors,
        cornerRadius: Float,
        alpha: Float,
        scale: Float
    ) {
        val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
        val w = size.width * scale
        val h = size.height * scale
        val actualTopLeft = Offset(center.x - w / 2f, center.y - h / 2f)
        val inset = w * 0.04f

        val cr = (w * 0.15f)
        val rect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, h - inset * 2)
        )
        val mainPath = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(cr, cr))) }

        // 1. Dark bronze/gold shadow foundation
        drawScope.drawPath(
            path = mainPath,
            color = Color(0xFF78350F).copy(alpha = alpha),
            style = Fill
        )

        // 2. Metallic bullion multi-stop gradient face
        val goldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = alpha),
                Color(0xFFFFD54F).copy(alpha = alpha),
                Color(0xFFFFA000).copy(alpha = alpha),
                Color(0xFFFFE082).copy(alpha = alpha),
                Color(0xFFD97706).copy(alpha = alpha)
            ),
            start = Offset(actualTopLeft.x, actualTopLeft.y),
            end = Offset(actualTopLeft.x + w, actualTopLeft.y + h)
        )
        val faceHeight = (h - inset * 2) * 0.90f
        val faceRect = Rect(
            offset = Offset(actualTopLeft.x + inset, actualTopLeft.y + inset),
            size = Size(w - inset * 2, faceHeight)
        )
        val facePath = Path().apply { addRoundRect(RoundRect(faceRect, CornerRadius(cr, cr))) }
        drawScope.drawPath(path = facePath, brush = goldBrush)

        // 3. Diagonal brilliant gold luster highlight
        val shineW = (w - inset * 2) * 0.65f
        val shineH = (h - inset * 2) * 0.18f
        val shinePath = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(
                        offset = Offset(actualTopLeft.x + inset + (w * 0.12f), actualTopLeft.y + inset + (h * 0.08f)),
                        size = Size(shineW, shineH)
                    ),
                    CornerRadius(shineH / 2, shineH / 2)
                )
            )
        }
        drawScope.drawPath(
            path = shinePath,
            color = Color.White.copy(alpha = 0.65f * alpha)
        )

        // 4. Ingot stamp center diamond emboss
        val embossSize = w * 0.22f
        val stampPath = Path().apply {
            moveTo(center.x, center.y - embossSize / 2f)
            lineTo(center.x + embossSize / 2f, center.y)
            lineTo(center.x, center.y + embossSize / 2f)
            lineTo(center.x - embossSize / 2f, center.y)
            close()
        }
        drawScope.drawPath(
            path = stampPath,
            color = Color(0xFF78350F).copy(alpha = 0.40f * alpha),
            style = Stroke(width = w * 0.035f)
        )
        drawScope.drawPath(
            path = stampPath,
            color = Color(0xFFFFF9C4).copy(alpha = 0.50f * alpha),
            style = Fill
        )

        // 5. Polished gold rim border
        drawScope.drawPath(
            path = mainPath,
            color = Color(0xFFFFF3B0).copy(alpha = 0.75f * alpha),
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
