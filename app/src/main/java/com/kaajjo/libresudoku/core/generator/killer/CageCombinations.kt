package com.kaajjo.libresudoku.core.generator.killer

import java.util.concurrent.ConcurrentHashMap

/**
 * Digit-set arithmetic for cages: which digits can appear in a set of distinct digits with a given sum.
 * Masks use bit (value - 1) for digit value, matching the logical rater. Results are cached in a dense
 * table for grids up to 12 digits (about 8 MB for 12, 0.5 MB for 9) and in a map beyond that.
 */
internal object CageCombinations {
    /** Set in a result when at least one combination exists, so the empty combination is distinguishable. */
    const val FEASIBLE = 1 shl 30
    private const val DENSE_LIMIT = 12
    private const val STORED_FEASIBLE = 1 shl 14
    private const val STORED_DONE = 1 shl 15
    private val tables = arrayOfNulls<ShortArray>(DENSE_LIMIT + 1)
    private val memo = ConcurrentHashMap<Long, Int>()

    /**
     * Union of every [count]-digit subset of [available] that sums to [sum].
     *
     * @param available Digits that may still be used.
     * @param count Number of distinct digits to choose.
     * @param sum Required total of the chosen digits.
     * @return Digit mask with [FEASIBLE] set, or zero when no subset exists.
     */
    fun union(available: Int, count: Int, sum: Int): Int {
        if (count == 0) return if (sum == 0) FEASIBLE else 0
        if (count < 0 || sum <= 0 || Integer.bitCount(available) < count) return 0
        val digits = 32 - Integer.numberOfLeadingZeros(available)
        val maxSum = digits * (digits + 1) / 2
        if (sum > maxSum) return 0
        if (digits > DENSE_LIMIT) {
            val key = (available.toLong() shl 20) or (count.toLong() shl 10) or sum.toLong()
            memo[key]?.let { return it }
            return compute(available, count, sum).also { memo[key] = it }
        }
        // Every available mask below 2^digits fits the table for exactly that many digits.
        val table = table(digits)
        val index = (available * (digits + 1) + count) * (maxSum + 1) + sum
        val stored = table[index].toInt() and 0xFFFF
        if (stored and STORED_DONE != 0) {
            return if (stored and STORED_FEASIBLE != 0) (stored and (STORED_FEASIBLE - 1)) or FEASIBLE else 0
        }
        val result = compute(available, count, sum)
        val encoded = STORED_DONE or (if (result != 0) STORED_FEASIBLE else 0) or (result and (STORED_FEASIBLE - 1))
        table[index] = encoded.toShort()
        return result
    }

    private fun compute(available: Int, count: Int, sum: Int): Int {
        val top = 31 - Integer.numberOfLeadingZeros(available)
        val rest = available and (1 shl top).inv()
        var result = union(rest, count, sum)
        if (top + 1 <= sum) {
            val withDigit = union(rest, count - 1, sum - top - 1)
            if (withDigit != 0) result = result or withDigit or (1 shl top) or FEASIBLE
        }
        return result
    }

    private fun table(digits: Int): ShortArray {
        tables[digits]?.let { return it }
        synchronized(this) {
            tables[digits]?.let { return it }
            val maxSum = digits * (digits + 1) / 2
            return ShortArray((1 shl digits) * (digits + 1) * (maxSum + 1)).also { tables[digits] = it }
        }
    }
}
