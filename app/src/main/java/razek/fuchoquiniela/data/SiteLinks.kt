package razek.fuchoquiniela.data

import java.net.URI

/** A fuchoquiniela.com page the app can show itself (routes in fuchito's routes/web.php). */
sealed class AppLink {
    /** The home page and /pools: the pool list. */
    data object Pools : AppLink()

    /** /pool/join and /pool/{uuid}/join: the join dialog, prefilled when there's a uuid. */
    data class JoinPool(val uuid: String?) : AppLink()

    data class PoolPage(val uuid: String, val page: Page) : AppLink() {
        enum class Page { Matches, Standings, Rounds, Info }
    }

    /** /partidos, with ?estado=proximos|resultados. */
    data class Matches(val results: Boolean) : AppLink()
    data object Table : AppLink()
    data object Simulator : AppLink()
    data object News : AppLink()
    data class Post(val id: Int) : AppLink()
    data object Help : AppLink()

    companion object {
        private val hosts = setOf("fuchoquiniela.com", "www.fuchoquiniela.com")
        private val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        /**
         * The app screen for a link, or null when it should open in the browser: other sites,
         * pages the app doesn't have (creating a pool, legal pages, teams…) and /blog/link/{id},
         * which redirects to the post's outside source.
         */
        fun parse(url: String): AppLink? {
            val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
            if (uri.scheme !in setOf("http", "https") || uri.host?.lowercase() !in hosts) return null
            val parts = (uri.path ?: "").split('/').filter { it.isNotEmpty() }
            val query = uri.query.orEmpty()

            return when (parts.firstOrNull()) {
                null -> Pools
                "pools" -> if (parts.size == 1) Pools else null
                "pool" -> pool(parts.drop(1))
                "partidos" -> if (parts.size == 1) Matches(results = "estado=resultados" in query) else null
                "tabla" -> if (parts.size == 1) Table else null
                "simulador" -> Simulator
                "blog" -> when {
                    parts.size == 1 -> News
                    parts[1] == "link" -> null
                    else -> parts[1].toIntOrNull()?.let(::Post)
                }
                "home" -> if (parts.getOrNull(1) == "help") Help else null
                else -> null
            }
        }

        private fun pool(parts: List<String>): AppLink? {
            val first = parts.firstOrNull() ?: return null // /pool alone creates a pool: web only
            if (first == "join") return JoinPool(null)
            val uuid = uuidPattern.matchEntire(first)?.value ?: return null
            val page = when (parts.getOrNull(1)) {
                null, "fill", "picks" -> PoolPage.Page.Matches
                "positions" -> PoolPage.Page.Standings
                "rounddetail" -> PoolPage.Page.Rounds
                "information", "participants", "invite" -> PoolPage.Page.Info
                "join" -> return JoinPool(uuid)
                else -> return null
            }
            return PoolPage(uuid, page)
        }
    }
}
