import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.iiitnr.inventoryapp.data.auth.GoogleWebSignInHelper
import com.iiitnr.inventoryapp.di.initKoin
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.theme.AppTheme
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin {}

    val googleSignInHelper = GoogleWebSignInHelper()
    val scope = CoroutineScope(Dispatchers.Default)

    ComposeViewport(document.body!!) {
        AppTheme {
            App(
                onGoogleSignInClick = { callback ->
                    scope.launch {
                        val idToken = googleSignInHelper.signIn()
                        callback(idToken)
                    }
                },
            )
        }
    }
}
