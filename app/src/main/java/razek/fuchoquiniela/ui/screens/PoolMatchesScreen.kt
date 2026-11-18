package razek.fuchoquiniela.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import razek.fuchoquiniela.FuchoApp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.DateText
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Match
import razek.fuchoquiniela.data.PickInput
import razek.fuchoquiniela.data.PickScore
import razek.fuchoquiniela.data.Pool
import razek.fuchoquiniela.ui.components.AlertData
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.Badge
import razek.fuchoquiniela.ui.components.BadgeStyle
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FilterBar
import razek.fuchoquiniela.ui.components.FuchoCard
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.MAX_GOALS
import razek.fuchoquiniela.ui.components.SectionHeader
import razek.fuchoquiniela.ui.components.SegmentedTabs
import razek.fuchoquiniela.ui.components.TeamScoreRow
import razek.fuchoquiniela.ui.components.shareInvite
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

enum class SaveStatus { Hidden, Saving, Saved }

/**
 * Matches of a pool's season with the user's picks ("Llenar quiniela" on the web).
 * Each +/- tap updates the pick locally and saves it after a short pause, batching rapid taps
 * into one PUT /pools/{uuid}/picks. The server decides whether a match is still open; rejected
 * picks are reverted. Saves run on the app's scope so they finish after leaving the screen.
 */
class PoolPicksState(
    private val pool: Pool,
    private val context: Context,
    private val alerts: AlertState,
    private val scope: CoroutineScope,
) {
    val matches = mutableStateMapOf<Int, Match>()
    var order by mutableStateOf<List<Int>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var saveStatus by mutableStateOf(SaveStatus.Hidden)
        private set

    /** Last pick the server confirmed, used to revert rejected or failed saves. */
    private val savedPicks = mutableMapOf<Int, PickScore>()
    private val pendingPicks = mutableMapOf<Int, PickScore>()
    private var saveTimer: Job? = null
    private var hideStatus: Job? = null

    fun reload() {
        flush()
        loading = true
        scope.launch {
            try {
                val response = FuchoApi.matches(pool.uuid, "all")
                matches.clear()
                response.matches.forEach { matches[it.id] = it }
                savedPicks.clear()
                response.matches.forEach { match -> match.pick?.let { savedPicks[match.id] = it } }
                // Keep taps that are still waiting to be saved.
                pendingPicks.forEach { (id, pick) -> matches[id]?.let { matches[id] = it.copy(pick = pick) } }
                order = response.matches.map { it.id }
                loaded = true
            } catch (e: ApiError) {
                alerts.show(context, e, context.getString(R.string.tab_matches))
            } finally {
                loading = false
            }
        }
    }

    /** How many open matches still need a pick, or null when there are none open. */
    val missing: Int?
        get() {
            val open = matches.values.filter { !it.locked }
            return if (open.isEmpty()) null else open.count { it.pick == null }
        }

    /** A match with no pick starts from 0-0, so the first tap always produces a complete pick. */
    fun adjust(matchId: Int, local: Int = 0, visit: Int = 0) {
        val match = matches[matchId] ?: return
        if (match.locked) return
        val current = match.pick ?: PickScore(0, 0)
        val pick = PickScore(
            (current.scoreLocal + local).coerceIn(0, MAX_GOALS),
            (current.scoreVisit + visit).coerceIn(0, MAX_GOALS),
        )
        matches[matchId] = match.copy(pick = pick)
        pendingPicks[matchId] = pick

        saveTimer?.cancel()
        saveTimer = scope.launch {
            delay(800)
            flush()
        }
    }

    fun flush() {
        saveTimer?.cancel()
        saveTimer = null
        if (pendingPicks.isEmpty()) return
        val batch = pendingPicks.toMap()
        pendingPicks.clear()
        showStatus(SaveStatus.Saving)

        // The request outlives this screen.
        FuchoApp.scope.launch {
            try {
                val response = FuchoApi.savePicks(pool.uuid, batch.map { PickInput(it.key, it.value.scoreLocal, it.value.scoreVisit) })
                response.saved.forEach { id -> batch[id]?.let { savedPicks[id] = it } }
                if (response.rejected.isEmpty()) {
                    showStatus(SaveStatus.Saved)
                    return@launch
                }
                showStatus(SaveStatus.Hidden)
                for (rejection in response.rejected) {
                    val id = rejection.matchId ?: continue
                    if (pendingPicks.containsKey(id)) continue
                    if (rejection.reason == "locked") matches[id]?.let { matches[id] = it.copy(locked = true) }
                    revert(id)
                }
                val lockedOnly = response.rejected.all { it.reason == "locked" }
                alerts.show(
                    context.getString(R.string.picks),
                    context.getString(if (lockedOnly) R.string.picks_locked_rejected else R.string.picks_rejected),
                )
            } catch (e: ApiError) {
                showStatus(SaveStatus.Hidden)
                // Put the batch back (unless newer taps replaced it) so a retry sends it again.
                batch.forEach { (id, pick) -> if (!pendingPicks.containsKey(id)) pendingPicks[id] = pick }
                if (e is ApiError.Unauthorized) return@launch // the app shows the login screen
                alerts.show(
                    AlertData(
                        title = context.getString(R.string.picks_not_saved),
                        message = e.describe(context),
                        confirmTitle = context.getString(R.string.retry),
                        onConfirm = { flush() },
                        dismissTitle = context.getString(R.string.discard),
                        onDismiss = { discardPending() },
                    ),
                )
            }
        }
    }

    private fun discardPending() {
        val ids = pendingPicks.keys.toList()
        pendingPicks.clear()
        ids.forEach(::revert)
    }

    private fun revert(matchId: Int) {
        matches[matchId]?.let { matches[matchId] = it.copy(pick = savedPicks[matchId]) }
    }

    private fun showStatus(status: SaveStatus) {
        hideStatus?.cancel()
        saveStatus = status
        if (status == SaveStatus.Saved) {
            hideStatus = FuchoApp.scope.launch {
                delay(1600)
                saveStatus = SaveStatus.Hidden
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PoolMatchesScreen(
    pool: Pool,
    onBack: () -> Unit,
    onStandings: () -> Unit,
    onRoundDetail: () -> Unit,
    onInfo: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alerts = remember { AlertState() }
    val state = remember(pool.uuid) { PoolPicksState(pool, context.applicationContext, alerts, scope) }
    var showOnlyOpen by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(pool.uuid) { state.reload() }

    // Send picks still waiting when the app goes to the background or the screen closes.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) state.flush() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            state.flush()
        }
    }

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(pool.name, onBack = onBack) {
                IconButton(onClick = onStandings) {
                    Icon(Icons.Filled.FormatListNumbered, stringResource(R.string.standings))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options))
                    }
                    // The web's other pool pages: round detail, information and invites.
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = FuchoColors.card) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.round_detail)) },
                            leadingIcon = { Icon(Icons.Filled.ViewAgenda, null) },
                            onClick = { menuOpen = false; onRoundDetail() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.information)) },
                            leadingIcon = { Icon(Icons.Filled.Info, null) },
                            onClick = { menuOpen = false; onInfo() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.invite_friends)) },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = { menuOpen = false; shareInvite(context, pool) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterBar {
                SegmentedTabs(
                    listOf(stringResource(R.string.to_play), stringResource(R.string.all)),
                    selected = if (showOnlyOpen) 0 else 1,
                    onSelect = {
                        showOnlyOpen = it == 0
                        scope.launch { listState.scrollToItem(0) }
                    },
                    modifier = Modifier.widthIn(max = 680.dp),
                )
                if (state.loaded) {
                    val missing = state.missing
                    Text(
                        when (missing) {
                            null -> stringResource(R.string.no_open_matches)
                            0 -> stringResource(R.string.all_picks_in)
                            1 -> stringResource(R.string.one_pick_left)
                            else -> stringResource(R.string.picks_left, missing)
                        },
                        color = when (missing) {
                            null -> FuchoColors.textMuted
                            0 -> FuchoColors.lime
                            else -> FuchoColors.textBody
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = state.loading && state.loaded,
                    onRefresh = { state.reload() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Matches grouped by round, in season order; "Por jugar" hides locked ones.
                    val sections = remember(state.order, state.matches.toMap(), showOnlyOpen) {
                        val groups = mutableListOf<Pair<String, MutableList<Match>>>()
                        for (id in state.order) {
                            val match = state.matches[id] ?: continue
                            if (showOnlyOpen && match.locked) continue
                            if (groups.lastOrNull()?.first != match.round) groups.add(match.round to mutableListOf())
                            groups.last().second.add(match)
                        }
                        groups
                    }
                    when {
                        !state.loaded && state.loading -> LoadingBox()
                        state.loaded && sections.isEmpty() -> EmptyState(
                            icon = if (showOnlyOpen) Icons.Outlined.Verified else Icons.Outlined.CalendarMonth,
                            title = stringResource(if (showOnlyOpen) R.string.no_matches_to_play else R.string.no_matches),
                            message = stringResource(if (showOnlyOpen) R.string.no_matches_to_play_message else R.string.season_no_matches),
                        )
                        else -> LazyColumn(
                            state = listState,
                            // Room for the floating save status.
                            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = 72.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            sections.forEachIndexed { index, (round, list) ->
                                stickyHeader(key = "round-$index") {
                                    SectionHeader(round, Modifier.padding(bottom = Spacing.xs))
                                }
                                items(list, key = { it.id }) { match ->
                                    Box(Modifier.padding(vertical = Spacing.s - 2.dp)) {
                                        MatchCard(
                                            match = match,
                                            onLocal = { state.adjust(match.id, local = it) },
                                            onVisit = { state.adjust(match.id, visit = it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SaveStatusPill(state.saveStatus, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.l))
            }
        }
    }
    AlertHost(alerts)
}

/** A match card: kickoff and status on top, then one row per team with the user's predicted goals. */
@Composable
private fun MatchCard(match: Match, onLocal: (Int) -> Unit, onVisit: (Int) -> Unit) {
    val context = LocalContext.current
    val editable = !match.locked
    var localWins = false
    var visitWins = false
    if (match.scoreLocal != null && match.scoreVisit != null && !match.isLive) {
        localWins = match.scoreLocal > match.scoreVisit
        visitWins = match.scoreVisit > match.scoreLocal
    }
    FuchoCard(Modifier.alpha(if (editable || match.isLive) 1f else 0.85f)) {
        Column(Modifier.padding(start = Spacing.l, end = Spacing.m, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateText.kickoff(context, match.kickoff),
                    color = FuchoColors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                MatchStatusBadge(match)
            }
            Spacer(Modifier.height(Spacing.m))
            HorizontalDivider(color = FuchoColors.separator)
            Spacer(Modifier.height(Spacing.m))
            TeamScoreRow(match.imageLocalUrl, match.local, match.pick?.scoreLocal, editable, localWins, onLocal)
            Spacer(Modifier.height(Spacing.s))
            TeamScoreRow(match.imageVisitUrl, match.visit, match.pick?.scoreVisit, editable, visitWins, onVisit)
        }
    }
}

@Composable
private fun MatchStatusBadge(match: Match) {
    val context = LocalContext.current
    val score = if (match.hasResult) "${match.scoreLocal}-${match.scoreVisit}" else null
    when {
        match.isLive -> Badge(
            listOfNotNull(stringResource(R.string.live), score, match.liveStatus).filter { it.isNotEmpty() }.joinToString(" · "),
            BadgeStyle.Live,
        )
        score != null -> Badge(stringResource(R.string.final_score, score), BadgeStyle.Muted)
        match.locked -> Badge(stringResource(R.string.closed), BadgeStyle.Muted, Icons.Filled.Lock)
        else -> {
            val closesIn = DateText.closesIn(context, match.kickoff)
            if (closesIn != null) {
                Badge(closesIn, BadgeStyle.Lime, Icons.Filled.AccessTime)
            } else {
                Badge(stringResource(R.string.open), BadgeStyle.Accent)
            }
        }
    }
}

/** Floating pill that confirms picks are being saved, like an inline toast. */
@Composable
private fun SaveStatusPill(status: SaveStatus, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(SaveStatus.Saving) }
    if (status != SaveStatus.Hidden) last = status
    AnimatedVisibility(
        visible = status != SaveStatus.Hidden,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 },
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(18.dp)
        Row(
            Modifier
                .shadow(12.dp, shape)
                .clip(shape)
                .background(FuchoColors.card)
                .border(1.dp, FuchoColors.border, shape)
                .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (last == SaveStatus.Saving) {
                CircularProgressIndicator(Modifier.size(16.dp), color = FuchoColors.textMuted, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.CheckCircle, null, tint = FuchoColors.lime, modifier = Modifier.size(18.dp))
            }
            Text(
                stringResource(if (last == SaveStatus.Saving) R.string.saving else R.string.picks_saved),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
