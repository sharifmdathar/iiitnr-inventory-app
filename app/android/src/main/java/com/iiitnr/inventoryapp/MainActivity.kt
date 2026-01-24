package com.iiitnr.inventoryapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.theme.IIITNRInventoryAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enableEdgeToEdge()
        }
        setContent {
            IIITNRInventoryAppTheme {
                val tokenManager = createTokenManager(this@MainActivity)
                App(tokenManager = tokenManager)
            }
        }
    }
}