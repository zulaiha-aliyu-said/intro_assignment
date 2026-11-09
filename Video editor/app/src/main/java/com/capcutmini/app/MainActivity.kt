package com.capcutmini.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.capcutmini.app.ui.navigation.CapCutMiniNavHost
import com.capcutmini.app.ui.theme.CapCutMiniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CapCutMiniApp
        setContent {
            CapCutMiniTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CapCutMiniNavHost(application = app)
                }
            }
        }
    }
}
