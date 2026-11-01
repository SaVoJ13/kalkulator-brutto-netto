package com.savoj.vatkalkulator

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            // Правила: спочатку збережена/вбудована копія, потім оновлення з інтернету у фоні
            var rules by remember { mutableStateOf(RulesRepository.loadLocal(context)) }
            LaunchedEffect(Unit) {
                val fresh = withContext(Dispatchers.IO) { RulesRepository.fetchRemote(context) }
                if (fresh != null) rules = fresh
            }

            val systemDark = isSystemInDarkTheme()
            var dark by rememberSaveable {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                mutableStateOf(
                    if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false)
                    else systemDark
                )
            }

            // Іконки статус-бару й навігації мають відповідати темі застосунку, а не системній
            DisposableEffect(dark) {
                val appDark = dark
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { appDark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { appDark }
                )
                onDispose { }
            }

            AppTheme(dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SalaryScreen(rules, dark) {
                        dark = !dark
                        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                            .edit().putBoolean("dark_theme", dark).apply()
                    }
                }
            }
        }
    }
}
