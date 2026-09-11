package com.example.heartmatch.engine.model

class Board(
    val rows: Int,
    val cols: Int,
    private val grid: Array<Array<Cell>>
) {
    init {
        require(rows > 0 && cols > 0) { "Board dimensions must be positive ($rows x $cols)" }
        require(grid.size == rows && grid.all { it.size == cols }) { "Grid dimension mismatch" }
    }

    operator fun get(coord: Coord): Cell? {
        if (!isValid(coord)) return null
        return grid[coord.row][coord.col]
    }

    operator fun get(row: Int, col: Int): Cell? = get(Coord(row, col))

    fun isValid(coord: Coord): Boolean {
        return coord.row in 0 until rows && coord.col in 0 until cols
    }

    fun getTile(coord: Coord): Tile? = get(coord)?.tile

    fun setTile(coord: Coord, tile: Tile?) {
        val cell = get(coord) ?: return
        if (cell.isPlayable) {
            cell.tile = tile
        }
    }

    fun swap(from: Coord, to: Coord) {
        val cellFrom = get(from) ?: return
        val cellTo = get(to) ?: return
        val temp = cellFrom.tile
        cellFrom.tile = cellTo.tile
        cellTo.tile = temp
    }

    fun clone(): Board {
        val newGrid = Array(rows) { r ->
            Array(cols) { c ->
                val cell = grid[r][c]
                Cell(
                    coord = Coord(r, c),
                    state = cell.state,
                    tile = cell.tile
                )
            }
        }
        return Board(rows, cols, newGrid)
    }

    fun forEachCell(action: (Cell) -> Unit) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                action(grid[r][c])
            }
        }
    }

    fun getAllPlayableCoords(): List<Coord> {
        val list = mutableListOf<Coord>()
        forEachCell { cell ->
            if (cell.isPlayable) {
                list.add(cell.coord)
            }
        }
        return list
    }

    companion object {
        fun createEmpty(rows: Int, cols: Int): Board {
            val grid = Array(rows) { r ->
                Array(cols) { c ->
                    Cell(Coord(r, c), CellState.PLAYABLE, null)
                }
            }
            return Board(rows, cols, grid)
        }

        fun createWithLayout(
            rows: Int,
            cols: Int,
            cellStates: Array<Array<CellState>>? = null,
            initialTiles: Array<Array<Tile?>>? = null
        ): Board {
            val grid = Array(rows) { r ->
                Array(cols) { c ->
                    val state = cellStates?.getOrNull(r)?.getOrNull(c) ?: CellState.PLAYABLE
                    val tile = if (state == CellState.PLAYABLE) {
                        initialTiles?.getOrNull(r)?.getOrNull(c)
                    } else null
                    Cell(Coord(r, c), state, tile)
                }
            }
            return Board(rows, cols, grid)
        }
    }
}
