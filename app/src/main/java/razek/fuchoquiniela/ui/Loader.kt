package razek.fuchoquiniela.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import razek.fuchoquiniela.data.ApiError

/**
 * Loads one API response for a screen: keeps the last value, whether a request is running (for
 * the spinner and pull-to-refresh) and reports failures to [onError].
 */
class Loader<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend () -> T,
    private val onError: (ApiError) -> Unit,
) {
    var value by mutableStateOf<T?>(null)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
        private set
    private var job: Job? = null

    /** True once the first response arrived. */
    val loaded: Boolean get() = value != null

    fun load() {
        job?.cancel()
        job = scope.launch {
            loading = true
            try {
                value = fetch()
                error = null
            } catch (e: ApiError) {
                error = e
                onError(e)
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun <T> rememberLoader(key: Any? = Unit, onError: (ApiError) -> Unit, fetch: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    return remember(key) { Loader(scope, fetch, onError) }
}
