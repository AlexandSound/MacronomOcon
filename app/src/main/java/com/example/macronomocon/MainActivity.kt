package com.example.macronomocon

import android.content.Intent
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
        if (savedInstanceState == null) container.handleAuthRedirect(intent)
        setContent { MacronomOconTheme { MacronomoconApp(container) } }
    }

    // singleTask: the OAuth redirect reuses this activity instead of stacking a new one.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        (application as MacronomoconApplication).container.handleAuthRedirect(intent)
    }
}
