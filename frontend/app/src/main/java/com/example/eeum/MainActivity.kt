
package com.example.eeum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.eeum.ui.navigation.EeumApp
import com.example.eeum.ui.theme.EeumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EeumTheme(dynamicColor = false) {
                EeumApp()
            }
        }
    }
}