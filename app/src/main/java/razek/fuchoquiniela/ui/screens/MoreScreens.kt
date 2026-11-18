package razek.fuchoquiniela.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import razek.fuchoquiniela.BuildConfig
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.SITE_URL
import razek.fuchoquiniela.data.Session
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.GroupedSection
import razek.fuchoquiniela.ui.components.ListRow
import razek.fuchoquiniela.ui.components.openSite
import razek.fuchoquiniela.ui.components.openWeb
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

private const val CONTACT_EMAIL = "contacto@fuchoquiniela.com"

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = FuchoColors.textFaint)

@Composable
private fun External() = Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = FuchoColors.textFaint, modifier = Modifier.size(16.dp))

/**
 * "Más" tab: the simulator, help, the site's information pages (opened in a Custom Tab so legal
 * text has a single source on fuchoquiniela.com), language and account.
 */
@Composable
fun MoreScreen(onSimulator: () -> Unit, onHelp: () -> Unit) {
    val context = LocalContext.current
    val user by Session.user.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(stringResource(R.string.tab_more)) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
        ) {
            GroupedSection(stringResource(R.string.liga_mx)) {
                ListRow(
                    stringResource(R.string.playoff_simulator),
                    subtitle = stringResource(R.string.playoff_simulator_subtitle),
                    icon = Icons.Filled.Casino,
                    tint = FuchoColors.lime,
                    trailing = { Chevron() },
                    showDivider = false,
                    onClick = onSimulator,
                )
            }
            GroupedSection(stringResource(R.string.help)) {
                ListRow(stringResource(R.string.how_to_use), icon = Icons.AutoMirrored.Filled.Help, trailing = { Chevron() }, onClick = onHelp)
                ListRow(stringResource(R.string.contact), icon = Icons.Filled.Email, trailing = { External() }, showDivider = false) {
                    openSite(context, "contacto")
                }
            }
            GroupedSection(stringResource(R.string.section_site)) {
                ListRow(stringResource(R.string.about), icon = Icons.Filled.Info, trailing = { External() }) { openSite(context, "acerca") }
                ListRow(stringResource(R.string.website), subtitle = "fuchoquiniela.com", icon = Icons.Filled.Public, trailing = { External() }) {
                    openWeb(context, SITE_URL)
                }
                ListRow(stringResource(R.string.terms), icon = Icons.Filled.Description, tint = FuchoColors.textMuted, trailing = { External() }) {
                    openSite(context, "home/terms")
                }
                ListRow(stringResource(R.string.privacy), icon = Icons.Filled.PanTool, tint = FuchoColors.textMuted, trailing = { External() }, showDivider = false) {
                    openSite(context, "home/privacy")
                }
            }
            GroupedSection(stringResource(R.string.preferences), footer = stringResource(R.string.language_footer)) {
                ListRow(
                    stringResource(R.string.language),
                    icon = Icons.Filled.Language,
                    trailing = {
                        Text(stringResource(R.string.language_name), color = FuchoColors.textMuted)
                        External()
                    },
                    showDivider = false,
                ) {
                    // Android 13+ has a per-app language setting (the app lists Spanish and English);
                    // before that, the app follows the system language.
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    } else {
                        Intent(Settings.ACTION_LOCALE_SETTINGS)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    }
                }
            }
            GroupedSection(
                stringResource(R.string.account),
                footer = stringResource(R.string.version_footer, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            ) {
                ListRow(
                    user?.displayName ?: stringResource(R.string.my_account),
                    subtitle = user?.email,
                    icon = Icons.Filled.AccountCircle,
                    trailing = { External() },
                ) { openSite(context, "user/profile") }
                ListRow(
                    stringResource(R.string.sign_out),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    destructive = true,
                    showDivider = false,
                ) { confirmSignOut = true }
            }
        }
    }
    if (confirmSignOut) SignOutDialog(onDismiss = { confirmSignOut = false })
}

/**
 * Help for the Android app: how it works, how points are scored (same rules as the web's
 * /home/help) and an expandable FAQ about the app's own screens.
 */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val steps = listOf(
        R.string.step1_title to R.string.step1_text,
        R.string.step2_title to R.string.step2_text,
        R.string.step3_title to R.string.step3_text,
        R.string.step4_title to R.string.step4_text,
    )
    val rules = listOf(
        Triple("3", R.string.rule_exact, R.string.rule_exact_example) to FuchoColors.lime,
        Triple("1", R.string.result, R.string.rule_result_example) to FuchoColors.accent,
        Triple("0", R.string.miss, R.string.rule_miss_example) to FuchoColors.textFaint,
    )
    val faq = listOf(
        R.string.faq1_q to R.string.faq1_a, R.string.faq2_q to R.string.faq2_a,
        R.string.faq3_q to R.string.faq3_a, R.string.faq4_q to R.string.faq4_a,
        R.string.faq5_q to R.string.faq5_a, R.string.faq6_q to R.string.faq6_a,
        R.string.faq7_q to R.string.faq7_a, R.string.faq8_q to R.string.faq8_a,
        R.string.faq9_q to R.string.faq9_a, R.string.faq10_q to R.string.faq10_a,
    )
    var expanded by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(stringResource(R.string.help), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
        ) {
            GroupedSection(stringResource(R.string.how_it_works)) {
                steps.forEachIndexed { i, (title, text) ->
                    ListRow(
                        stringResource(title),
                        subtitle = stringResource(text),
                        leading = { NumberTile("${i + 1}", FuchoColors.accent) },
                        showDivider = i < steps.size - 1,
                    )
                }
            }
            GroupedSection(stringResource(R.string.points)) {
                rules.forEachIndexed { i, (rule, color) ->
                    ListRow(
                        stringResource(rule.second),
                        subtitle = stringResource(rule.third),
                        leading = { NumberTile(rule.first, color) },
                        showDivider = i < rules.size - 1,
                    )
                }
            }
            GroupedSection(stringResource(R.string.faq)) {
                faq.forEachIndexed { i, (question, answer) ->
                    val open = i in expanded
                    // Questions expand to show their answer.
                    ListRow(
                        stringResource(question),
                        trailing = { Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = FuchoColors.textFaint) },
                        showDivider = !open && i < faq.size - 1,
                        onClick = { expanded = if (open) expanded - i else expanded + i },
                    )
                    AnimatedVisibility(open) {
                        Column {
                            Text(
                                stringResource(answer),
                                color = FuchoColors.textBody,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.m),
                            )
                            if (i < faq.size - 1) {
                                androidx.compose.material3.HorizontalDivider(color = FuchoColors.separator, modifier = Modifier.padding(start = Spacing.l))
                            }
                        }
                    }
                }
            }
            GroupedSection(stringResource(R.string.need_help)) {
                ListRow(
                    stringResource(R.string.email_us),
                    subtitle = CONTACT_EMAIL,
                    icon = Icons.Filled.Email,
                    showDivider = false,
                ) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL")))
                    } catch (_: ActivityNotFoundException) {
                        openSite(context, "contacto")
                    }
                }
            }
        }
    }
}

/** The rule's points (or the step number) in its color, like the web's `.help-points` tiles. */
@Composable
private fun NumberTile(text: String, color: Color) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
    }
}
