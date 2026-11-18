package razek.fuchoquiniela.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import razek.fuchoquiniela.BuildConfig
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.Session
import razek.fuchoquiniela.ui.components.AlertData
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.Badge
import razek.fuchoquiniela.ui.components.BadgeStyle
import razek.fuchoquiniela.ui.components.CardShape
import razek.fuchoquiniela.ui.components.LogoMark
import razek.fuchoquiniela.ui.components.PrimaryButton
import razek.fuchoquiniela.ui.components.Wordmark
import razek.fuchoquiniela.ui.components.openSite
import razek.fuchoquiniela.ui.theme.EyebrowStyle
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

/**
 * Sign-in screen, styled after the website's hero: logo, wordmark and a form card.
 * Accounts are created and recovered on fuchoquiniela.com.
 */
@Composable
fun LoginScreen(message: String?, onMessageShown: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.sign_in_title)

    // Debug builds pointed at local fuchito start with its test user (see app/build.gradle.kts).
    var email by rememberSaveable { mutableStateOf(BuildConfig.TEST_EMAIL) }
    var password by remember { mutableStateOf(BuildConfig.TEST_PASSWORD) }
    var loading by remember { mutableStateOf(false) }
    var showRegister by rememberSaveable { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(message) {
        if (message != null) {
            alerts.show(title, message)
            onMessageShown()
        }
    }

    fun resendVerification(email: String, password: String) {
        loading = true
        scope.launch {
            try {
                val response = FuchoApi.resendVerification(email, password)
                alerts.show(context.getString(R.string.verify_your_email), response.message)
            } catch (e: ApiError) {
                alerts.show(context, e, context.getString(R.string.verify_your_email))
            } finally {
                loading = false
            }
        }
    }

    fun signIn() {
        keyboard?.hide() // not clearFocus: see RegisterScreen.submit()
        val trimmed = email.trim()
        if (trimmed.isEmpty() || password.isEmpty()) {
            alerts.show(title, context.getString(R.string.enter_email_password))
            return
        }
        loading = true
        scope.launch {
            try {
                val response = FuchoApi.login(trimmed, password)
                password = ""
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                Session.signIn(response.token, response.user)
            } catch (e: ApiError) {
                if (e is ApiError.Server && e.unverified) {
                    // 403 for an account that hasn't opened its verification link yet.
                    alerts.show(
                        AlertData(
                            title = context.getString(R.string.verify_your_email),
                            message = e.describe(context),
                            confirmTitle = context.getString(R.string.resend_email),
                            onConfirm = { resendVerification(trimmed, password) },
                            dismissTitle = context.getString(R.string.ok),
                        ),
                    )
                } else {
                    alerts.show(context, e, title)
                }
            } finally {
                loading = false
            }
        }
    }

    if (showRegister) {
        RegisterScreen(
            onCancel = { showRegister = false },
            onRegistered = { response ->
                showRegister = false
                email = response.email
                password = ""
                alerts.show(context.getString(R.string.check_your_email), response.message)
            },
        )
        AlertHost(alerts)
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FuchoColors.background)
            // Blue glow in the top corner, like `.home-hero` on the website.
            .background(
                Brush.radialGradient(
                    listOf(FuchoColors.accent.copy(alpha = 0.28f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 1400f,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 440.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoMark(72.dp)
                Spacer(Modifier.height(Spacing.l))
                Wordmark(32.sp)
                Spacer(Modifier.height(Spacing.m))
                Badge(stringResource(R.string.tagline), BadgeStyle.Lime)
                Spacer(Modifier.height(Spacing.l))
                Text(
                    stringResource(R.string.lead),
                    color = FuchoColors.textBody,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(Spacing.xxl))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(FuchoColors.card)
                        .border(1.dp, FuchoColors.border, CardShape)
                        .padding(horizontal = 20.dp, vertical = Spacing.xl),
                ) {
                    FormField(
                        title = stringResource(R.string.email),
                        icon = Icons.Outlined.Email,
                        value = email,
                        onValueChange = { email = it },
                        placeholder = stringResource(R.string.email_placeholder),
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                    )
                    Spacer(Modifier.height(Spacing.l))
                    var visible by remember { mutableStateOf(false) }
                    FormField(
                        title = stringResource(R.string.password),
                        icon = Icons.Outlined.Lock,
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        enabled = !loading,
                        modifier = Modifier.focusRequester(passwordFocus),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { signIn() }),
                        trailing = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    stringResource(if (visible) R.string.hide_password else R.string.show_password),
                                    tint = FuchoColors.textMuted,
                                )
                            }
                        },
                    )
                    TextButton(
                        onClick = { openSite(context, "user/recover") },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.forgot_password), color = FuchoColors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(Spacing.m))
                    PrimaryButton(
                        title = stringResource(if (loading) R.string.signing_in else R.string.sign_in),
                        onClick = { signIn() },
                        loading = loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(Spacing.s))
                TextButton(onClick = { showRegister = true }) {
                    Row {
                        Text(stringResource(R.string.no_account) + " ", color = FuchoColors.textMuted, fontSize = 15.sp)
                        Text(stringResource(R.string.sign_up), color = FuchoColors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    AlertHost(alerts)
}

/** Label above an inset text field with a leading icon; the border lights up while editing. */
@Composable
private fun FormField(
    title: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column {
        Text(title.uppercase(), style = EyebrowStyle, color = FuchoColors.textMuted)
        Spacer(Modifier.height(Spacing.s))
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder, color = FuchoColors.textFaint) },
            leadingIcon = { Icon(icon, null, tint = FuchoColors.textMuted, modifier = Modifier.size(20.dp)) },
            trailingIcon = trailing,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FuchoColors.inset,
                unfocusedContainerColor = FuchoColors.inset,
                disabledContainerColor = FuchoColors.inset,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = FuchoColors.accent,
                focusedTextColor = FuchoColors.textPrimary,
                unfocusedTextColor = FuchoColors.textPrimary,
            ),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, if (focused) FuchoColors.accent else FuchoColors.border, shape)
                .onFocusChanged { focused = it.isFocused },
        )
    }
}
