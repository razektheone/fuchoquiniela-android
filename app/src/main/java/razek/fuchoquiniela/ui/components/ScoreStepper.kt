package razek.fuchoquiniela.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

const val MAX_GOALS = 20

/** −, goals tile, +. TalkBack reads one element with increase/decrease actions. */
@Composable
fun ScoreStepper(teamName: String, goals: Int?, editable: Boolean, onStep: (Int) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val step = { delta: Int ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onStep(delta)
    }
    val increase = context.getString(R.string.goals_of, teamName) + " +1"
    val decrease = context.getString(R.string.goals_of, teamName) + " −1"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = context.getString(R.string.goals_of, teamName)
            stateDescription = goals?.toString() ?: context.getString(R.string.no_pick)
            if (editable) {
                customActions = listOf(
                    CustomAccessibilityAction(increase) { step(1); true },
                    CustomAccessibilityAction(decrease) { step(-1); true },
                )
            }
        },
    ) {
        if (editable) StepButton(Icons.Filled.Remove, enabled = (goals ?: 0) > 0) { step(-1) }
        Box(
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FuchoColors.inset)
                .border(
                    1.dp,
                    if (editable && goals != null) FuchoColors.accent.copy(alpha = 0.45f) else Color.Transparent,
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                goals?.toString() ?: "–",
                color = if (goals == null) FuchoColors.textFaint else FuchoColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        if (editable) StepButton(Icons.Filled.Add, enabled = (goals ?: 0) < MAX_GOALS) { step(1) }
    }
}

/** A 44×44 touch target around a smaller drawn circle. */
@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (enabled) FuchoColors.textPrimary else FuchoColors.textFaint, modifier = Modifier.size(16.dp))
        }
    }
}

/** Crest, team name and a [ScoreStepper]; used by the pick cards and the simulator. */
@Composable
fun TeamScoreRow(
    crestUrl: String?,
    name: String,
    goals: Int?,
    editable: Boolean,
    isWinner: Boolean,
    onStep: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamCrest(crestUrl, 30.dp)
        Spacer(Modifier.width(Spacing.m))
        Text(
            name,
            color = if (isWinner) FuchoColors.lime else FuchoColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.m))
        ScoreStepper(name, goals, editable, onStep)
    }
}
