package razek.fuchoquiniela.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.DateText
import razek.fuchoquiniela.data.FormGame
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.LeagueMatch
import razek.fuchoquiniela.data.LeagueSimulator
import razek.fuchoquiniela.data.SimulatedScore
import razek.fuchoquiniela.data.SimulatorResponse
import razek.fuchoquiniela.data.SimulatorStore
import razek.fuchoquiniela.data.Tally
import razek.fuchoquiniela.data.TableRow
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.Badge
import razek.fuchoquiniela.ui.components.BadgeStyle
import razek.fuchoquiniela.ui.components.CardShape
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FilterBar
import razek.fuchoquiniela.ui.components.FuchoCard
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.gutter
import razek.fuchoquiniela.ui.components.GroupedSection
import razek.fuchoquiniela.ui.components.LeagueTableHeader
import razek.fuchoquiniela.ui.components.LeagueTableRow
import razek.fuchoquiniela.ui.components.LiguillaBracket
import razek.fuchoquiniela.ui.components.ListRow
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.MAX_GOALS
import razek.fuchoquiniela.ui.components.SectionHeader
import razek.fuchoquiniela.ui.components.SegmentedTabs
import razek.fuchoquiniela.ui.components.TeamCrest
import razek.fuchoquiniela.ui.components.TeamScoreRow
import razek.fuchoquiniela.ui.rememberLoader
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId

/** The table shows the W/D/L columns when there's room for them. */
private const val WIDE_TABLE_DP = 520

// MARK: League matches

/**
 * "Partidos" tab: the calendar and results of the active seasons (the web's /partidos),
 * grouped by day and loaded 30 at a time as the list scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LeagueMatchesScreen(requestedStatus: Int? = null, onRequestHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alerts = remember { AlertState() }
    var status by remember { mutableIntStateOf(requestedStatus ?: 0) } // 0 upcoming, 1 results
    // A link like /partidos?estado=resultados picks the filter.
    LaunchedEffect(requestedStatus) {
        if (requestedStatus != null) {
            status = requestedStatus
            onRequestHandled()
        }
    }
    val matches = remember { mutableStateListOf<LeagueMatch>() }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val title = stringResource(R.string.tab_matches)

    fun loadNextPage(replacing: Boolean = false) {
        if (!replacing && (!hasMore || loading)) return
        if (replacing) job?.cancel()
        loading = true
        val apiStatus = if (status == 0) "upcoming" else "results"
        job = scope.launch {
            try {
                val response = FuchoApi.leagueMatches(apiStatus, if (replacing) 1 else page + 1)
                if (replacing) matches.clear()
                val known = matches.map { it.id }.toSet()
                matches.addAll(response.matches.filter { it.id !in known })
                page = response.page
                hasMore = response.hasMore
                loadedOnce = true
            } catch (e: ApiError) {
                alerts.show(context, e, title)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(status) {
        loadedOnce = false
        loadNextPage(replacing = true)
        // Only when the list is on screen: scrolling waits for a layout, which the spinner has none of.
        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
    }
    // Load the next page a few rows before the end.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 5) loadNextPage() }
    }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(title) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterBar {
                SegmentedTabs(
                    listOf(stringResource(R.string.upcoming), stringResource(R.string.results)),
                    selected = status,
                    onSelect = { status = it },
                    modifier = Modifier.widthIn(max = 680.dp),
                )
            }
            PullToRefreshBox(
                isRefreshing = loading && loadedOnce && page <= 1,
                onRefresh = { loadNextPage(replacing = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                val zone = ZoneId.systemDefault()
                val days = remember(matches.toList()) { matches.groupBy { it.kickoff.atZone(zone).toLocalDate() }.toList() }
                when {
                    !loadedOnce && matches.isEmpty() -> LoadingBox()
                    matches.isEmpty() -> EmptyState(
                        Icons.Outlined.CalendarMonth,
                        stringResource(if (status == 0) R.string.no_upcoming else R.string.no_results),
                        stringResource(R.string.schedule_soon),
                    )
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = Spacing.xl),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        days.forEach { (day: LocalDate, list) ->
                            stickyHeader(key = "day-$day") {
                                SectionHeader(DateText.longDay(context, list.first().kickoff), Modifier.padding(bottom = Spacing.xs))
                            }
                            items(list, key = { it.id }) { match ->
                                Box(Modifier.padding(vertical = 5.dp)) { LeagueMatchCard(match) }
                            }
                        }
                    }
                }
            }
        }
    }
    AlertHost(alerts)
}

/** A match row like `.match-row` on the web: home team, time or score pill, away team. */
@Composable
private fun LeagueMatchCard(match: LeagueMatch) {
    var localWins = false
    var visitWins = false
    if (match.scoreLocal != null && match.scoreVisit != null && !match.isLive) {
        localWins = match.scoreLocal > match.scoreVisit
        visitWins = match.scoreVisit > match.scoreLocal
    }
    FuchoCard {
        Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(match.round.takeIf { it.isNotEmpty() }, match.season).joinToString(" · ").uppercase(AppLocale.locale()),
                    color = FuchoColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                when {
                    match.isLive -> Badge(listOfNotNull(stringResource(R.string.live), match.liveStatus).joinToString(" · "), BadgeStyle.Live)
                    match.hasResult -> Badge(stringResource(R.string.final_), BadgeStyle.Muted)
                }
            }
            Spacer(Modifier.height(Spacing.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    TeamCrest(match.imageLocalUrl, 28.dp)
                    Spacer(Modifier.width(Spacing.s))
                    Text(
                        match.local,
                        color = if (localWins) FuchoColors.lime else FuchoColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier
                        .padding(horizontal = Spacing.s)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FuchoColors.inset)
                        .padding(horizontal = Spacing.m, vertical = 6.dp),
                ) {
                    Text(
                        if (match.hasResult) "${match.scoreLocal} - ${match.scoreVisit}" else DateText.time(match.kickoff),
                        color = when {
                            match.isLive -> FuchoColors.live
                            match.hasResult -> FuchoColors.textPrimary
                            else -> FuchoColors.accent
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = if (match.hasResult) FontFamily.Monospace else FontFamily.Default,
                    )
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    Text(
                        match.visit,
                        color = if (visitWins) FuchoColors.lime else FuchoColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    TeamCrest(match.imageVisitUrl, 28.dp)
                }
            }
        }
    }
}

// MARK: League table

/**
 * "Tabla" tab: the current Liga MX table (the web's /tabla) with each team's last results.
 * Tapping a team shows its record; the toolbar opens the Liguilla simulator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueTableScreen(onSimulator: () -> Unit) {
    val context = LocalContext.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.tab_table)
    val loader = rememberLoader(onError = { error ->
        // A 404 means there's no active tournament; the screen says so instead of an alert.
        if (!(error is ApiError.Server && error.status == 404)) alerts.show(context, error, title)
    }) { FuchoApi.standings() }
    // The Liguilla bracket goes above the table while the server says it's visible (the website's
    // home page rule). It's extra, so a 404 or any failure just leaves it out.
    val liguillaLoader = rememberLoader(onError = {}) { FuchoApi.liguilla() }
    var detail by remember { mutableStateOf<TableRow?>(null) }

    LaunchedEffect(Unit) {
        loader.load()
        liguillaLoader.load()
    }

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(title) {
                IconButton(onClick = onSimulator) { Icon(Icons.Filled.Casino, stringResource(R.string.simulator)) }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loader.loading && loader.loaded,
            onRefresh = { loader.load(); liguillaLoader.load() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val response = loader.value
            val error = loader.error
            val liguilla = liguillaLoader.value?.takeIf { it.visible && liguillaLoader.error == null }
            when {
                response == null && liguilla != null && error is ApiError.Server && error.status == 404 ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
                        item { LiguillaBracket(liguilla) }
                    }
                response == null && error is ApiError.Server && error.status == 404 -> EmptyState(
                    Icons.Outlined.FormatListNumbered,
                    stringResource(R.string.no_tournament),
                    stringResource(R.string.table_soon),
                )
                response == null -> if (error == null) LoadingBox()
                else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth.value >= WIDE_TABLE_DP
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
                        if (liguilla != null) item(key = "liguilla") { LiguillaBracket(liguilla) }
                        item {
                            GroupedSection(stringResource(R.string.liga_mx)) {
                                ListRow(
                                    response.season.title,
                                    subtitle = stringResource(R.string.table_summary, response.finishedMatches, response.totalMatches, response.qualified),
                                    icon = Icons.Filled.EmojiEvents,
                                    tint = FuchoColors.lime,
                                    showDivider = false,
                                )
                            }
                        }
                        item {
                            GroupedSection(null, footer = stringResource(R.string.table_legend)) {
                                LeagueTableHeader(wide, withDisclosure = true)
                                HorizontalDivider(color = FuchoColors.separator)
                                response.rows.forEachIndexed { i, row ->
                                    LeagueTableRow(
                                        position = row.position,
                                        team = row.team,
                                        tally = row.tally,
                                        goalDiff = row.goalDiff,
                                        qualified = row.position <= response.qualified,
                                        wide = wide,
                                        form = row.form,
                                        showDivider = i < response.rows.size - 1,
                                        onClick = { detail = row },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { row ->
        ModalBottomSheet(onDismissRequest = { detail = null }, containerColor = FuchoColors.background) {
            TeamDetail(row, qualified = row.position <= (loader.value?.qualified ?: 0))
        }
    }
    AlertHost(alerts)
}

/** A team's record: overall, home and away, plus its results this season (latest first). */
@Composable
private fun TeamDetail(row: TableRow, qualified: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.l)
            .padding(bottom = Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamCrest(row.team.imageUrl, 56.dp)
            Spacer(Modifier.width(Spacing.l))
            Column {
                Text(row.team.name, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.place_points, row.position, row.points), color = FuchoColors.textMuted, fontSize = 14.sp)
                Spacer(Modifier.height(Spacing.xs))
                Badge(
                    stringResource(if (qualified) R.string.playoff_zone else R.string.outside_playoffs),
                    if (qualified) BadgeStyle.Lime else BadgeStyle.Muted,
                )
            }
        }
        Spacer(Modifier.height(Spacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            RecordTile(stringResource(R.string.overall), row.tally, Modifier.weight(1f))
            RecordTile(stringResource(R.string.home), row.home, Modifier.weight(1f))
            RecordTile(stringResource(R.string.away), row.away, Modifier.weight(1f))
        }
        SectionHeader(stringResource(R.string.results), background = FuchoColors.background)
        val form = row.form.orEmpty()
        if (form.isEmpty()) {
            Text(stringResource(R.string.no_games_yet), color = FuchoColors.textMuted, modifier = Modifier.padding(vertical = Spacing.m))
        } else {
            Column(Modifier.clip(CardShape).background(FuchoColors.card)) {
                form.reversed().forEachIndexed { i, game ->
                    FormRow(game)
                    if (i < form.size - 1) HorizontalDivider(color = FuchoColors.separator)
                }
            }
        }
    }
}

@Composable
private fun RecordTile(title: String, tally: Tally?, modifier: Modifier = Modifier) {
    FuchoCard(modifier) {
        Column(Modifier.padding(Spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title.uppercase(), color = FuchoColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                tally?.points?.toString() ?: "–",
                color = FuchoColors.lime,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
            )
            Text(stringResource(R.string.pts), color = FuchoColors.textMuted, fontSize = 11.sp)
            if (tally != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(stringResource(R.string.record, tally.won, tally.drawn, tally.lost), color = FuchoColors.textBody, fontSize = 12.sp)
                Text(stringResource(R.string.goals_record, tally.goalsFor, tally.goalsAgainst), color = FuchoColors.textMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FormRow(game: FormGame) {
    val (outcome, color) = when (game.result) {
        "W" -> stringResource(R.string.won) to FuchoColors.qualified
        "D" -> stringResource(R.string.drew) to FuchoColors.textMuted
        else -> stringResource(R.string.lost) to FuchoColors.live
    }
    Row(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        razek.fuchoquiniela.ui.components.FormBadge(game.result)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.vs_rival, game.rival), fontWeight = FontWeight.SemiBold)
            Text(outcome, color = color, fontSize = 13.sp)
        }
        Text(game.score, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

// MARK: Simulator

/**
 * "Simulador de Liguilla" (the web's /simulador): type or randomize the scores of the matches
 * still to play and see how the Liga MX table would end up. Everything happens on the device;
 * the scores are kept per season and never sent to the server.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SimulatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.simulator)
    val loader = rememberLoader(onError = { error ->
        if (!(error is ApiError.Server && error.status == 404)) alerts.show(context, error, title)
    }) { FuchoApi.simulator() }
    var scores by remember { mutableStateOf<Map<Int, SimulatedScore>>(emptyMap()) }
    var showTable by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { loader.load() }

    val response: SimulatorResponse? = loader.value
    val allMatches = remember(response) { response?.rounds?.flatMap { it.matches }.orEmpty() }
    val simulator = remember(response) { response?.let { LeagueSimulator(it.teams, allMatches) } }
    LaunchedEffect(response) {
        // Drop saved scores of matches that were played since.
        if (response != null) {
            val ids = allMatches.map { it.id }.toSet()
            scores = SimulatorStore.load(context, response.season.id).filterKeys { it in ids }
        }
    }
    fun update(newScores: Map<Int, SimulatedScore>) {
        scores = newScores
        response?.let { SimulatorStore.save(context, it.season.id, newScores) }
    }
    val table = remember(simulator, scores) { simulator?.table(scores).orEmpty() }

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(title, onBack = onBack) {
                Box {
                    IconButton(onClick = { menuOpen = true }, enabled = response != null) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.simulator_options))
                    }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = FuchoColors.card) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.random_results)) },
                            leadingIcon = { Icon(Icons.Outlined.Casino, null) },
                            onClick = {
                                menuOpen = false
                                update(allMatches.associate { it.id to LeagueSimulator.randomScore() })
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_simulation), color = FuchoColors.danger) },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null, tint = FuchoColors.danger) },
                            onClick = { menuOpen = false; update(emptyMap()) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val error = loader.error
            if (response == null) {
                when {
                    error is ApiError.Server && error.status == 404 -> EmptyState(
                        Icons.Outlined.Casino,
                        stringResource(R.string.no_tournament),
                        stringResource(R.string.simulator_soon),
                    )
                    error == null -> LoadingBox()
                }
                return@Column
            }
            FilterBar {
                SegmentedTabs(
                    listOf(stringResource(R.string.tab_matches), stringResource(R.string.tab_table)),
                    selected = if (showTable) 1 else 0,
                    onSelect = { showTable = it == 1 },
                    modifier = Modifier.widthIn(max = 680.dp),
                )
                Text(
                    if (allMatches.isEmpty()) stringResource(R.string.no_matches_left)
                    else stringResource(R.string.simulated_count, scores.size, allMatches.size),
                    color = FuchoColors.textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (showTable) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth.value >= WIDE_TABLE_DP
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
                        item {
                            GroupedSection(response.season.title) {
                                LeagueTableHeader(wide, withDisclosure = false)
                                HorizontalDivider(color = FuchoColors.separator)
                                table.forEachIndexed { i, row ->
                                    LeagueTableRow(
                                        position = row.position,
                                        team = row.team,
                                        tally = row.tally,
                                        goalDiff = row.goalDiff,
                                        qualified = row.position <= response.qualified,
                                        wide = wide,
                                        movement = row.movement,
                                        showDivider = i < table.size - 1,
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (response.rounds.isEmpty()) {
                EmptyState(
                    Icons.Outlined.Verified,
                    stringResource(R.string.season_finished),
                    stringResource(R.string.season_finished_message),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = gutter, end = gutter, bottom = Spacing.xl),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    response.rounds.forEach { round ->
                        stickyHeader(key = "round-${round.id}") { SectionHeader(round.name, Modifier.padding(bottom = Spacing.xs)) }
                        items(round.matches, key = { it.id }) { match ->
                            Box(Modifier.padding(vertical = 5.dp)) {
                                SimulatorMatchCard(match, scores[match.id]) { local, visit ->
                                    // Like picks: the first tap on an empty match starts it at 0-0.
                                    val current = scores[match.id] ?: SimulatedScore(0, 0)
                                    val next = SimulatedScore(
                                        (current.local + local).coerceIn(0, MAX_GOALS),
                                        (current.visit + visit).coerceIn(0, MAX_GOALS),
                                    )
                                    update(scores + (match.id to next))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    AlertHost(alerts)
}

/** A pending match with two score steppers; a match counts once both sides have a score. */
@Composable
private fun SimulatorMatchCard(match: LeagueMatch, score: SimulatedScore?, onAdjust: (Int, Int) -> Unit) {
    val context = LocalContext.current
    FuchoCard(highlighted = score != null) {
        Column(Modifier.padding(start = Spacing.l, end = Spacing.m, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateText.kickoff(context, match.kickoff),
                    color = FuchoColors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (match.isLive) Badge(stringResource(R.string.live), BadgeStyle.Live)
            }
            Spacer(Modifier.height(Spacing.m))
            TeamScoreRow(match.imageLocalUrl, match.local, score?.local, editable = true, isWinner = false) { onAdjust(it, 0) }
            Spacer(Modifier.height(Spacing.s))
            TeamScoreRow(match.imageVisitUrl, match.visit, score?.visit, editable = true, isWinner = false) { onAdjust(0, it) }
        }
    }
}
