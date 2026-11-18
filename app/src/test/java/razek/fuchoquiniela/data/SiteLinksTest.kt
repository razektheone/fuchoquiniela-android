package razek.fuchoquiniela.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteLinksTest {
    private val uuid = "ca43e2a5-ecb2-4c3b-892d-9507ac1d23fc"

    @Test
    fun opensThePoolListForTheHomePageAndPools() {
        // News posts link the word "quiniela" to the home page.
        assertEquals(AppLink.Pools, AppLink.parse("https://www.fuchoquiniela.com/"))
        assertEquals(AppLink.Pools, AppLink.parse("https://www.fuchoquiniela.com"))
        assertEquals(AppLink.Pools, AppLink.parse("http://fuchoquiniela.com/pools"))
    }

    @Test
    fun opensPoolPages() {
        val page = AppLink.PoolPage.Page::class.java
        assertEquals(AppLink.PoolPage(uuid, AppLink.PoolPage.Page.Matches), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid/fill"))
        assertEquals(AppLink.PoolPage(uuid, AppLink.PoolPage.Page.Matches), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid"))
        assertEquals(AppLink.PoolPage(uuid, AppLink.PoolPage.Page.Standings), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid/positions"))
        assertEquals(AppLink.PoolPage(uuid, AppLink.PoolPage.Page.Rounds), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid/rounddetail/17"))
        assertEquals(AppLink.PoolPage(uuid, AppLink.PoolPage.Page.Info), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid/participants"))
        assertEquals(AppLink.JoinPool(uuid), AppLink.parse("https://www.fuchoquiniela.com/pool/$uuid/join"))
        assertEquals(AppLink.JoinPool(null), AppLink.parse("https://www.fuchoquiniela.com/pool/join"))
        assertEquals(4, page.enumConstants!!.size)
    }

    @Test
    fun opensPublicSections() {
        assertEquals(AppLink.Matches(results = false), AppLink.parse("https://www.fuchoquiniela.com/partidos?estado=proximos"))
        assertEquals(AppLink.Matches(results = true), AppLink.parse("https://www.fuchoquiniela.com/partidos?estado=resultados"))
        assertEquals(AppLink.Table, AppLink.parse("https://www.fuchoquiniela.com/tabla"))
        assertEquals(AppLink.Simulator, AppLink.parse("https://www.fuchoquiniela.com/simulador"))
        assertEquals(AppLink.News, AppLink.parse("https://www.fuchoquiniela.com/blog"))
        assertEquals(AppLink.Post(9), AppLink.parse("https://www.fuchoquiniela.com/blog/9/mexico-vs-jamaica-vuelta-nations-league"))
        assertEquals(AppLink.Help, AppLink.parse("https://www.fuchoquiniela.com/home/help"))
    }

    @Test
    fun leavesEverythingElseToTheBrowser() {
        assertNull(AppLink.parse("https://www.fuchoquiniela.com/pool")) // creating a pool is web only
        assertNull(AppLink.parse("https://www.fuchoquiniela.com/blog/link/7")) // redirects to the source
        assertNull(AppLink.parse("https://www.fuchoquiniela.com/home/terms"))
        assertNull(AppLink.parse("https://www.fuchoquiniela.com/tabla/3/apertura-2025"))
        assertNull(AppLink.parse("https://www.fuchoquiniela.com/equipos/5/america"))
        assertNull(AppLink.parse("https://www.concacaf.com/"))
        assertNull(AppLink.parse("https://fuchoquiniela.com.evil.example/pools"))
        assertNull(AppLink.parse("mailto:contacto@fuchoquiniela.com"))
    }
}
