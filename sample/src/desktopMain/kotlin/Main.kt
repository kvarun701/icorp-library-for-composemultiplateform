import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material3.MaterialTheme
import icorp.App

fun main() = application {
    val windowState = rememberWindowState(width = 800.dp, height = 750.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "icorp Image Cropper Sample",
        state = windowState
    ) {
        MaterialTheme {
            App()
        }
    }
}
