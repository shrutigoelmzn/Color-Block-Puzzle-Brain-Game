package com.example.domain.model

data class BlockShape(
    val id: String,
    val name: String,
    val matrix: List<List<Boolean>>,
    val colorId: Int
) {
    val height: Int = matrix.size
    val width: Int = if (matrix.isNotEmpty()) matrix[0].size else 0

    val blockCount: Int = matrix.sumOf { row -> row.count { it } }

    fun isFilled(r: Int, c: Int): Boolean {
        if (r !in 0 until height || c !in 0 until width) return false
        return matrix[r][c]
    }

    companion object {
        // Dot (1x1)
        fun dot(colorId: Int = 0): BlockShape = BlockShape(
            id = "dot_1x1",
            name = "Dot",
            matrix = listOf(listOf(true)),
            colorId = colorId
        )

        // Horizontal line 2
        fun line2H(colorId: Int = 1): BlockShape = BlockShape(
            id = "line_2h",
            name = "Line 2H",
            matrix = listOf(listOf(true, true)),
            colorId = colorId
        )

        // Vertical line 2
        fun line2V(colorId: Int = 1): BlockShape = BlockShape(
            id = "line_2v",
            name = "Line 2V",
            matrix = listOf(listOf(true), listOf(true)),
            colorId = colorId
        )

        // Horizontal line 3
        fun line3H(colorId: Int = 2): BlockShape = BlockShape(
            id = "line_3h",
            name = "Line 3H",
            matrix = listOf(listOf(true, true, true)),
            colorId = colorId
        )

        // Vertical line 3
        fun line3V(colorId: Int = 2): BlockShape = BlockShape(
            id = "line_3v",
            name = "Line 3V",
            matrix = listOf(listOf(true), listOf(true), listOf(true)),
            colorId = colorId
        )

        // Horizontal line 4
        fun line4H(colorId: Int = 3): BlockShape = BlockShape(
            id = "line_4h",
            name = "Line 4H",
            matrix = listOf(listOf(true, true, true, true)),
            colorId = colorId
        )

        // Vertical line 4
        fun line4V(colorId: Int = 3): BlockShape = BlockShape(
            id = "line_4v",
            name = "Line 4V",
            matrix = listOf(listOf(true), listOf(true), listOf(true), listOf(true)),
            colorId = colorId
        )

        // Horizontal line 5
        fun line5H(colorId: Int = 4): BlockShape = BlockShape(
            id = "line_5h",
            name = "Line 5H",
            matrix = listOf(listOf(true, true, true, true, true)),
            colorId = colorId
        )

        // Vertical line 5
        fun line5V(colorId: Int = 4): BlockShape = BlockShape(
            id = "line_5v",
            name = "Line 5V",
            matrix = listOf(listOf(true), listOf(true), listOf(true), listOf(true), listOf(true)),
            colorId = colorId
        )

        // Square 2x2
        fun square2(colorId: Int = 0): BlockShape = BlockShape(
            id = "square_2x2",
            name = "Square 2x2",
            matrix = listOf(
                listOf(true, true),
                listOf(true, true)
            ),
            colorId = colorId
        )

        // Square 3x3
        fun square3(colorId: Int = 5): BlockShape = BlockShape(
            id = "square_3x3",
            name = "Square 3x3",
            matrix = listOf(
                listOf(true, true, true),
                listOf(true, true, true),
                listOf(true, true, true)
            ),
            colorId = colorId
        )

        // Corner 2x2 (4 rotations)
        fun corner2TL(colorId: Int = 2): BlockShape = BlockShape(
            id = "corner_2_tl",
            name = "Small Corner TL",
            matrix = listOf(
                listOf(true, true),
                listOf(true, false)
            ),
            colorId = colorId
        )

        fun corner2TR(colorId: Int = 2): BlockShape = BlockShape(
            id = "corner_2_tr",
            name = "Small Corner TR",
            matrix = listOf(
                listOf(true, true),
                listOf(false, true)
            ),
            colorId = colorId
        )

        fun corner2BL(colorId: Int = 2): BlockShape = BlockShape(
            id = "corner_2_bl",
            name = "Small Corner BL",
            matrix = listOf(
                listOf(true, false),
                listOf(true, true)
            ),
            colorId = colorId
        )

        fun corner2BR(colorId: Int = 2): BlockShape = BlockShape(
            id = "corner_2_br",
            name = "Small Corner BR",
            matrix = listOf(
                listOf(false, true),
                listOf(true, true)
            ),
            colorId = colorId
        )

        // L Shape 3x3 (4 rotations)
        fun lShape3TL(colorId: Int = 3): BlockShape = BlockShape(
            id = "l_3_tl",
            name = "L 3x3 TL",
            matrix = listOf(
                listOf(true, true, true),
                listOf(true, false, false),
                listOf(true, false, false)
            ),
            colorId = colorId
        )

        fun lShape3TR(colorId: Int = 3): BlockShape = BlockShape(
            id = "l_3_tr",
            name = "L 3x3 TR",
            matrix = listOf(
                listOf(true, true, true),
                listOf(false, false, true),
                listOf(false, false, true)
            ),
            colorId = colorId
        )

        fun lShape3BL(colorId: Int = 3): BlockShape = BlockShape(
            id = "l_3_bl",
            name = "L 3x3 BL",
            matrix = listOf(
                listOf(true, false, false),
                listOf(true, false, false),
                listOf(true, true, true)
            ),
            colorId = colorId
        )

        fun lShape3BR(colorId: Int = 3): BlockShape = BlockShape(
            id = "l_3_br",
            name = "L 3x3 BR",
            matrix = listOf(
                listOf(false, false, true),
                listOf(false, false, true),
                listOf(true, true, true)
            ),
            colorId = colorId
        )

        // T Shape (4 rotations)
        fun tShapeUp(colorId: Int = 4): BlockShape = BlockShape(
            id = "t_up",
            name = "T Up",
            matrix = listOf(
                listOf(true, true, true),
                listOf(false, true, false)
            ),
            colorId = colorId
        )

        fun tShapeDown(colorId: Int = 4): BlockShape = BlockShape(
            id = "t_down",
            name = "T Down",
            matrix = listOf(
                listOf(false, true, false),
                listOf(true, true, true)
            ),
            colorId = colorId
        )

        fun tShapeLeft(colorId: Int = 4): BlockShape = BlockShape(
            id = "t_left",
            name = "T Left",
            matrix = listOf(
                listOf(true, false),
                listOf(true, true),
                listOf(true, false)
            ),
            colorId = colorId
        )

        fun tShapeRight(colorId: Int = 4): BlockShape = BlockShape(
            id = "t_right",
            name = "T Right",
            matrix = listOf(
                listOf(false, true),
                listOf(true, true),
                listOf(false, true)
            ),
            colorId = colorId
        )

        // Standard L (3x2 and 2x3)
        fun lStandardBL(colorId: Int = 1): BlockShape = BlockShape(
            id = "l_std_bl",
            name = "L standard BL",
            matrix = listOf(
                listOf(true, false),
                listOf(true, false),
                listOf(true, true)
            ),
            colorId = colorId
        )

        fun lStandardBR(colorId: Int = 1): BlockShape = BlockShape(
            id = "l_std_br",
            name = "L standard BR",
            matrix = listOf(
                listOf(false, true),
                listOf(false, true),
                listOf(true, true)
            ),
            colorId = colorId
        )

        fun lStandardTL(colorId: Int = 1): BlockShape = BlockShape(
            id = "l_std_tl",
            name = "L standard TL",
            matrix = listOf(
                listOf(true, true),
                listOf(true, false),
                listOf(true, false)
            ),
            colorId = colorId
        )

        fun lStandardTR(colorId: Int = 1): BlockShape = BlockShape(
            id = "l_std_tr",
            name = "L standard TR",
            matrix = listOf(
                listOf(true, true),
                listOf(false, true),
                listOf(false, true)
            ),
            colorId = colorId
        )

        // Z and S shapes
        fun zShapeH(colorId: Int = 5): BlockShape = BlockShape(
            id = "z_h",
            name = "Z Horizontal",
            matrix = listOf(
                listOf(true, true, false),
                listOf(false, true, true)
            ),
            colorId = colorId
        )

        fun sShapeH(colorId: Int = 5): BlockShape = BlockShape(
            id = "s_h",
            name = "S Horizontal",
            matrix = listOf(
                listOf(false, true, true),
                listOf(true, true, false)
            ),
            colorId = colorId
        )

        // Cross / Plus (3x3)
        fun plus(colorId: Int = 6): BlockShape = BlockShape(
            id = "plus_cross",
            name = "Plus",
            matrix = listOf(
                listOf(false, true, false),
                listOf(true, true, true),
                listOf(false, true, false)
            ),
            colorId = colorId
        )

        val ALL_SHAPES: List<BlockShape> = listOf(
            dot(),
            line2H(), line2V(),
            line3H(), line3V(),
            line4H(), line4V(),
            line5H(), line5V(),
            square2(), square3(),
            corner2TL(), corner2TR(), corner2BL(), corner2BR(),
            lShape3TL(), lShape3TR(), lShape3BL(), lShape3BR(),
            tShapeUp(), tShapeDown(), tShapeLeft(), tShapeRight(),
            lStandardBL(), lStandardBR(), lStandardTL(), lStandardTR(),
            zShapeH(), sShapeH(),
            plus()
        )
    }
}
