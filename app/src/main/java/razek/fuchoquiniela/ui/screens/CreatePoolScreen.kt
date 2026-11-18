package razek.fuchoquiniela.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.CreatePoolRequest
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Pool
import razek.fuchoquiniela.data.PoolStore
import razek.fuchoquiniela.data.Season
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.PrimaryButton
import razek.fuchoquiniela.ui.components.gutter
import razek.fuchoquiniela.ui.rememberLoader
import razek.fuchoquiniela.ui.theme.EyebrowStyle
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

/**
 * "Crear quiniela" (the web's /pool form): name, season, optional entry fee and description.
 * The server applies the same rules as the website (POST /api/v1/pools); on success the new pool
 * opens, ready to fill and share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePoolScreen(onBack: () -> Unit, onCreated: (Pool) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.create_pool)
    val seasons = rememberLoader(onError = { alerts.show(context, it, title) }) { FuchoApi.seasons().seasons }

    var name by rememberSaveable { mutableStateOf("") }
    var seasonId by rememberSaveable { mutableStateOf<Int?>(null) }
    var price by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var seasonMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { seasons.load() }
    LaunchedEffect(seasons.value) {
        // Liga MX first when there's a choice, since that's what most pools play.
        val list = seasons.value ?: return@LaunchedEffect
        if (seasonId == null) seasonId = (list.firstOrNull { it.league.contains("liga mx", ignoreCase = true) } ?: list.firstOrNull())?.id
    }

    fun create() {
        focus.clearFocus()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            alerts.show(title, context.getString(R.string.pool_name_required))
            return
        }
        val amount = price.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.let {
            it.toDoubleOrNull()?.takeIf { value -> value in 0.0..999.99 } ?: run {
                alerts.show(title, context.getString(R.string.entry_fee_invalid))
                return
            }
        }
        val season = seasonId ?: return
        saving = true
        scope.launch {
            try {
                val pool = FuchoApi.createPool(
                    CreatePoolRequest(trimmed, season, amount, description.trim().takeIf { it.isNotEmpty() }),
                ).pool
                PoolStore.put(pool)
                onCreated(pool)
            } catch (e: ApiError) {
                alerts.show(context, e, title)
            } finally {
                saving = false
            }
        }
    }

    Scaffold(containerColor = FuchoColors.background, topBar = { FuchoTopBar(title, onBack = onBack) }) { padding ->
        val list = seasons.value
        when {
            list == null -> if (seasons.error == null) LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                Icons.Outlined.EmojiEvents,
                title,
                stringResource(R.string.no_seasons),
                Modifier.padding(padding),
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = gutter, vertical = Spacing.l),
            ) {
                Text(stringResource(R.string.create_pool_lead), color = FuchoColors.textBody, fontSize = 15.sp)
                Spacer(Modifier.height(Spacing.xl))

                Field(stringResource(R.string.pool_name)) { modifier ->
                    TextField(
                        value = name,
                        onValueChange = { if (it.length <= 255) name = it },
                        singleLine = true,
                        enabled = !saving,
                        placeholder = { Text(stringResource(R.string.pool_name_placeholder), color = FuchoColors.textFaint) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        colors = fieldColors(),
                        shape = FieldShape,
                        modifier = modifier,
                    )
                }

                Field(stringResource(R.string.season)) { modifier ->
                    val selected = list.firstOrNull { it.id == seasonId }
                    ExposedDropdownMenuBox(expanded = seasonMenu, onExpandedChange = { if (!saving) seasonMenu = it }) {
                        TextField(
                            value = selected?.let(::seasonLabel) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            enabled = !saving,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonMenu) },
                            colors = fieldColors(),
                            shape = FieldShape,
                            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = seasonMenu,
                            onDismissRequest = { seasonMenu = false },
                            containerColor = FuchoColors.card,
                        ) {
                            list.forEach { season ->
                                DropdownMenuItem(
                                    text = { Text(seasonLabel(season)) },
                                    onClick = {
                                        seasonId = season.id
                                        seasonMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Field(stringResource(R.string.entry_fee), hint = stringResource(R.string.entry_fee_hint)) { modifier ->
                    TextField(
                        value = price,
                        onValueChange = { text -> if (text.length <= 6 && text.all { it.isDigit() || it == '.' || it == ',' }) price = text },
                        singleLine = true,
                        enabled = !saving,
                        prefix = { Text("$", color = FuchoColors.textMuted) },
                        placeholder = { Text("0", color = FuchoColors.textFaint) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = fieldColors(),
                        shape = FieldShape,
                        modifier = modifier,
                    )
                }

                Field(stringResource(R.string.description), hint = stringResource(R.string.description_hint)) { modifier ->
                    TextField(
                        value = description,
                        onValueChange = { if (it.length <= 5000) description = it },
                        enabled = !saving,
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions.Default,
                        colors = fieldColors(),
                        shape = FieldShape,
                        modifier = modifier.heightIn(min = 96.dp),
                    )
                }

                Spacer(Modifier.height(Spacing.s))
                PrimaryButton(
                    title = stringResource(if (saving) R.string.creating else R.string.create_pool),
                    onClick = { create() },
                    loading = saving,
                    enabled = seasonId != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    AlertHost(alerts)
}

/** "Liga MX · Apertura 2026" (the API's league names are uppercase on some seasons). */
private fun seasonLabel(season: Season) = "${season.league} · ${season.name}"

private val FieldShape = RoundedCornerShape(10.dp)

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FuchoColors.inset,
    unfocusedContainerColor = FuchoColors.inset,
    disabledContainerColor = FuchoColors.inset,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = FuchoColors.accent,
    focusedTextColor = FuchoColors.textPrimary,
    unfocusedTextColor = FuchoColors.textPrimary,
    disabledTextColor = FuchoColors.textMuted,
)

/** Uppercase label over a field (like the login form), the border lights up while editing. */
@Composable
private fun Field(label: String, hint: String? = null, content: @Composable (Modifier) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.padding(bottom = Spacing.l)) {
        Text(label.uppercase(), style = EyebrowStyle, color = FuchoColors.textMuted)
        Spacer(Modifier.height(Spacing.s))
        content(
            Modifier
                .fillMaxWidth()
                .border(1.dp, if (focused) FuchoColors.accent else FuchoColors.border, FieldShape)
                .onFocusChanged { focused = it.hasFocus },
        )
        if (hint != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(hint, color = FuchoColors.textMuted, fontSize = 12.sp)
        }
    }
}
