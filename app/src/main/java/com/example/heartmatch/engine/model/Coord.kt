package com.example.heartmatch.engine.model

data class Coord(val row: Int, val col: Int) {
    fun isAdjacentTo(other: Coord): Boolean {
        val dRow = Math.abs(row - other.row)
        val dCol = Math.abs(col - other.col)
        return (dRow == 1 && dCol == 0) || (dRow == 0 && dCol == 1)
    }

    fun orthogonalNeighbors(): List<Coord> {
        return listOf(
            Coord(row - 1, col),
            Coord(row + 1, col),
            Coord(row, col - 1),
            Coord(row, col + 1)
        )
    }

    fun allNeighbors(): List<Coord> {
        return listOf(
            Coord(row - 1, col - 1), Coord(row - 1, col), Coord(row - 1, col + 1),
            Coord(row, col - 1),                         Coord(row, col + 1),
            Coord(row + 1, col - 1), Coord(row + 1, col), Coord(row + 1, col + 1)
        )
    }
}
