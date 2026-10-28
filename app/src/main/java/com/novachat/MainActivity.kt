package com.novachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.novachat.data.settings.SettingsStore
import com.novachat.ui.navigation.NavGraph
import com.novachat.ui.theme.NovaChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NovaChatApp).container
        setContent {
            val mode by container.settings.themeMode
                .collectAsState(initial = SettingsStore.THEME_SYSTEM)
            val dynamic by container.settings.dynamicColor
                .collectAsState(initial = true)
            val dark = when (mode) {
                SettingsStore.THEME_LIGHT -> false
                SettingsStore.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            NovaChatTheme(darkTheme = dark, dynamicColor = dynamic) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(container = container)
                }
            }
        }
    }
}
