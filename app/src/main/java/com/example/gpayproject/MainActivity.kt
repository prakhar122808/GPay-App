package com.example.gpayproject

import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    lateinit var db: MessageDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = MessageDbHelper(applicationContext)

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
    var messages by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.insertMessage("Harsh paid you ₹50.")
        messages = db.getAllMessages()
    }

    Column(modifier = modifier) {
        for (msg in messages) {
            Text(msg)
        }
    }
}