package com.mrdev.tashtech.tashbox.puzzle

import kotlinx.serialization.Serializable

/**
 * Square grid of nodes. 0 = node off, 1..3 = lit node with one of three "colors".
 * (The UI also gives every color its own shape, so the game stays readable for color-blind players.)
 */
@Serializable
data class Grid(val size: Int, val cells: List<Int>) {
    init {
        require(size in 2..6) { "Unsupported grid size: $size" }
        require(cells.size == size * size) { "Expected ${size * size} cells but got ${cells.size}" }
        require(cells.all { it in 0..MAX_COLOR }) { "Cell value out of range" }
    }

    operator fun get(row: Int, col: Int): Int = cells[row * size + col]

    val litCount: Int get() = cells.count { it != 0 }

    fun withCell(index: Int, value: Int): Grid =
        Grid(size, cells.toMutableList().also { it[index] = value })

    /** Indices of cells whose value differs from [other] (same size required). */
    fun diffIndices(other: Grid): List<Int> {
        require(other.size == size) { "Grid sizes differ" }
        return cells.indices.filter { cells[it] != other.cells[it] }
    }

    companion object {
        const val MAX_COLOR = 3

        inline fun build(size: Int, cell: (row: Int, col: Int) -> Int): Grid =
            Grid(size, List(size * size) { cell(it / size, it % size) })
    }
}
