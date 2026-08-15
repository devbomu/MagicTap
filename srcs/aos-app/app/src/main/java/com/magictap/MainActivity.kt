package com.magictap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.magictap.ui.MagicTapApp
import com.magictap.ui.theme.MagicTapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MagicTapTheme {
                MagicTapApp()
            }
        }
    }
}
