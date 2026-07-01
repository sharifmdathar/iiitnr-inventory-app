import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.iiitnr.inventoryapp.data.cache.DatabaseModule
import com.iiitnr.inventoryapp.data.cache.DriverFactory
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.shared.App
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val tokenManager = createTokenManager()

    ComposeViewport(document.body!!) {
        App(
            tokenManager = tokenManager
        )
    }
}
