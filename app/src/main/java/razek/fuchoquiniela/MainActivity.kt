package razek.fuchoquiniela

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.poolUuid
import razek.fuchoquiniela.ui.FuchoAppRoot
import razek.fuchoquiniela.ui.theme.FuchoTheme

class MainActivity : ComponentActivity() {
    /** A pool code from an invite link the app was opened with, waiting for the join dialog. */
    private val pendingJoin = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark-only, like the website.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        AppLocale.update(this)
        if (savedInstanceState == null) handle(intent)
        setContent {
            FuchoTheme {
                FuchoAppRoot(
                    pendingJoin = pendingJoin.value,
                    onJoinHandled = { pendingJoin.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val link = intent?.data?.toString() ?: return
        poolUuid(link)?.let { pendingJoin.value = link }
    }
}
