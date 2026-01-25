package com.example.gpayproject

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gpayproject.ui.theme.GPayProjectTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    lateinit var db: MessageDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = MessageDbHelper(applicationContext)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                100
            )
        }

        setContent {
            GPayProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        db = db,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    db: MessageDbHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var messages by remember { mutableStateOf<List<String>>(emptyList()) }
    var didClear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {


        if (!didClear) {
            db.clearAllMessages()
            didClear = true
        }


        val sms = readInboxSms(context)


        for ((sender, body) in sms) {
            if (isLikelyTransaction(sender, body) && !db.messageExists(body)) {


                val amount = extractAmount(body)


                if (amount != null) {
                // TEMP: just log or print
                    println("Parsed amount: $amount")
                }


                db.insertMessage(body)
            }
        }


        messages = db.getAllMessages()
    }

    Column(modifier = modifier) {
        for (msg in messages) {
            Text(msg)
        }
    }
}

fun readInboxSms(context: Context): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val uri = "content://sms/inbox".toUri()

    val cursor = context.contentResolver.query(
        uri,
        arrayOf("address", "body"),
        null,
        null,
        "date DESC"
    )

    cursor?.use { c ->
        while (c.moveToNext()) {
            val sender = c.getString(0) ?: ""
            val body = c.getString(1) ?: ""
            result.add(sender to body)
        }
    }

    return result
}

fun isGPayMessage(text: String): Boolean {
    return text.contains("paid", ignoreCase = true) ||
            text.contains("GPay", ignoreCase = true)
}

fun isLikelyTransaction(sender: String, body: String): Boolean {

    // Normalize once
    val s = sender.lowercase()
    val b = body.lowercase()

    // Sender-based hints (banks / gpay)
    val senderMatch =
        s.contains("gpay") ||
                s.contains("hdfc") ||
                s.contains("icici") ||
                s.contains("sbi") ||
                s.contains("axis") ||
                s.contains("bank")

    // Content-based hints (money movement)
    val bodyMatch =
        b.contains("₹") ||
                b.contains("inr") ||
                b.contains("paid") ||
                b.contains("debited") ||
                b.contains("credited")

    return senderMatch && bodyMatch
}

fun extractAmount(text: String): Double? {

    val regex = Regex(
        """(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    val match = regex.find(text) ?: return null

    val numberPart = match.groupValues[1]
        .replace(",", "")

    return numberPart.toDoubleOrNull()
}