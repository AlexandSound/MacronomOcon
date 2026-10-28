package com.example.macronomocon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.macronomocon.ui.MacronomoconApp
import com.example.macronomocon.ui.theme.MacronomOconTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MacronomoconApplication).container
        setContent { MacronomOconTheme { MacronomoconApp(container) } }
    }
}
