package com.example.blockblast.game

class PlaceResult(
    val points: Int,
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    val combo: Int,
)

class GameEngine(
    private val gridSize: Int = 8,
    private val traySize: Int = 3,
) {
    companion object {
        const val COMBO_WINDOW_MS = 5000L
    }

    val grid = Array(gridSize) { IntArray(gridSize) }
    val tray = ArrayList<Piece>(traySize)

    var score = 0
        private set
    var combo = 0
        private set
    var comboDeadlineMs = 0L
        private set
    var gameOver = false
        private set

    private val factory = PieceFactory()

    init {
        refillTray()
    }

    fun canPlace(piece: Piece, topRow: Int, topCol: Int): Boolean {
        for (cell in piece.cells) {
            val r = topRow + cell.row
            val c = topCol + cell.col
            if (r !in 0 until gridSize || c !in 0 until gridSize) return false
            if (grid[r][c] != 0) return false
        }
        return true
    }

    fun place(piece: Piece, topRow: Int, topCol: Int, nowMs: Long): PlaceResult? {
        if (!canPlace(piece, topRow, topCol)) return null

        for (cell in piece.cells) {
            grid[topRow + cell.row][topCol + cell.col] = piece.color
        }

        var points = piece.size

        val clearedRows = ArrayList<Int>()
        val clearedCols = ArrayList<Int>()
        for (r in 0 until gridSize) {
            if (grid[r].all { it != 0 }) clearedRows.add(r)
        }
        for (c in 0 until gridSize) {
            var full = true
            for (r in 0 until gridSize) if (grid[r][c] == 0) { full = false; break }
            if (full) clearedCols.add(c)
        }

        if (clearedRows.isNotEmpty() || clearedCols.isNotEmpty()) {
            if (nowMs <= comboDeadlineMs && combo > 0) combo++ else combo = 1
            comboDeadlineMs = nowMs + COMBO_WINDOW_MS
            val clearedCount = clearedRows.size * gridSize + clearedCols.size * gridSize
            points += clearedCount * 10 * combo
            for (r in clearedRows) for (c in 0 until gridSize) grid[r][c] = 0
            for (c in clearedCols) for (r in 0 until gridSize) grid[r][c] = 0
        } else {
            if (nowMs > comboDeadlineMs) combo = 0
        }

        score += points
        tray.remove(piece)
        if (tray.isEmpty()) refillTray()
        gameOver = tray.none { canFitAnywhere(it) }
        return PlaceResult(points, clearedRows, clearedCols, combo)
    }

    private fun canFitAnywhere(piece: Piece): Boolean {
        for (r in 0 until gridSize - piece.h + 1) {
            for (c in 0 until gridSize - piece.w + 1) {
                if (canPlace(piece, r, c)) return true
            }
        }
        return false
    }

    fun canAnyTrayPieceFit(): Boolean =
        tray.isNotEmpty() && tray.any { canFitAnywhere(it) }

    fun restart() {
        for (r in 0 until gridSize) for (c in 0 until gridSize) grid[r][c] = 0
        score = 0
        combo = 0
        comboDeadlineMs = 0L
        gameOver = false
        tray.clear()
        refillTray()
    }

    private fun refillTray() {
        tray.clear()
        repeat(traySize) { tray.add(factory.randomPiece()) }
    }

    fun removeFromTray(index: Int): Piece {
        return tray.removeAt(index)
    }
}
