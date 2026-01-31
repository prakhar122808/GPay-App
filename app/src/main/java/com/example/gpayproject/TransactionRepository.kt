package com.example.gpayproject

class TransactionRepository(
    private val db: MessageDbHelper
) {

    fun getTodayExpenseTotal(): Double {
        val start = startOfToday()

        val cursor = db.readableDatabase.rawQuery(
            """
        SELECT SUM(amount)
        FROM messages
        WHERE direction = ?
        AND timestamp >= ?
        """.trimIndent(),
            arrayOf(
                Direction.OUTGOING.name,
                start.toString()
            )
        )

        cursor.use {
            if (it.moveToFirst()) {
                // SUM() returns null if there are no matching rows
                return it.getDouble(0)
            }
        }

        return 0.0
    }

    fun getWeekExpenseTotal(): Double = TODO()

    fun getMonthExpenseTotal(): Double = TODO()

    fun getMonthAverageDailySpend(): Double = TODO()

    fun getTopCounterparties(limit: Int): List<Pair<String, Double>> = TODO()
}