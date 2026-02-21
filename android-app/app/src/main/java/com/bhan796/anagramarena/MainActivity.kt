package com.bhan796.anagramarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.bhan796.anagramarena.data.AppDependencies
import com.bhan796.anagramarena.ui.AnagramArenaApp
import com.bhan796.anagramarena.ui.theme.AnagramArenaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val dependencies = remember { AppDependencies.from(applicationContext) }
            AnagramArenaTheme {
                AnagramArenaApp(dependencies = dependencies)
            }
        }
    }
}