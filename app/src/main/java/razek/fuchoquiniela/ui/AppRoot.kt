package razek.fuchoquiniela.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.PoolStore
import razek.fuchoquiniela.data.Session
import razek.fuchoquiniela.ui.screens.HelpScreen
import razek.fuchoquiniela.ui.screens.LeagueMatchesScreen
import razek.fuchoquiniela.ui.screens.LeagueTableScreen
import razek.fuchoquiniela.ui.screens.LoginScreen
import razek.fuchoquiniela.ui.screens.MoreScreen
import razek.fuchoquiniela.ui.screens.NewsDetailScreen
import razek.fuchoquiniela.ui.screens.NewsScreen
import razek.fuchoquiniela.ui.screens.PoolInfoScreen
import razek.fuchoquiniela.ui.screens.PoolMatchesScreen
import razek.fuchoquiniela.ui.screens.PoolsScreen
import razek.fuchoquiniela.ui.screens.RoundDetailScreen
import razek.fuchoquiniela.ui.screens.SimulatorScreen
import razek.fuchoquiniela.ui.screens.StandingsScreen
import razek.fuchoquiniela.ui.theme.FuchoColors

/**
 * Top-level navigation, like the iOS SceneDelegate: the login screen when signed out, the tabs
 * when signed in (including a saved token at launch; a 401 brings the login screen back).
 */
@Composable
fun FuchoAppRoot(pendingJoin: String?, onJoinHandled: () -> Unit) {
    val context = LocalContext.current
    val signedIn by Session.signedIn.collectAsState()
    var loginMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        Session.expired.collect {
            PoolStore.clear()
            loginMessage = context.getString(R.string.error_session_expired)
        }
    }

    Crossfade(signedIn, label = "root") { isSignedIn ->
        if (isSignedIn) {
            MainNavigation(pendingJoin, onJoinHandled)
        } else {
            LoginScreen(message = loginMessage, onMessageShown = { loginMessage = null })
        }
    }
}

private enum class Tab(val route: String, val title: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    Pools("pools", R.string.tab_pools, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    Matches("matches", R.string.tab_matches, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Table("table", R.string.tab_table, Icons.Filled.FormatListNumbered, Icons.Filled.FormatListNumbered),
    News("news", R.string.tab_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    More("more", R.string.tab_more, Icons.Filled.MoreHoriz, Icons.Filled.MoreHoriz),
}

/** The website's main sections as tabs: Quinielas · Partidos · Tabla · Noticias · Más. */
@Composable
private fun MainNavigation(pendingJoin: String?, onJoinHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = Tab.entries.any { it.route == route }

    Scaffold(
        containerColor = FuchoColors.background,
        bottomBar = {
            if (showTabs) {
                NavigationBar(containerColor = FuchoColors.chrome) {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.selectTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                            label = { Text(stringResource(tab.title)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FuchoColors.accent,
                                selectedTextColor = FuchoColors.accent,
                                unselectedIconColor = FuchoColors.textMuted,
                                unselectedTextColor = FuchoColors.textMuted,
                                indicatorColor = FuchoColors.accent.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)) {
            NavHost(nav, startDestination = Tab.Pools.route) {
                composable(Tab.Pools.route) {
                    PoolsScreen(
                        pendingJoin = pendingJoin,
                        onJoinHandled = onJoinHandled,
                        onOpenPool = { nav.navigate("pool/${it.uuid}") },
                    )
                }
                composable(Tab.Matches.route) { LeagueMatchesScreen() }
                composable(Tab.Table.route) { LeagueTableScreen(onSimulator = { nav.navigate("simulator") }) }
                composable(Tab.News.route) { NewsScreen(onOpenPost = { nav.navigate("news/$it") }) }
                composable(Tab.More.route) {
                    MoreScreen(
                        onSimulator = { nav.navigate("simulator") },
                        onHelp = { nav.navigate("help") },
                    )
                }

                composable("pool/{uuid}", listOf(navArgument("uuid") { type = NavType.StringType })) { backStack ->
                    val pool = PoolStore[backStack.arguments?.getString("uuid").orEmpty()]
                    if (pool == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        PoolMatchesScreen(
                            pool = pool,
                            onBack = { nav.popBackStack() },
                            onStandings = { nav.navigate("pool/${pool.uuid}/standings") },
                            onRoundDetail = { nav.navigate("pool/${pool.uuid}/rounds") },
                            onInfo = { nav.navigate("pool/${pool.uuid}/info") },
                        )
                    }
                }
                composable("pool/{uuid}/standings") { backStack ->
                    PoolStore[backStack.arguments?.getString("uuid").orEmpty()]?.let {
                        StandingsScreen(it, onBack = { nav.popBackStack() })
                    }
                }
                composable("pool/{uuid}/rounds") { backStack ->
                    PoolStore[backStack.arguments?.getString("uuid").orEmpty()]?.let {
                        RoundDetailScreen(it, onBack = { nav.popBackStack() })
                    }
                }
                composable("pool/{uuid}/info") { backStack ->
                    PoolStore[backStack.arguments?.getString("uuid").orEmpty()]?.let {
                        PoolInfoScreen(it, onBack = { nav.popBackStack() })
                    }
                }
                composable("news/{id}", listOf(navArgument("id") { type = NavType.IntType })) { backStack ->
                    NewsDetailScreen(backStack.arguments?.getInt("id") ?: 0, onBack = { nav.popBackStack() })
                }
                composable("simulator") { SimulatorScreen(onBack = { nav.popBackStack() }) }
                composable("help") { HelpScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}

private fun NavHostController.selectTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
