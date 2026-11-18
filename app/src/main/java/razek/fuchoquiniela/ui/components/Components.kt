package razek.fuchoquiniela.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.Pool
import razek.fuchoquiniela.data.SITE_URL
import razek.fuchoquiniela.ui.theme.EyebrowStyle
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.SectionHeaderStyle
import razek.fuchoquiniela.ui.theme.Spacing

// Small reusable pieces styled after the website (and the iOS app's Components.swift): cards,
// pill badges, the brand wordmark, avatars, section headers, empty states and buttons.

val CardShape = RoundedCornerShape(12.dp)

/** Widest a screen's content gets, like the iOS app's readable width on iPad. */
val MaxContentWidth = 680.dp

/**
 * Extra side margin that keeps content at [MaxContentWidth] on tablets; 0 on phones. Set once in
 * AppRoot from the space next to the navigation. Lists add it to their content padding (rather
 * than narrowing the list), so the whole screen still scrolls.
 */
val LocalSideInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

/** The usual 16dp gutter plus [LocalSideInset]. */
val gutter: Dp
    @Composable get() = Spacing.l + LocalSideInset.current

/** Side margin that centers [MaxContentWidth] of content (plus 16dp gutters) in [width]. */
fun sideInsetFor(width: Dp): Dp = ((width - MaxContentWidth - Spacing.l * 2) / 2).coerceAtLeast(0.dp)

/** Rounded panel with the web's card color and hairline border. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FuchoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var base = modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(if (highlighted) FuchoColors.accent.copy(alpha = 0.12f) else FuchoColors.card)
        .border(1.dp, if (highlighted) FuchoColors.accent.copy(alpha = 0.4f) else FuchoColors.border, CardShape)
    if (onClick != null || onLongClick != null) {
        base = base.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
    }
    Column(base, content = content)
}

enum class BadgeStyle { Accent, Lime, Live, Muted, Outline }

/** Small uppercase pill, like `.featured-match__badge` and `.live-badge` on the web. */
@Composable
fun Badge(text: String, style: BadgeStyle, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val background = when (style) {
        BadgeStyle.Accent -> FuchoColors.accent.copy(alpha = 0.16f)
        BadgeStyle.Lime -> FuchoColors.lime.copy(alpha = 0.12f)
        BadgeStyle.Live -> FuchoColors.live
        BadgeStyle.Muted -> FuchoColors.inset
        BadgeStyle.Outline -> Color.Transparent
    }
    val foreground = when (style) {
        BadgeStyle.Accent -> FuchoColors.accent
        BadgeStyle.Lime -> FuchoColors.lime
        BadgeStyle.Live -> Color.White
        BadgeStyle.Muted, BadgeStyle.Outline -> FuchoColors.textMuted
    }
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier
            .clip(shape)
            .background(background)
            .then(if (style == BadgeStyle.Outline) Modifier.border(1.dp, FuchoColors.border, shape) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (style == BadgeStyle.Live) {
            val pulse by rememberInfiniteTransition(label = "live").animateFloat(
                initialValue = 1f, targetValue = 0.25f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
            )
            Box(Modifier.size(6.dp).alpha(pulse).clip(CircleShape).background(Color.White))
        }
        if (icon != null) Icon(icon, null, tint = foreground, modifier = Modifier.size(12.dp))
        Text(text.uppercase(), style = EyebrowStyle, color = foreground, maxLines = 1)
    }
}

/** "Fucho" in white and "quiniela" in the accent blue, as in the site header. */
@Composable
fun Wordmark(size: TextUnit) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = FuchoColors.textPrimary)) { append("Fucho") }
            withStyle(SpanStyle(color = FuchoColors.accent)) { append("quiniela") }
        },
        fontSize = size,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.3).sp,
    )
}

@Composable
fun LogoMark(size: Dp) {
    Image(androidx.compose.ui.res.painterResource(R.drawable.logo_mark), null, Modifier.size(size))
}

/** Round avatar with the user's initial on the brand gradient (`.site-account__avatar`). */
@Composable
fun Avatar(name: String?, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(FuchoColors.brandGradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name?.trim()?.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.44f).sp,
        )
    }
}

/** Blue uppercase group title with a hairline underneath (`.matches-group`). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, background: Color = FuchoColors.background) {
    Column(modifier.fillMaxWidth().background(background).padding(top = Spacing.m, bottom = Spacing.xs)) {
        Text(
            title.uppercase(),
            style = SectionHeaderStyle,
            color = FuchoColors.accent,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.s))
        HorizontalDivider(color = FuchoColors.border)
    }
}

/** Centered icon, title and message, with an optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryTitle: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 420.dp)) {
            Box(
                Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(FuchoColors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = FuchoColors.accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(Spacing.l))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FuchoColors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.s))
            Text(message, fontSize = 15.sp, color = FuchoColors.textMuted, textAlign = TextAlign.Center)
            if (actionTitle != null && onAction != null) {
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(actionTitle, onAction)
            }
            if (secondaryTitle != null && onSecondary != null) {
                Spacer(Modifier.height(Spacing.m))
                SecondaryButton(secondaryTitle, onSecondary)
            }
        }
    }
}

private val ButtonShape = RoundedCornerShape(10.dp)

/** Solid blue button. */
@Composable
fun PrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = FuchoColors.accent,
            contentColor = Color.White,
            disabledContainerColor = FuchoColors.accent.copy(alpha = 0.6f),
            disabledContentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.size(Spacing.s))
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Outlined button, like the web's bordered buttons. */
@Composable
fun SecondaryButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, FuchoColors.accent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FuchoColors.textPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** A team crest, with a grey shield while it loads or when there's none. */
@Composable
fun TeamCrest(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Shield)
    if (url.isNullOrEmpty()) {
        Image(placeholder, null, modifier.size(size), colorFilter = ColorFilter.tint(FuchoColors.textFaint))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.size(size),
            placeholder = placeholder,
            error = placeholder,
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FuchoColors.textMuted)
    }
}

/** Dark top bar like the website's sticky header, with an optional back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuchoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FuchoColors.chrome,
            titleContentColor = FuchoColors.textPrimary,
            navigationIconContentColor = FuchoColors.accent,
            actionIconContentColor = FuchoColors.accent,
        ),
    )
}

/** Segmented filter (Por jugar / Todos, Próximos / Resultados…) on the chrome color. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(FuchoColors.inset).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) FuchoColors.accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    color = if (isSelected) FuchoColors.textPrimary else FuchoColors.textMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/** Bar under the top bar holding a filter and an optional caption. */
@Composable
fun FilterBar(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(FuchoColors.chrome)) {
        Column(
            Modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        HorizontalDivider(color = FuchoColors.separator)
    }
}

// Grouped lists (Más, Ayuda, pool information)

/** Inset group with a blue header, like iOS's inset-grouped lists. */
@Composable
fun GroupedSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = gutter)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = SectionHeaderStyle,
                color = FuchoColors.accent,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xl, bottom = Spacing.s),
            )
        } else {
            Spacer(Modifier.height(Spacing.l))
        }
        Column(Modifier.fillMaxWidth().clip(CardShape).background(FuchoColors.card), content = content)
        if (footer != null) {
            Text(
                footer,
                fontSize = 13.sp,
                color = FuchoColors.textMuted,
                modifier = Modifier.padding(start = Spacing.xs, end = Spacing.xs, top = Spacing.s),
            )
        }
    }
}

/** Title (and optional secondary text) with an icon, inside a [GroupedSection]. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = FuchoColors.accent,
    destructive: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    background: Color = Color.Transparent,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            when {
                leading != null -> leading()
                icon != null -> Icon(icon, null, tint = if (destructive) FuchoColors.danger else tint, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (destructive) FuchoColors.danger else FuchoColors.textPrimary,
                    fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 16.sp,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = FuchoColors.textMuted, fontSize = 13.sp)
                }
            }
            trailing?.invoke()
        }
        if (showDivider) HorizontalDivider(color = FuchoColors.separator, modifier = Modifier.padding(start = Spacing.l))
    }
}

// Alerts and errors

data class AlertData(
    val title: String,
    val message: String,
    val confirmTitle: String? = null,
    val onConfirm: (() -> Unit)? = null,
    val dismissTitle: String? = null,
    val onDismiss: (() -> Unit)? = null,
)

/** Holds the alert a screen is showing; `show` replaces it. */
class AlertState {
    var current by mutableStateOf<AlertData?>(null)
        private set

    fun show(alert: AlertData) {
        current = alert
    }

    fun show(title: String, message: String) = show(AlertData(title, message))

    /**
     * Shows an API failure. An expired session needs no alert: the app goes back to the login
     * screen on its own.
     */
    fun show(context: Context, error: ApiError, title: String) {
        if (error is ApiError.Unauthorized && !error.duringLogin) return
        show(title, error.describe(context))
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun AlertHost(state: AlertState) {
    val alert = state.current ?: return
    AlertDialog(
        onDismissRequest = { state.dismiss(); alert.onDismiss?.invoke() },
        title = { Text(alert.title) },
        text = { Text(alert.message) },
        confirmButton = {
            TextButton(onClick = { state.dismiss(); alert.onConfirm?.invoke() }) {
                Text(alert.confirmTitle ?: stringResource(R.string.ok))
            }
        },
        dismissButton = alert.dismissTitle?.let { title ->
            {
                TextButton(onClick = { state.dismiss(); alert.onDismiss?.invoke() }) { Text(title) }
            }
        },
        containerColor = FuchoColors.card,
    )
}

// Links and sharing

/** Opens a fuchoquiniela.com page (or any URL) in a Custom Tab in the app's colors. */
fun openWeb(context: Context, url: String) {
    val colors = CustomTabColorSchemeParams.Builder().setToolbarColor(FuchoColors.chrome.toArgb()).build()
    val intent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colors)
        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
        .setShowTitle(true)
        .build()
    try {
        intent.launchUrl(context, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}

fun openSite(context: Context, path: String) = openWeb(context, "$SITE_URL/$path")

/** Share sheet with the pool's invite link and a short message. */
fun shareInvite(context: Context, pool: Pool) {
    val message = context.getString(R.string.invite_message, pool.name)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.invite_subject, pool.name))
        putExtra(Intent.EXTRA_TEXT, "$message ${pool.inviteUrl}")
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.invite_friends)))
}
