package razek.fuchoquiniela.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.AppLocale
import razek.fuchoquiniela.data.BracketTeam
import razek.fuchoquiniela.data.BracketTie
import razek.fuchoquiniela.data.DateText
import razek.fuchoquiniela.data.LiguillaResponse
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing
import java.time.Instant

/** Narrowest a tie gets before the grid drops a column (the web's `minmax(150px, 1fr)`). */
private val MinTieWidth = 150.dp
private val TieShape = RoundedCornerShape(6.dp)

/**
 * The Liguilla bracket of the website's home page (fuchito `liguilla_bracket.blade.php`): one
 * block per stage with its ties in a grid, then the champion. The server computes everything.
 */
@Composable
fun LiguillaBracket(liguilla: LiguillaResponse) {
    GroupedSection(stringResource(R.string.liguilla), footer = stringResource(R.string.liguilla_rules)) {
        Column(Modifier.padding(Spacing.m)) {
            Text(
                liguilla.season.title.uppercase(AppLocale.locale()),
                color = FuchoColors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (liguilla.projected) {
                Spacer(Modifier.height(Spacing.xs))
                Text(stringResource(R.string.liguilla_projection), color = FuchoColors.lime, fontSize = 12.sp)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = Spacing.s
                val columns = ((maxWidth + gap) / (MinTieWidth + gap)).toInt().coerceIn(1, 4)
                // When there's room, the final shares its row with the champion.
                val final = liguilla.stages.lastOrNull()?.takeIf { it.ties.size == 1 && columns > 1 }
                Column {
                    liguilla.stages.forEach { stage ->
                        if (stage === final) return@forEach
                        StageTitle(stage.name)
                        TieGrid(stage.ties.size, columns, gap) { TieCard(stage.ties[it]) }
                    }
                    if (final != null) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                StageTitle(final.name)
                                TieCard(final.ties[0])
                            }
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                StageTitle(stringResource(R.string.champion))
                                ChampionBox(liguilla.champion, Modifier.weight(1f))
                            }
                            repeat(columns - 2) { Spacer(Modifier.weight(1f)) }
                        }
                    } else {
                        StageTitle(stringResource(R.string.champion))
                        ChampionBox(liguilla.champion)
                    }
                }
            }
        }
    }
}

@Composable
private fun StageTitle(title: String) {
    Text(
        title.uppercase(AppLocale.locale()),
        color = FuchoColors.textMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.s),
    )
}

/** [count] cells in rows of [columns], each row as tall as its tallest cell. */
@Composable
private fun TieGrid(count: Int, columns: Int, gap: androidx.compose.ui.unit.Dp, cell: @Composable (Int) -> Unit) {
    val perRow = columns.coerceAtMost(count.coerceAtLeast(1))
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        (0 until count).chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { index ->
                    Column(Modifier.weight(1f).fillMaxHeight()) { cell(index) }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TieCard(tie: BracketTie) {
    val live = tie.isLive
    val outline = when {
        live -> FuchoColors.live.copy(alpha = 0.55f)
        tie.projected -> FuchoColors.lime.copy(alpha = 0.18f)
        else -> null
    }
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(TieShape)
            .background(FuchoColors.inset)
            .then(if (outline != null) Modifier.border(1.dp, outline, TieShape) else Modifier)
            .padding(horizontal = Spacing.s, vertical = 6.dp),
    ) {
        TeamLine(tie, tie.high, high = true)
        TeamLine(tie, tie.low, high = false)
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = FuchoColors.separator, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.heightIn(min = 22.dp).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val note = tieNote(tie)
            if (note is TieNote.Live) {
                Badge(stringResource(R.string.live), BadgeStyle.Live)
            } else {
                Text(
                    noteText(note).uppercase(AppLocale.locale()),
                    color = FuchoColors.textMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/** One side of a tie: seed, crest, short name, goals per leg and the aggregate in bold. */
@Composable
private fun TeamLine(tie: BracketTie, team: BracketTeam?, high: Boolean) {
    val winner = team != null && tie.winnerId != null && tie.winnerId == team.id
    val out = team != null && tie.winnerId != null && tie.winnerId != team.id
    Row(
        Modifier.fillMaxWidth().height(26.dp).alpha(if (out) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            team?.seed?.toString() ?: "",
            color = FuchoColors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(14.dp),
        )
        if (team == null) {
            Text(
                stringResource(R.string.to_be_decided),
                color = FuchoColors.textFaint,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            return@Row
        }
        TeamCrest(team.imageUrl, 18.dp)
        Text(
            team.shortname ?: team.name,
            color = if (winner) FuchoColors.lime else FuchoColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val aggregate = tie.aggregate ?: return@Row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tie.legs.forEach { leg ->
                val goals = if (leg.played || leg.isLive) (if (high) leg.high else leg.low) else null
                Text(
                    goals?.toString() ?: "–",
                    color = FuchoColors.textMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 8.dp),
                )
            }
        }
        Text(
            (if (high) aggregate.high else aggregate.low).toString(),
            color = if (winner || tie.isLive) FuchoColors.textPrimary else FuchoColors.textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 16.dp),
        )
    }
}

@Composable
private fun ChampionBox(champion: BracketTeam?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(TieShape)
            .background(FuchoColors.inset)
            .then(if (champion != null) Modifier.border(1.dp, FuchoColors.lime.copy(alpha = 0.5f), TieShape) else Modifier)
            .padding(horizontal = Spacing.s, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = if (champion != null) FuchoColors.lime else FuchoColors.textFaint,
            modifier = Modifier.size(24.dp),
        )
        if (champion != null) {
            TeamCrest(champion.imageUrl, 40.dp)
            Text(champion.name, color = FuchoColors.textPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        } else {
            Text(stringResource(R.string.to_be_decided), color = FuchoColors.textFaint, fontSize = 13.sp)
        }
    }
}

/** The line under a tie, chosen like the website's `.ko-tie__meta`. */
sealed interface TieNote {
    data object Live : TieNote
    data class Aggregate(val high: Int, val low: Int) : TieNote
    data class ByPosition(val high: Int, val low: Int) : TieNote
    data class WonOnPenalties(val high: Int, val low: Int) : TieNote
    data object ToPenalties : TieNote
    data class Legs(val first: Instant, val second: Instant) : TieNote
    data class FirstLeg(val date: Instant) : TieNote
    data object SecondLegToSchedule : TieNote
    data object Projected : TieNote
    data object DatesToConfirm : TieNote
    data object None : TieNote
}

fun tieNote(tie: BracketTie): TieNote {
    val aggregate = tie.aggregate
    val dates = tie.legs.map { it.kickoff }
    return when {
        tie.isLive -> TieNote.Live
        tie.status == "finished" && aggregate != null -> when (tie.decidedBy) {
            "position" -> TieNote.ByPosition(aggregate.high, aggregate.low)
            "penalties" -> if (tie.winnerId != null) TieNote.WonOnPenalties(aggregate.high, aggregate.low) else TieNote.ToPenalties
            else -> TieNote.Aggregate(aggregate.high, aggregate.low)
        }
        tie.legs.size >= 2 && dates[0] != null && dates[1] != null -> TieNote.Legs(dates[0]!!, dates[1]!!)
        tie.legs.size == 1 -> when {
            tie.legs[0].played -> TieNote.SecondLegToSchedule
            dates[0] != null -> TieNote.FirstLeg(dates[0]!!)
            else -> TieNote.DatesToConfirm
        }
        tie.high != null -> if (tie.projected) TieNote.Projected else TieNote.DatesToConfirm
        else -> TieNote.None
    }
}

@Composable
private fun noteText(note: TieNote): String {
    val context = LocalContext.current
    return when (note) {
        TieNote.Live -> context.getString(R.string.live)
        is TieNote.Aggregate -> context.getString(R.string.tie_aggregate, note.high, note.low)
        is TieNote.ByPosition -> context.getString(R.string.tie_by_position, note.high, note.low)
        is TieNote.WonOnPenalties -> context.getString(R.string.tie_won_on_penalties, note.high, note.low)
        TieNote.ToPenalties -> context.getString(R.string.tie_to_penalties)
        is TieNote.Legs -> context.getString(R.string.tie_legs, DateText.dayMonth(note.first), DateText.dayMonth(note.second))
        is TieNote.FirstLeg -> context.getString(R.string.tie_first_leg, DateText.dayMonth(note.date))
        TieNote.SecondLegToSchedule -> context.getString(R.string.tie_second_leg_tbd)
        TieNote.Projected -> context.getString(R.string.tie_projected)
        TieNote.DatesToConfirm -> context.getString(R.string.tie_dates_tbd)
        TieNote.None -> ""
    }
}
