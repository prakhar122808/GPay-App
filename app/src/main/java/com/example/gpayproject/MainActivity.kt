package com.example.gpayproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gpayproject.ui.theme.GPayProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = "Harsh paid you ₹50"
        setContent {
            GPayProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = fakeMsg(text),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

fun fakeMsg(text : String): String {
    return text
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "$name ",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GPayProjectTheme {
        Greeting("Android")
    }
}