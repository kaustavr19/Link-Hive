package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LinkViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)

        val sharedPref = getSharedPreferences("linkhive_preferences", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            
            // Check manual preference
            var hasManualChoice by remember {
                mutableStateOf(sharedPref.contains("is_dark_theme"))
            }
            var isDarkThemeManual by remember {
                mutableStateOf(sharedPref.getBoolean("is_dark_theme", systemDark))
            }

            val finalDarkTheme = if (hasManualChoice) isDarkThemeManual else systemDark

            MyApplicationTheme(darkTheme = finalDarkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    isDarkTheme = finalDarkTheme,
                    onToggleTheme = {
                        val nextValue = !finalDarkTheme
                        sharedPref.edit().putBoolean("is_dark_theme", nextValue).apply()
                        hasManualChoice = true
                        isDarkThemeManual = nextValue
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: intent.getStringExtra("android.intent.extra.TEXT")
            if (sharedText != null) {
                val url = viewModel.extractUrl(sharedText)
                if (url != null) {
                    viewModel.saveLinkFromShare(url)
                } else {
                    android.widget.Toast.makeText(this, "No valid link found in shared content", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
