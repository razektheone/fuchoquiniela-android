package razek.fuchoquiniela.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.DateText
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Participant
import razek.fuchoquiniela.data.Pool
import razek.fuchoquiniela.data.PositionsResponse
import razek.fuchoquiniela.data.RoundDetailResponse
import razek.fuchoquiniela.data.Standing
import razek.fuchoquiniela.data.money
import razek.fuchoquiniela.data.parseDate
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.Avatar
import razek.fuchoquiniela.ui.components.Badge
import razek.fuchoquiniela.ui.components.BadgeStyle
import razek.fuchoquiniela.ui.components.CardShape
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FilterBar
import razek.fuchoquiniela.ui.components.FuchoCard
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.GroupedSection
import razek.fuchoquiniela.ui.components.ListRow
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.SectionHeader
import razek.fuchoquiniela.ui.components.TeamCrest
import razek.fuchoquiniela.ui.components.shareInvite
import razek.fuchoquiniela.ui.rememberLoader
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

// MARK: Standings

/**
 * Pool standings ("Tabla de posiciones" on the web), styled like the site's leaderboard.
 * Scoring is computed by the server: 3 points for an exact score, 1 for the right result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(pool: Pool, onBack: () -> Unit) {
    val context = LocalContext.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.standings)
    val loader = rememberLoader(pool.uuid, onError = { alerts.show(context, it, title) }) { FuchoApi.positions(pool.uuid) }
    LaunchedEffect(pool.uuid) { loader.load() }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(title, onBack = onBack) }) { padding ->
        PullToRefreshBox(
            isRefreshing = loader.loading && loader.loaded,
            onRefresh = { loader.load() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val response: PositionsResponse? = loader.value
            if (response == null) {
                if (loader.loading || loader.error == null) LoadingBox()
                return@PullToRefreshBox
            }
            LazyColumn(
                contentPadding = PaddingValues(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                item { ScoringSummary(pool.name, response.completedMatchesCount) }
                item { Spacer(Modifier.height(Spacing.s)) }
                items(response.positions, key = { it.poolUserId }) { StandingRow(it) }
            }
        }
    }
    AlertHost(alerts)
}

/** Pool name, matches played and the scoring rules, like `.help-points` on the web. */
@Composable
private fun ScoringSummary(poolName: String, completedMatches: Int) {
    FuchoCard(Modifier.widthIn(max = 680.dp)) {
        Column(Modifier.padding(Spacing.l)) {
            Text(poolName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                if (completedMatches == 1) stringResource(R.string.one_match_played) else stringResource(R.string.matches_played_n, completedMatches),
                color = FuchoColors.textMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(Spacing.m))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                PointsTile("3", stringResource(R.string.exact), FuchoColors.lime, Modifier.weight(1f))
                PointsTile("1", stringResource(R.string.result), FuchoColors.accent, Modifier.weight(1f))
                PointsTile("0", stringResource(R.string.miss), FuchoColors.textFaint, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PointsTile(value: String, caption: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(FuchoColors.inset).padding(vertical = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text(caption.uppercase(), color = FuchoColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Place number in a circle: gold, silver, bronze for the top three. */
@Composable
fun PlaceBadge(place: Int, podium: Boolean = true, size: Int = 28) {
    val color = if (podium) mapOf(1 to FuchoColors.gold, 2 to FuchoColors.silver, 3 to FuchoColors.bronze)[place] else null
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color ?: FuchoColors.inset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            place.toString(),
            color = if (color == null) FuchoColors.textPrimary else FuchoColors.background,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun PointsText(points: Int, big: Boolean = true) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = FuchoColors.lime, fontWeight = FontWeight.Black, fontSize = if (big) 20.sp else 16.sp, fontFamily = FontFamily.Monospace)) {
                append(points.toString())
            }
            withStyle(SpanStyle(color = FuchoColors.textMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)) {
                append(" " + stringResource(R.string.pts))
            }
        },
    )
}

/** One leaderboard row: place badge, name, hits and points. */
@Composable
private fun StandingRow(standing: Standing) {
    FuchoCard(Modifier.widthIn(max = 680.dp), highlighted = standing.isMe) {
        Row(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            PlaceBadge(standing.place)
            Spacer(Modifier.width(Spacing.m))
            Avatar(standing.displayName, 34.dp)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(
                        standing.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (standing.isMe) Badge(stringResource(R.string.you), BadgeStyle.Accent)
                }
                Text(stringResource(R.string.hits, standing.exactHits, standing.resultHits), color = FuchoColors.textMuted, fontSize = 13.sp)
            }
            PointsText(standing.points)
        }
    }
}

// MARK: Round detail

/**
 * "Detalle por ronda" (the web's /pool/{uuid}/rounddetail): the round's points per participant
 * and, for every match, what each participant picked and the points it earned. The server only
 * reveals other participants' picks once a match has kicked off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundDetailScreen(pool: Pool, onBack: () -> Unit) {
    val context = LocalContext.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.round_detail)
    val rounds = rememberLoader(pool.uuid, onError = { alerts.show(context, it, title) }) { FuchoApi.rounds(pool.uuid) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val detail = rememberLoader(selected, onError = { alerts.show(context, it, title) }) {
        FuchoApi.roundDetail(pool.uuid, selected ?: 0)
    }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(pool.uuid) { rounds.load() }
    LaunchedEffect(rounds.value) {
        val response = rounds.value ?: return@LaunchedEffect
        if (selected == null) selected = response.currentRoundId ?: response.rounds.lastOrNull()?.id
    }
    LaunchedEffect(selected) { if (selected != null) detail.load() }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val list = rounds.value?.rounds.orEmpty()
            val index = list.indexOfFirst { it.id == selected }.takeIf { it >= 0 }
            // ‹  JORNADA 17 ▾  ›  — arrows step through rounds, the middle opens a menu with all of them.
            FilterBar {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    IconButton(onClick = { index?.let { selected = list[it - 1].id } }, enabled = index != null && index > 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_round))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { menuOpen = true }, enabled = list.isNotEmpty()) {
                            Text(
                                (index?.let { list[it].name } ?: stringResource(R.string.rounds)).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = FuchoColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, tint = FuchoColors.accent)
                        }
                        DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = FuchoColors.card) {
                            list.forEach { round ->
                                DropdownMenuItem(
                                    text = { Text(round.name, fontWeight = if (round.id == selected) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { menuOpen = false; selected = round.id },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { index?.let { selected = list[it + 1].id } }, enabled = index != null && index < list.size - 1) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_round))
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = detail.loading && detail.loaded,
                onRefresh = { detail.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                val response = detail.value
                when {
                    rounds.value != null && list.isEmpty() -> EmptyState(
                        Icons.Outlined.CalendarMonth,
                        stringResource(R.string.no_matches),
                        stringResource(R.string.season_no_matches),
                    )
                    response == null -> if (detail.error == null && rounds.error == null) LoadingBox()
                    response.matches.isEmpty() -> EmptyState(
                        Icons.Outlined.CalendarMonth,
                        stringResource(R.string.no_matches),
                        stringResource(R.string.no_matches_in_round),
                    )
                    else -> RoundDetailList(response)
                }
            }
        }
    }
    AlertHost(alerts)
}

@Composable
private fun RoundDetailList(response: RoundDetailResponse) {
    // Round place per participant; ties share a place ("1, 1, 3").
    val places = remember(response) {
        val result = mutableMapOf<Int, Int>()
        response.participants.forEachIndexed { index, member ->
            val tied = index > 0 && response.participants[index - 1].points == member.points
            result[member.poolUserId] = if (tied) result[response.participants[index - 1].poolUserId]!! else index + 1
        }
        result
    }
    val me = response.participants.firstOrNull { it.isMe }

    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.xl),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            GroupedSection(stringResource(R.string.round_points)) {
                response.participants.forEachIndexed { i, member ->
                    ListRow(
                        title = member.displayName,
                        background = if (member.isMe) FuchoColors.accent.copy(alpha = 0.12f) else Color.Transparent,
                        // No medals before anyone has scored.
                        leading = { PlaceBadge(places[member.poolUserId] ?: 0, podium = member.points > 0, size = 26) },
                        trailing = { PointsText(member.points, big = false) },
                        showDivider = i < response.participants.size - 1,
                    )
                }
            }
        }
        response.matches.forEach { match ->
            item(key = "match-${match.id}") {
                Column(Modifier.padding(horizontal = Spacing.l)) {
                    RoundMatchHeader(match)
                    Column(Modifier.fillMaxWidth().clip(CardShape).background(FuchoColors.card)) {
                        if (match.locked) {
                            // Everyone, in the round's points order; "–" for whoever didn't pick.
                            response.participants.forEachIndexed { i, member ->
                                PickRow(match, member, showDivider = i < response.participants.size - 1)
                            }
                        } else {
                            me?.let { PickRow(match, it, showDivider = true) }
                            ListRow(
                                title = stringResource(R.string.others_hidden),
                                icon = Icons.Filled.VisibilityOff,
                                tint = FuchoColors.textMuted,
                                showDivider = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Crests, short names and the score (or kickoff) above each match's picks. */
@Composable
private fun RoundMatchHeader(match: RoundDetailResponse.RoundMatch) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top = Spacing.xl, bottom = Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamCrest(match.imageLocalUrl, 24.dp)
            Spacer(Modifier.width(Spacing.s))
            Text(match.localShortname ?: match.local, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (match.hasResult) {
                Text(
                    "${match.scoreLocal} - ${match.scoreVisit}",
                    color = if (match.isLive) FuchoColors.live else FuchoColors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = Spacing.s),
                )
            } else {
                Text(
                    DateText.kickoff(context, match.kickoff),
                    color = FuchoColors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.s),
                )
            }
            Text(
                match.visitShortname ?: match.visit,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.s))
            TeamCrest(match.imageVisitUrl, 24.dp)
        }
        Spacer(Modifier.height(Spacing.s))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                match.isLive -> Badge(stringResource(R.string.live), BadgeStyle.Live)
                match.hasResult -> Badge(stringResource(R.string.final_), BadgeStyle.Muted)
                match.locked -> Badge(stringResource(R.string.closed), BadgeStyle.Muted, Icons.Filled.Lock)
                else -> Badge(stringResource(R.string.open), BadgeStyle.Accent)
            }
        }
    }
}

/** The web's colors: exact score in lime, right result in blue, a miss in grey. */
private fun pickColor(points: Int?, hasPick: Boolean): Color = when {
    !hasPick -> FuchoColors.textFaint
    points == 3 -> FuchoColors.lime
    points == 1 -> FuchoColors.accent
    points == 0 -> FuchoColors.textMuted
    else -> FuchoColors.textPrimary
}

@Composable
private fun PickRow(match: RoundDetailResponse.RoundMatch, member: RoundDetailResponse.Member, showDivider: Boolean) {
    val pick = match.picks.firstOrNull { it.poolUserId == member.poolUserId }
    Column(Modifier.background(if (member.isMe) FuchoColors.accent.copy(alpha = 0.12f) else Color.Transparent)) {
        Row(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Text(
                member.displayName,
                color = if (member.isMe) FuchoColors.textPrimary else FuchoColors.textBody,
                fontWeight = if (member.isMe) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                pick?.let { "${it.scoreLocal} - ${it.scoreVisit}" } ?: "–",
                color = pickColor(pick?.points, pick != null),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
            )
            val points = pick?.points
            Box(Modifier.width(40.dp), contentAlignment = Alignment.CenterEnd) {
                if (points != null) {
                    Text(
                        "+$points",
                        color = pickColor(points, true),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        if (showDivider) HorizontalDivider(color = FuchoColors.separator, modifier = Modifier.padding(start = Spacing.l))
    }
}

// MARK: Pool information

/**
 * "Información" of a pool (the web's /pool/{uuid}/information and participants pages): its
 * details, the code and invite link, and who is playing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolInfoScreen(pool: Pool, onBack: () -> Unit) {
    val context = LocalContext.current
    val alerts = remember { AlertState() }
    val participantsTitle = stringResource(R.string.participants)
    val loader = rememberLoader(pool.uuid, onError = { alerts.show(context, it, participantsTitle) }) {
        // The owner first, then you, then everyone else alphabetically (server order).
        FuchoApi.participants(pool.uuid).participants
    }
    LaunchedEffect(pool.uuid) { loader.load() }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(stringResource(R.string.information), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
        ) {
            GroupedSection(null) {
                Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(FuchoColors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, null, tint = FuchoColors.accent)
                    }
                    Spacer(Modifier.width(Spacing.m))
                    Column(Modifier.weight(1f)) {
                        Text(pool.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (pool.subtitle.isNotEmpty()) Text(pool.subtitle, color = FuchoColors.textMuted, fontSize = 14.sp)
                    }
                    if (pool.isAdmin) Badge(stringResource(R.string.admin), BadgeStyle.Accent)
                }
            }

            GroupedSection(stringResource(R.string.details)) {
                DetailRow(stringResource(R.string.entry_fee), money(pool.priceValue), Icons.Filled.Payments)
                pool.created?.let { DetailRow(stringResource(R.string.created), DateText.long(it), Icons.Filled.AccessTime) }
                pool.description?.takeIf { it.isNotBlank() }?.let {
                    ListRow(stringResource(R.string.description), subtitle = it, icon = Icons.AutoMirrored.Filled.Notes)
                }
                val copied = stringResource(R.string.code_copied)
                val copiedMessage = stringResource(R.string.code_copied_message)
                ListRow(
                    stringResource(R.string.code),
                    subtitle = pool.uuid,
                    icon = Icons.Filled.Tag,
                    showDivider = false,
                    onClick = {
                        copy(context, pool.uuid)
                        alerts.show(copied, copiedMessage)
                    },
                )
            }

            GroupedSection(stringResource(R.string.invite_friends)) {
                ListRow(
                    stringResource(R.string.share_invite_link),
                    subtitle = stringResource(R.string.share_invite_hint),
                    icon = Icons.Filled.Share,
                    showDivider = false,
                    onClick = { shareInvite(context, pool) },
                )
            }

            val participants = loader.value
            GroupedSection(
                if (participants.isNullOrEmpty()) participantsTitle else stringResource(R.string.participants_count, participants.size),
            ) {
                if (participants == null) {
                    Box(Modifier.fillMaxWidth().height(80.dp)) { if (loader.loading) LoadingBox() }
                } else {
                    participants.forEachIndexed { i, participant ->
                        ParticipantRow(participant, showDivider = i < participants.size - 1)
                    }
                }
            }
        }
    }
    AlertHost(alerts)
}

@Composable
private fun DetailRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ListRow(
        title,
        icon = icon,
        trailing = { Text(value, color = FuchoColors.textMuted, fontSize = 15.sp) },
    )
}

@Composable
private fun ParticipantRow(participant: Participant, showDivider: Boolean) {
    ListRow(
        title = participant.displayName,
        subtitle = participant.joinedAt?.let(::parseDate)?.let { stringResource(R.string.since, DateText.long(it)) },
        leading = { Avatar(participant.displayName, 34.dp) },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (participant.isOwner) Badge(stringResource(R.string.admin), BadgeStyle.Lime)
                if (participant.isMe) Badge(stringResource(R.string.you), BadgeStyle.Accent)
            }
        },
        showDivider = showDivider,
    )
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("code", text))
}
