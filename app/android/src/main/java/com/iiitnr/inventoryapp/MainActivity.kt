package com.iiitnr.inventoryapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iiitnr.inventoryapp.data.auth.GoogleSignInHelper
import com.iiitnr.inventoryapp.di.initKoin
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.platform.ImagePickerLauncher
import com.iiitnr.inventoryapp.ui.platform.exportComponentsCsvAndroid
import com.iiitnr.inventoryapp.ui.platform.setImagePickerActivity
import com.iiitnr.inventoryapp.ui.theme.IIITNRInventoryAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private var onGoogleSignInResult: ((String?) -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webClientId = resources.getString(R.string.google_web_client_id)
        googleSignInHelper = GoogleSignInHelper(this, webClientId)
        setImagePickerActivity(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enableEdgeToEdge()
        }

        initKoin {
            androidContext(this@MainActivity)
        }

        setContent {
            IIITNRInventoryAppTheme {
                ImagePickerLauncher()
                App(
                    onGoogleSignInClick = { callback ->
                        onGoogleSignInResult = callback
                        coroutineScope.launch {
                            val idToken = googleSignInHelper.signIn()
                            onGoogleSignInResult?.invoke(idToken)
                            onGoogleSignInResult = null
                        }
                    },
                    onExportComponentsCsv = { csvContent ->
                        exportComponentsCsvAndroid(
                            context = this@MainActivity,
                            filename = "components.csv",
                            content = csvContent,
                        )
                    },
                )
            }
        }
    }
}
