package razek.fuchoquiniela.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Colors shared by every screen. Values mirror the web app's dark theme
 * (fuchito public/assets/css/custom.css) and the iOS app, so all three look like one product.
 */
object FuchoColors {
    /** Page background (`#1e2024` on the web). */
    val background = Color(0xFF1E2024)

    /** Header/nav bar and footer (`#191b1e`). */
    val chrome = Color(0xFF191B1E)

    /** Cards and panels (`#292c31`). */
    val card = Color(0xFF292C31)

    /** Insets inside cards: score tiles, place badges. */
    val inset = Color(0xFF1E2024)
    val separator = Color.White.copy(alpha = 0.06f)
    val border = Color.White.copy(alpha = 0.08f)

    /** Primary accent: links, buttons, active tabs (`#38a9ff`). */
    val accent = Color(0xFF38A9FF)

    /** Highlight for points and winners (`#c2ff1f`). */
    val lime = Color(0xFFC2FF1F)

    /** Live matches (`#ff4757`). */
    val live = Color(0xFFFF4757)
    val danger = Color(0xFFF34141)

    /** Liguilla places in the league table (`#24b35f`). */
    val qualified = Color(0xFF24B35F)

    val textPrimary = Color.White
    val textBody = Color(0xFFC5C8CC)
    val textMuted = Color(0xFF9A9DA2)
    val textFaint = Color(0xFF5D6168)

    val gold = Color(0xFFF2C94C)
    val silver = Color(0xFFC0C5CC)
    val bronze = Color(0xFFCD8A4E)

    /** Brand gradient of the logo and avatars (`#4cb6ff` → `#1767d6`). */
    val brandGradient = Brush.linearGradient(listOf(Color(0xFF4CB6FF), Color(0xFF1767D6)))
}

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

private val colorScheme = darkColorScheme(
    primary = FuchoColors.accent,
    onPrimary = Color.White,
    secondary = FuchoColors.lime,
    onSecondary = FuchoColors.background,
    background = FuchoColors.background,
    onBackground = FuchoColors.textPrimary,
    surface = FuchoColors.card,
    onSurface = FuchoColors.textPrimary,
    surfaceVariant = FuchoColors.inset,
    onSurfaceVariant = FuchoColors.textMuted,
    surfaceContainer = FuchoColors.card,
    surfaceContainerHigh = FuchoColors.card,
    surfaceContainerHighest = FuchoColors.card,
    surfaceContainerLow = FuchoColors.chrome,
    outline = FuchoColors.border,
    outlineVariant = FuchoColors.separator,
    error = FuchoColors.danger,
)

private val base = Typography()

private val typography = Typography(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

/** Small uppercase labels (badges, form field titles). */
val EyebrowStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)

/** Blue uppercase group titles. */
val SectionHeaderStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)

/** The website is dark-only; the app follows it. */
@Composable
fun FuchoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
