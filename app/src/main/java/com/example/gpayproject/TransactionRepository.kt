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

    fun getWeekExpenseTotal(): Double {
        val start = startOfThisWeek()

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
                return it.getDouble(0)
            }
        }

        return 0.0
    }


    fun getMonthExpenseTotal(): Double {
        val start = startOfThisMonth()

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
                return it.getDouble(0)
            }
        }

        return 0.0
    }


    fun getMonthAverageDailySpend(): Double {
        val total = getMonthExpenseTotal()
        val days = daysElapsedThisMonth()

        if (days <= 0) return 0.0
        return total / days
    }

    private fun daysElapsedThisMonth(): Int {
        val start = startOfThisMonth()
        val now = System.currentTimeMillis()

        val millisInDay = 24L * 60 * 60 * 1000
        return ((now - start) / millisInDay).toInt() + 1
    }
}