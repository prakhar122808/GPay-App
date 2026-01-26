package com.example.gpayproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessageDbHelper(context: Context) :
    SQLiteOpenHelper(context, "gpay.db", null, 2) {

    data class StoredMessage(
        val id: Long,
        val rawText: String,
        val amount: Double,
        val timestamp: Long
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rawText TEXT NOT NULL,
                amount REAL NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple destructive migration for now (acceptable at this stage)
        db.execSQL("DROP TABLE IF EXISTS messages")
        onCreate(db)
    }

    fun insertMessage(
        rawText: String,
        amount: Double,
        timestamp: Long
    ) {
        val values = ContentValues().apply {
            put("rawText", rawText)
            put("amount", amount)
            put("timestamp", timestamp)
        }
        writableDatabase.insert("messages", null, values)
    }

    fun getAllMessages(): List<StoredMessage> {
        val list = mutableListOf<StoredMessage>()

        val cursor = readableDatabase.rawQuery(
            "SELECT id, rawText, amount, timestamp FROM messages ORDER BY timestamp DESC",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    StoredMessage(
                        id = it.getLong(0),
                        rawText = it.getString(1),
                        amount = it.getDouble(2),
                        timestamp = it.getLong(3)
                    )
                )
            }
        }
        return list
    }

    fun messageExists(rawText: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM messages WHERE rawText = ? LIMIT 1",
            arrayOf(rawText)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun clearAllMessages() {
        writableDatabase.delete("messages", null, null)
    }
}