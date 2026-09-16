package com.example.domain.engine

import com.example.domain.model.Board

data class CompletedLines(
    val rows: List<Int>,
    val cols: List<Int>
) {
    val totalLines: Int get() = rows.size + cols.size
    val hasClears: Boolean get() = totalLines > 0
}

object LineClearEngine {

    fun findCompletedLines(board: Board): CompletedLines {
        val completedRows = mutableListOf<Int>()
        val completedCols = mutableListOf<Int>()

        // Check horizontal rows
        for (r in 0 until board.size) {
            var full = true
            for (c in 0 until board.size) {
                if (board.isEmpty(r, c)) {
                    full = false
                    break
                }
            }
            if (full) completedRows.add(r)
        }

        // Check vertical columns
        for (c in 0 until board.size) {
            var full = true
            for (r in 0 until board.size) {
                if (board.isEmpty(r, c)) {
                    full = false
                    break
                }
            }
            if (full) completedCols.add(c)
        }

        return CompletedLines(rows = completedRows, cols = completedCols)
    }

    fun clearLines(board: Board, rows: List<Int>, cols: List<Int>): Board {
        if (rows.isEmpty() && cols.isEmpty()) return board

        var newBoard = board
        for (r in rows) {
            for (c in 0 until board.size) {
                newBoard = newBoard.withCell(r, c, null)
            }
        }
        for (c in cols) {
            for (r in 0 until board.size) {
                newBoard = newBoard.withCell(r, c, null)
            }
        }
        return newBoard
    }
}
