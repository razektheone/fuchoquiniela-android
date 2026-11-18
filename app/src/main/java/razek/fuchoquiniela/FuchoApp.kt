package razek.fuchoquiniela

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.Session

class FuchoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
        AppLocale.update(this)
    }

    companion object {
        /** For work that must outlive a screen, like saving the picks typed just before leaving it. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
