package com.example.domain.engine

import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import com.example.domain.model.Cell

object PlacementEngine {

    fun canPlace(board: Board, shape: BlockShape, startRow: Int, startCol: Int): Boolean {
        // Must fit within board boundary
        if (startRow < 0 || startCol < 0) return false
        if (startRow + shape.height > board.size || startCol + shape.width > board.size) return false

        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.isFilled(r, c)) {
                    val boardRow = startRow + r
                    val boardCol = startCol + c
                    if (board.isOccupied(boardRow, boardCol)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun place(board: Board, shape: BlockShape, startRow: Int, startCol: Int): Board {
        require(canPlace(board, shape, startRow, startCol)) {
            "Cannot place shape ${shape.id} at ($startRow, $startCol)"
        }

        var updatedBoard = board
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.isFilled(r, c)) {
                    val boardRow = startRow + r
                    val boardCol = startCol + c
                    updatedBoard = updatedBoard.withCell(
                        boardRow,
                        boardCol,
                        Cell(colorId = shape.colorId)
                    )
                }
            }
        }
        return updatedBoard
    }

    fun getCandidateCoordinates(shape: BlockShape, startRow: Int, startCol: Int): List<Pair<Int, Int>> {
        val coords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.isFilled(r, c)) {
                    coords.add(Pair(startRow + r, startCol + c))
                }
            }
        }
        return coords
    }
}
