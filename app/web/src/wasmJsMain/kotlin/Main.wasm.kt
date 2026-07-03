import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import com.iiitnr.inventoryapp.ui.theme.AppTheme
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val tokenManager = createTokenManager()

    ComposeViewport(document.body!!) {
        AppTheme {
            App(
                tokenManager = tokenManager,
            )
        }
    }
}
