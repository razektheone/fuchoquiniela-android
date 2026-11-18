package razek.fuchoquiniela.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.FormGame
import razek.fuchoquiniela.data.Tally
import razek.fuchoquiniela.data.TeamRef
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

// Rows of the Liga MX table (Tabla tab and simulator), styled like `.league-table` on the web:
// green bar for Liguilla places, monospaced numbers, points in lime.

private val columnWidth = 30.dp
private val pointsWidth = 38.dp

/** Column header: # · Equipo · PJ · (G E P on wide screens) · DG · PTS. */
@Composable
fun LeagueTableHeader(wide: Boolean, withDisclosure: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.l, end = if (withDisclosure) 36.dp else Spacing.l, top = Spacing.s, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderLabel("#", 26.dp)
        Spacer(Modifier.width(30.dp))
        Text(stringResource(R.string.col_team), color = FuchoColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        HeaderLabel(stringResource(R.string.col_played), columnWidth)
        if (wide) {
            HeaderLabel(stringResource(R.string.col_won), columnWidth)
            HeaderLabel(stringResource(R.string.col_drawn), columnWidth)
            HeaderLabel(stringResource(R.string.col_lost), columnWidth)
        }
        HeaderLabel(stringResource(R.string.col_diff), columnWidth)
        HeaderLabel(stringResource(R.string.col_points), pointsWidth)
    }
}

@Composable
private fun HeaderLabel(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        color = FuchoColors.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width),
    )
}

/**
 * One team. `movement` is shown by the simulator (places gained or lost); `form` by the
 * standings.
 */
@Composable
fun LeagueTableRow(
    position: Int,
    team: TeamRef,
    tally: Tally,
    goalDiff: Int,
    qualified: Boolean,
    wide: Boolean,
    movement: Int? = null,
    form: List<FormGame>? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .padding(vertical = 6.dp)
                    .fillMaxHeight()
                    .background(if (qualified) FuchoColors.qualified else Color.Transparent),
            )
            Row(
                Modifier.weight(1f).padding(start = Spacing.l - 3.dp, end = if (onClick != null) Spacing.xs else Spacing.l, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        position.toString(),
                        color = if (qualified) FuchoColors.qualified else FuchoColors.textMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (movement != null) {
                        Text(
                            when {
                                movement == 0 -> "="
                                movement > 0 -> "▲$movement"
                                else -> "▼${-movement}"
                            },
                            color = when {
                                movement > 0 -> FuchoColors.qualified
                                movement < 0 -> FuchoColors.live
                                else -> FuchoColors.textFaint
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.xs))
                TeamCrest(team.imageUrl, 22.dp)
                Spacer(Modifier.width(Spacing.xs))
                Column(Modifier.weight(1f)) {
                    Text(team.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val recent = form.orEmpty().takeLast(5)
                    if (recent.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { recent.forEach { FormBadge(it.result) } }
                    }
                }
                Number(tally.played.toString())
                if (wide) {
                    Number(tally.won.toString())
                    Number(tally.drawn.toString())
                    Number(tally.lost.toString())
                }
                Number(if (goalDiff > 0) "+$goalDiff" else goalDiff.toString())
                Text(
                    tally.points.toString(),
                    color = FuchoColors.lime,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(pointsWidth),
                )
                if (onClick != null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = FuchoColors.textFaint, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (showDivider) HorizontalDivider(color = FuchoColors.separator, modifier = Modifier.padding(start = Spacing.l))
    }
}

@Composable
private fun Number(text: String) {
    Text(
        text,
        color = FuchoColors.textBody,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(columnWidth),
    )
}

/** W/D/L dot (`.form-badge` on the web), localized as G/E/P in Spanish. */
@Composable
fun FormBadge(result: String) {
    val (text, color) = when (result) {
        "W" -> stringResource(R.string.form_win) to FuchoColors.qualified
        "D" -> stringResource(R.string.form_draw) to FuchoColors.textMuted
        else -> stringResource(R.string.form_loss) to FuchoColors.live
    }
    Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color), contentAlignment = Alignment.Center) {
        // Without the font padding and with the line trimmed, the letter sits in the middle of the box.
        Text(
            text,
            color = Color.White,
            style = TextStyle(
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Black,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}
