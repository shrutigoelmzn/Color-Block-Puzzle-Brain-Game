package com.example.domain.model

data class Cell(
    val colorId: Int,
    val isClearing: Boolean = false
)

data class Board(
    val size: Int = 8,
    val grid: List<List<Cell?>> = List(8) { List(8) { null } }
) {
    fun get(row: Int, col: Int): Cell? {
        if (row !in 0 until size || col !in 0 until size) return null
        return grid[row][col]
    }

    fun isOccupied(row: Int, col: Int): Boolean = get(row, col) != null

    fun isEmpty(row: Int, col: Int): Boolean = get(row, col) == null

    fun withCell(row: Int, col: Int, cell: Cell?): Board {
        val newGrid = grid.mapIndexed { r, rowList ->
            if (r == row) {
                rowList.mapIndexed { c, currentCell ->
                    if (c == col) cell else currentCell
                }
            } else {
                rowList
            }
        }
        return copy(grid = newGrid)
    }

    fun countOccupied(): Int {
        var count = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] != null) count++
            }
        }
        return count
    }

    companion object {
        fun empty(size: Int = 8): Board = Board(size = size, grid = List(size) { List(size) { null } })
    }
}
