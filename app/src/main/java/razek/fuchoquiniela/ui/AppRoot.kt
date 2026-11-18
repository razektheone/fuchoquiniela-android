package razek.fuchoquiniela.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import razek.fuchoquiniela.ui.components.LocalSideInset
import razek.fuchoquiniela.ui.components.LogoMark
import razek.fuchoquiniela.ui.components.sideInsetFor
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
    // Tablets and phones in landscape: tabs in a side rail instead of the bottom bar.
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        containerColor = FuchoColors.background,
        // Only the tab bar's height: each screen's own Scaffold handles the status and
        // navigation bars (the tab bar already covers the navigation bar when it's shown).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showTabs && !wide) {
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
        Row(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)) {
            val railShown = showTabs && wide
            if (railShown) TabRail(route) { nav.selectTab(it) }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // The rail already covers the start-side system bar (landscape navigation bar).
                    .then(if (railShown) Modifier.consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start)) else Modifier),
            ) {
            CompositionLocalProvider(LocalSideInset provides sideInsetFor(maxWidth)) {
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
    }
}

/** The tabs as a side rail on tablets and landscape, with the logo on top. */
@Composable
private fun TabRail(route: String?, onSelect: (String) -> Unit) {
    // Phones in landscape are only ~400dp tall: no logo or extra spacing there, and the
    // items scroll if they still don't fit.
    val tall = LocalConfiguration.current.screenHeightDp >= 560
    NavigationRail(
        containerColor = FuchoColors.chrome,
        header = if (tall) {
            { Box(Modifier.padding(vertical = 12.dp)) { LogoMark(40.dp) } }
        } else {
            null
        },
    ) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (tall) 8.dp else 0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Tab.entries.forEach { tab ->
            val selected = route == tab.route
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(tab.route) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                label = { Text(stringResource(tab.title)) },
                colors = NavigationRailItemDefaults.colors(
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
}

private fun NavHostController.selectTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
