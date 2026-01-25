package com.example.gpayproject
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessageDbHelper(context: Context) :
    SQLiteOpenHelper(context, "gpay.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rawText TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // no-op for now
    }

    fun insertMessage(text: String) {
        val values = ContentValues().apply {
            put("rawText", text)
        }
        writableDatabase.insert("messages", null, values)
    }

    fun getAllMessages(): List<String> {
        val list = mutableListOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT rawText FROM messages",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }
    fun messageExists(text: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM messages WHERE rawText = ? LIMIT 1",
            arrayOf(text)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun clearAllMessages() {
        writableDatabase.delete("messages", null, null)
    }
}