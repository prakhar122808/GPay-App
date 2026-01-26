package com.example.gpayproject

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.gpayproject.ui.theme.GPayProjectTheme

class MainActivity : ComponentActivity() {

    private lateinit var db: MessageDbHelper

    private val smsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                tryIngestSms()
            }
            // else: permission denied → do nothing for now
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = MessageDbHelper(applicationContext)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            tryIngestSms()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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

    private fun tryIngestSms() {
        ingestSms(this, db)
    }
}

/* ===================== INGESTION (NO COMPOSE) ===================== */

fun ingestSms(context: Context, db: MessageDbHelper) {
    val sms = readInboxSms(context)

    for ((sender, body) in sms) {

        if (!isLikelyTransaction(sender, body)) continue
        if (db.messageExists(body)) continue

        // already filtered as GPay + payment
        val amount = extractAmount(body) ?: continue
        val direction = extractDirection(body) ?: continue
        val counterparty = extractCounterparty(body) ?: continue

        db.insertMessage(
            rawText = body,
            amount = amount,
            direction = direction,
            counterparty = counterparty,
            timestamp = System.currentTimeMillis()
        )
    }

    for ((sender, body) in sms) {

        // Gate 1: GPay only
        if (!isGPayMessage(sender, body)) continue

        // Gate 2: payment semantics
        if (!isLikelyTransaction(sender, body)) continue

        if (db.messageExists(body)) continue

        // Gate 3: parsing
        val amount = extractAmount(body) ?: continue
        val direction = extractDirection(body) ?: continue
        val counterparty = extractCounterparty(body) ?: continue

        val timestamp = System.currentTimeMillis()

        db.insertMessage(
            rawText = body,
            amount = amount,
            direction = direction,
            counterparty = counterparty,
            timestamp = timestamp
        )
    }
}

/* ===================== UI (READ ONLY) ===================== */

@Composable
fun MainScreen(
    db: MessageDbHelper,
    modifier: Modifier = Modifier
) {
    var messages by remember {
        mutableStateOf<List<MessageDbHelper.StoredMessage>>(emptyList())
    }

    LaunchedEffect(Unit) {
        messages = db.getAllMessages()
    }

    Column(modifier = modifier) {
        for (msg in messages) {
            Text("${msg.rawText} — ₹${msg.amount}")
        }
    }
}

/* ===================== HELPERS ===================== */

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

fun isLikelyTransaction(sender: String, body: String): Boolean {
    val s = sender.lowercase()
    val b = body.lowercase()

    val senderMatch =
        s.contains("gpay") ||
                s.contains("hdfc") ||
                s.contains("icici") ||
                s.contains("sbi") ||
                s.contains("axis") ||
                s.contains("bank")

    val bodyMatch =
        b.contains("₹") ||
                b.contains("inr") ||
                b.contains("paid") ||
                b.contains("debited") ||
                b.contains("credited")

    return senderMatch && bodyMatch
}

fun extractAmount(text: String): Double? {
    val cleaned = text.lowercase()

    val patterns = listOf(
        // ₹50 or ₹ 50.00
        Regex("""₹\s*([\d,]+(?:\.\d{1,2})?)"""),

        // rs. 50 / rs 50
        Regex("""rs\.?\s*([\d,]+(?:\.\d{1,2})?)"""),

        // inr 50
        Regex("""inr\s*([\d,]+(?:\.\d{1,2})?)"""),

        // 50 inr
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*inr"""),

        // paid 50 / sent 50
        Regex("""(?:paid|sent)\s*([\d,]+(?:\.\d{1,2})?)""")
    )

    for (regex in patterns) {
        val match = regex.find(cleaned) ?: continue
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    return null
}

fun isGPayMessage(sender: String, body: String): Boolean {
    val s = sender.lowercase()
    val b = body.lowercase()

    // Strict sender-based detection
    val senderMatch =
        s.contains("gpay") ||
                s.contains("googlepay") ||
                s.contains("google") && s.contains("pay")

    // Fallback body check (very strict)
    val bodyMatch =
        b.contains("gpay") ||
                b.contains("google pay")

    return senderMatch || bodyMatch
}

enum class Direction {
    INCOMING,
    OUTGOING
}

fun extractDirection(text: String): Direction? {
    val t = text.lowercase()

    return when {
        t.contains("paid") ||
                t.contains("sent") ||
                t.contains("debited") ||
                t.contains("spent") -> Direction.OUTGOING

        t.contains("received") ||
                t.contains("credited") ||
                t.contains("credit") ||
                t.contains("got") -> Direction.INCOMING

        else -> null
    }
}

fun extractCounterparty(text: String): String? {
    val t = text.trim()

    val patterns = listOf(
        // paid ₹50 to Mr John Doe
        Regex(
            """\bto\s+(?:mr|mrs|ms|shri|smt)?\.?\s*([A-Za-z][A-Za-z\s]{1,30})""",
            RegexOption.IGNORE_CASE
        ),

        // received ₹50 from Mr John Doe
        Regex(
            """\bfrom\s+(?:mr|mrs|ms|shri|smt)?\.?\s*([A-Za-z][A-Za-z\s]{1,30})""",
            RegexOption.IGNORE_CASE
        ),

        // payment for Amazon
        Regex(
            """\bfor\s+([A-Za-z][A-Za-z\s]{1,30})""",
            RegexOption.IGNORE_CASE
        )
    )

    for (regex in patterns) {
        val match = regex.find(t) ?: continue
        return match.groupValues[1].trim()
    }

    return null
}