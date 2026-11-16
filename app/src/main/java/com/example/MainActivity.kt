package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.ThemeMode
import com.example.ui.SaviiFocusApp
import com.example.ui.theme.SaviiFocusTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SaviiViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SaviiFocusApplication).container
        val factory = SaviiViewModelFactory(appContainer)

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = factory)
            val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()

            val isDark = when (prefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            SaviiFocusTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColors
            ) {
                SaviiFocusApp(
                    factory = factory,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
