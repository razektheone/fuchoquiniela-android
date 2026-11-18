package razek.fuchoquiniela

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Session

class FuchoApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
        AppLocale.update(this)
    }

    /** Crests and news images go through the app's HTTP client, so they carry its User-Agent. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { FuchoApi.httpClient }
        .crossfade(true)
        .build()

    companion object {
        /** For work that must outlive a screen, like saving the picks typed just before leaving it. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
