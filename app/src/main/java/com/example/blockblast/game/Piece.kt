package com.example.blockblast.game

import java.util.Random

class Piece(
    val shape: Array<IntArray>,
    val color: Int,
) {
    val h: Int get() = shape.size
    val w: Int get() = shape[0].size
    val cells: List<Cell> get() {
        val list = ArrayList<Cell>()
        for (r in shape.indices)
            for (c in shape[r].indices)
                if (shape[r][c] == 1) list.add(Cell(r, c))
        return list
    }
    val size: Int get() = cells.size
}

data class Cell(val row: Int, val col: Int)

object Shapes {
    // '#' = block, '.' = empty. Each string is a row.
    val ALL: List<Array<IntArray>> = listOf(
        shape("##"),
        shape("#;#"),
        shape("###"),
        shape("#;#;#"),
        shape("####"),
        shape("#;#;#;#"),
        shape("##;##"),
        shape("###;###"),
        shape("###;###;###"),
        shape("#.;##"),
        shape(".#;##"),
        shape("##;#."),
        shape("##;.#"),
        shape("###;#"),
        shape("#.;#.;##"),
        shape(".#;.#;##"),
        shape("##;#;#"),
        shape("#.;##;#"),
        shape(".#;##;.#"),
        shape("#.;##;.#"),
        shape(".#.;###;.#."),
        shape("###;#.;#."),
        shape("##;..;##"),
        shape("#.;##;..#"),
    )

    private fun shape(spec: String): Array<IntArray> {
        val rows = spec.split(";")
        val grid = Array(rows.size) { r ->
            IntArray(rows[r].length) { c -> if (rows[r][c] == '#') 1 else 0 }
        }
        return grid
    }
}

object Palette {
    val COLORS = intArrayOf(
        0xFFFF5963.toInt(),
        0xFFFFB020.toInt(),
        0xFFFFD23F.toInt(),
        0xFF2ECC71.toInt(),
        0xFF2BC8B4.toInt(),
        0xFF3B9EFF.toInt(),
        0xFFA855F7.toInt(),
        0xFFFF6FAE.toInt(),
        0xFF8B5CF6.toInt(),
    )
}

class PieceFactory(private val random: Random = Random()) {
    private val pieces: List<Piece> = buildList {
        for ((i, shape) in Shapes.ALL.withIndex()) {
            add(Piece(shape, Palette.COLORS[i % Palette.COLORS.size]))
        }
    }

    fun randomPiece(): Piece {
        val index = random.nextInt(pieces.size)
        return pieces[index]
    }
}
