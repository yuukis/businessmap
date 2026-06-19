package com.github.yuukis.businessmap.util

import android.database.Cursor
import java.util.Locale

/**
 * CursorJoinerクラスがもつ、Int型のキー同士でJoinさせる際の問題点を修正したクラス
 * ref: http://poly.hatenablog.com/entry/20101006/p1
 */
class CursorJoinerWithIntKey(
    private val cursorLeft: Cursor,
    columnNamesLeft: Array<String>,
    private val cursorRight: Cursor,
    columnNamesRight: Array<String>
) : Iterator<CursorJoinerWithIntKey.Result>, Iterable<CursorJoinerWithIntKey.Result> {

    private var compareResultIsValid = false
    private lateinit var compareResult: Result

    private val columnsLeft: IntArray
    private val columnsRight: IntArray
    private val values: IntArray

    enum class Result {
        RIGHT, LEFT, BOTH
    }

    init {
        require(columnNamesLeft.size == columnNamesRight.size) {
            "you must have the same number of columns on the left and right, " +
                "${columnNamesLeft.size} != ${columnNamesRight.size}"
        }

        cursorLeft.moveToFirst()
        cursorRight.moveToFirst()

        columnsLeft = buildColumnIndiciesArray(cursorLeft, columnNamesLeft)
        columnsRight = buildColumnIndiciesArray(cursorRight, columnNamesRight)

        values = IntArray(columnsLeft.size * 2)
    }

    override fun iterator(): Iterator<Result> = this

    private fun buildColumnIndiciesArray(cursor: Cursor, columnNames: Array<String>): IntArray {
        val columns = IntArray(columnNames.size)
        for (i in columnNames.indices) {
            try {
                columns[i] = cursor.getColumnIndexOrThrow(columnNames[i])
            } catch (e: IllegalArgumentException) {
                val names = cursor.columnNames
                val message = String.format(
                    Locale.getDefault(), "%s / exist columns: %s",
                    e.message, StringUtils.join(names, ", ")
                )
                throw IllegalArgumentException(message)
            }
        }
        return columns
    }

    override fun hasNext(): Boolean {
        return if (compareResultIsValid) {
            when (compareResult) {
                Result.BOTH -> !cursorLeft.isLast || !cursorRight.isLast
                Result.LEFT -> !cursorLeft.isLast || !cursorRight.isAfterLast
                Result.RIGHT -> !cursorLeft.isAfterLast || !cursorRight.isLast
            }
        } else {
            !cursorLeft.isAfterLast || !cursorRight.isAfterLast
        }
    }

    override fun next(): Result {
        check(hasNext()) { "you must only call next() when hasNext() is true" }
        incrementCursors()

        val hasLeft = !cursorLeft.isAfterLast
        val hasRight = !cursorRight.isAfterLast

        compareResult = if (hasLeft && hasRight) {
            populateValues(values, cursorLeft, columnsLeft, 0 /* start filling at index 0 */)
            populateValues(values, cursorRight, columnsRight, 1 /* start filling at index 1 */)
            when (compareInts(*values)) {
                -1 -> Result.LEFT
                1 -> Result.RIGHT
                else -> Result.BOTH
            }
        } else if (hasLeft) {
            Result.LEFT
        } else {
            Result.RIGHT
        }
        compareResultIsValid = true
        return compareResult
    }

    fun remove() {
        throw UnsupportedOperationException("not implemented")
    }

    private fun incrementCursors() {
        if (compareResultIsValid) {
            when (compareResult) {
                Result.LEFT -> cursorLeft.moveToNext()
                Result.RIGHT -> cursorRight.moveToNext()
                Result.BOTH -> {
                    cursorLeft.moveToNext()
                    cursorRight.moveToNext()
                }
            }
            compareResultIsValid = false
        }
    }

    companion object {
        private fun populateValues(
            values: IntArray,
            cursor: Cursor,
            columnIndicies: IntArray,
            startingIndex: Int
        ) {
            for (i in columnIndicies.indices) {
                values[startingIndex + i * 2] = cursor.getInt(columnIndicies[i])
            }
        }

        private fun compareInts(vararg values: Int): Int {
            require(values.size % 2 == 0) { "you must specify an even number of values" }

            var index = 0
            while (index < values.size) {
                val comp = values[index] - values[index + 1]
                if (comp != 0) {
                    return if (comp < 0) -1 else 1
                }
                index += 2
            }

            return 0
        }
    }
}
