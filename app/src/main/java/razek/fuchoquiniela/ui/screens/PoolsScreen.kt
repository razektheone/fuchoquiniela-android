package razek.fuchoquiniela.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import razek.fuchoquiniela.FuchoApp
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Pool
import razek.fuchoquiniela.data.PoolStore
import razek.fuchoquiniela.data.Session
import razek.fuchoquiniela.data.poolUuid
import razek.fuchoquiniela.ui.components.AlertData
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.Avatar
import razek.fuchoquiniela.ui.components.Badge
import razek.fuchoquiniela.ui.components.BadgeStyle
import razek.fuchoquiniela.ui.components.EmptyState
import razek.fuchoquiniela.ui.components.FuchoCard
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.LoadingBox
import razek.fuchoquiniela.ui.components.openSite
import razek.fuchoquiniela.ui.components.shareInvite
import razek.fuchoquiniela.ui.rememberLoader
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

/**
 * Pools the signed-in user participates in. Tapping a pool opens its matches; long-pressing
 * shares its invite; the account menu joins pools and signs out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolsScreen(pendingJoin: String?, onJoinHandled: () -> Unit, onOpenPool: (Pool) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.tab_pools)
    val user by Session.user.collectAsState()

    val loader = rememberLoader(onError = { alerts.show(context, it, title) }) {
        FuchoApi.pools().pools.also(PoolStore::put)
    }
    var joinText by remember { mutableStateOf<String?>(null) }
    var joining by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    // Also refreshes after coming back from a pool.
    LifecycleResumeEffect(Unit) {
        loader.load()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        // Launched with a saved token: fetch who we are for the account menu.
        if (Session.user.value == null) {
            runCatching { Session.setUser(FuchoApi.me().user) }
        }
    }
    LaunchedEffect(pendingJoin) {
        if (pendingJoin != null) {
            joinText = pendingJoin
            onJoinHandled()
        }
    }

    fun join(uuid: String) {
        joining = true
        scope.launch {
            try {
                val pool = FuchoApi.joinPool(uuid).pool
                PoolStore.put(pool)
                alerts.show(context.getString(R.string.done), context.getString(R.string.joined, pool.name))
                loader.load()
            } catch (e: ApiError) {
                alerts.show(context, e, context.getString(R.string.join))
            } finally {
                joining = false
            }
        }
    }

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(stringResource(R.string.my_pools)) {
                IconButton(onClick = { joinText = "" }) {
                    Icon(Icons.Filled.PersonAdd, stringResource(R.string.join_pool))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Avatar(user?.displayName, 32.dp)
                    }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }, containerColor = FuchoColors.card) {
                        user?.let {
                            Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                                Text(it.name, fontWeight = FontWeight.Bold, color = FuchoColors.textPrimary)
                                Text(it.email, color = FuchoColors.textMuted, fontSize = 13.sp)
                            }
                            HorizontalDivider(color = FuchoColors.separator)
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.join_pool)) },
                            leadingIcon = { Icon(Icons.Filled.PersonAdd, null) },
                            onClick = { menuOpen = false; joinText = "" },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_website)) },
                            leadingIcon = { Icon(Icons.Filled.Language, null) },
                            onClick = { menuOpen = false; openSite(context, "pools") },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sign_out), color = FuchoColors.danger) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = FuchoColors.danger) },
                            onClick = { menuOpen = false; confirmSignOut = true },
                        )
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loader.loading && loader.loaded,
            onRefresh = { loader.load() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val pools = loader.value
            when {
                pools == null && (loader.loading || loader.error == null) -> LoadingBox()
                pools.isNullOrEmpty() -> EmptyState(
                    icon = Icons.Outlined.EmojiEvents,
                    title = stringResource(R.string.no_pools_title),
                    message = stringResource(R.string.no_pools_message),
                    actionTitle = stringResource(R.string.join_pool),
                    onAction = { joinText = "" },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(pools, key = { it.uuid }) { pool ->
                        PoolCard(pool, onClick = { onOpenPool(pool) }, onLongClick = { shareInvite(context, pool) })
                    }
                }
            }
            if (joining) LoadingBox()
        }
    }

    joinText?.let { initial ->
        JoinDialog(
            initial = initial,
            onDismiss = { joinText = null },
            onJoin = { text ->
                joinText = null
                val uuid = poolUuid(text)
                if (uuid == null) {
                    alerts.show(context.getString(R.string.join), context.getString(R.string.join_unrecognized))
                } else {
                    join(uuid)
                }
            },
        )
    }

    if (confirmSignOut) {
        SignOutDialog(onDismiss = { confirmSignOut = false })
    }

    AlertHost(alerts)
}

/** Accepts the web invite link (…/pool/{uuid}/join) or the bare pool code. */
@Composable
private fun JoinDialog(initial: String, onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var text by remember {
        val pasted = clipboard.getText()?.text
        mutableStateOf(initial.ifEmpty { pasted?.takeIf { poolUuid(it) != null } ?: "" })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FuchoColors.card,
        title = { Text(stringResource(R.string.join_pool)) },
        text = {
            Column {
                Text(stringResource(R.string.join_prompt), color = FuchoColors.textBody)
                Spacer(Modifier.height(Spacing.m))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("fuchoquiniela.com/pool/…/join", color = FuchoColors.textFaint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FuchoColors.accent,
                        unfocusedBorderColor = FuchoColors.border,
                        cursorColor = FuchoColors.accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onJoin(text) }) { Text(stringResource(R.string.join)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Confirms, then signs out; the token is dropped locally even if the request fails. */
@Composable
fun SignOutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FuchoColors.card,
        text = { Text(stringResource(R.string.sign_out_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                FuchoApp.scope.launch {
                    runCatching { FuchoApi.logout() } // best effort
                    Session.signOut()
                    PoolStore.clear()
                }
            }) {
                Text(stringResource(R.string.sign_out), color = FuchoColors.danger)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PoolCard(pool: Pool, onClick: () -> Unit, onLongClick: () -> Unit) {
    FuchoCard(Modifier.widthIn(max = 680.dp), onClick = onClick, onLongClick = onLongClick) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(FuchoColors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.EmojiEvents, null, tint = FuchoColors.accent)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pool.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (pool.subtitle.isNotEmpty()) {
                    Text(pool.subtitle, color = FuchoColors.textMuted, fontSize = 15.sp, maxLines = 2)
                }
                val count = pool.participantsCount
                if (count != null || pool.isAdmin) {
                    Spacer(Modifier.height(Spacing.s))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        if (count != null) {
                            Text(
                                if (count == 1) stringResource(R.string.participants_one) else stringResource(R.string.participants_n, count),
                                color = FuchoColors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        if (pool.isAdmin) Badge(stringResource(R.string.admin), BadgeStyle.Accent)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = FuchoColors.textFaint)
        }
    }
}
