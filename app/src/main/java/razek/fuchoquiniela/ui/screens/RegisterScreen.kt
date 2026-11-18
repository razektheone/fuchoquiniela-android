package razek.fuchoquiniela.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import razek.fuchoquiniela.R
import razek.fuchoquiniela.data.ApiError
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.RegisterRequest
import razek.fuchoquiniela.data.RegisterResponse
import razek.fuchoquiniela.ui.components.AlertHost
import razek.fuchoquiniela.ui.components.AlertState
import razek.fuchoquiniela.ui.components.FuchoTopBar
import razek.fuchoquiniela.ui.components.PrimaryButton
import razek.fuchoquiniela.ui.components.gutter
import razek.fuchoquiniela.ui.components.openSite
import razek.fuchoquiniela.ui.theme.EyebrowStyle
import razek.fuchoquiniela.ui.theme.FuchoColors
import razek.fuchoquiniela.ui.theme.Spacing

private const val MIN_PASSWORD = 6

/**
 * "Crear cuenta": native sign-up (POST auth/register) with the web form's fields and rules.
 * The server emails a verification link and returns no token, so on success the login screen
 * takes over with the email filled in and a "Revisa tu correo" alert.
 */
@Composable
fun RegisterScreen(onCancel: () -> Unit, onRegistered: (RegisterResponse) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val alerts = remember { AlertState() }
    val title = stringResource(R.string.create_account)

    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    BackHandler(enabled = !saving, onBack = onCancel)

    fun submit() {
        // Only hide the keyboard: clearing focus would hand it to the ✕ button, and the release
        // of a hardware Enter (Go) would then "click" it and close the form.
        keyboard?.hide()
        val values = listOf(firstName, lastName, nickname, email).map { it.trim() }
        // Same checks as iOS before sending; the server repeats them with its own messages.
        val problem = when {
            values.any { it.isEmpty() } || password.isEmpty() || confirmation.isEmpty() -> R.string.fill_all_fields
            password.length < MIN_PASSWORD -> R.string.password_too_short
            password != confirmation -> R.string.passwords_dont_match
            else -> null
        }
        if (problem != null) {
            alerts.show(title, context.getString(problem))
            return
        }
        saving = true
        scope.launch {
            try {
                val (first, last, nick, mail) = values
                onRegistered(FuchoApi.register(RegisterRequest(first, last, nick, mail, password, confirmation)))
            } catch (e: ApiError) {
                alerts.show(context, e, title)
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        containerColor = FuchoColors.background,
        topBar = {
            FuchoTopBar(title) {
                IconButton(onClick = onCancel, enabled = !saving) {
                    Icon(Icons.Filled.Close, stringResource(R.string.cancel))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutter, vertical = Spacing.l),
        ) {
            Text(stringResource(R.string.register_lead), color = FuchoColors.textBody, fontSize = 15.sp)
            Spacer(Modifier.height(Spacing.xl))

            val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                RegisterField(
                    stringResource(R.string.first_name), firstName, { firstName = it }, !saving,
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    actions = next, modifier = Modifier.weight(1f),
                )
                RegisterField(
                    stringResource(R.string.last_name), lastName, { lastName = it }, !saving,
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    actions = next, modifier = Modifier.weight(1f),
                )
            }
            RegisterField(
                stringResource(R.string.nickname), nickname, { nickname = it }, !saving,
                placeholder = stringResource(R.string.nickname_placeholder),
                keyboard = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                actions = next,
            )
            RegisterField(
                stringResource(R.string.email), email, { email = it }, !saving,
                placeholder = stringResource(R.string.email_placeholder),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                actions = next,
            )
            var visible by remember { mutableStateOf(false) }
            val toggle: @Composable () -> Unit = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        stringResource(if (visible) R.string.hide_password else R.string.show_password),
                        tint = FuchoColors.textMuted,
                    )
                }
            }
            val hidden = if (visible) VisualTransformation.None else PasswordVisualTransformation()
            // Next goes straight to the confirmation; otherwise it would land on the eye button.
            val confirmFocus = remember { FocusRequester() }
            RegisterField(
                stringResource(R.string.password), password, { password = it }, !saving,
                placeholder = stringResource(R.string.password_placeholder),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                actions = KeyboardActions(onNext = { confirmFocus.requestFocus() }), transformation = hidden, trailing = toggle,
            )
            RegisterField(
                stringResource(R.string.confirm_password), confirmation, { confirmation = it }, !saving,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                actions = KeyboardActions(onGo = { submit() }), transformation = hidden,
                fieldModifier = Modifier.focusRequester(confirmFocus),
            )

            Spacer(Modifier.height(Spacing.s))
            PrimaryButton(
                title = stringResource(if (saving) R.string.creating_account else R.string.create_account),
                onClick = { submit() },
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.l))
            TermsText()
        }
    }
    AlertHost(alerts)
}

/** "Al crear tu cuenta aceptas los Términos y condiciones y el Aviso de privacidad." with both linked. */
@Composable
private fun TermsText() {
    val context = LocalContext.current
    val terms = stringResource(R.string.terms)
    val privacy = stringResource(R.string.privacy)
    val template = stringResource(R.string.register_terms, "\u0000", "\u0001")
    val linkStyle = TextLinkStyles(SpanStyle(color = FuchoColors.accent, fontWeight = FontWeight.SemiBold))
    val text = buildAnnotatedString {
        template.forEach { char ->
            when (char) {
                '\u0000' -> withLink(LinkAnnotation.Clickable("terms", linkStyle) { openSite(context, "home/terms") }) { append(terms) }
                '\u0001' -> withLink(LinkAnnotation.Clickable("privacy", linkStyle) { openSite(context, "home/privacy") }) { append(privacy) }
                else -> append(char)
            }
        }
    }
    Text(text, color = FuchoColors.textMuted, fontSize = 13.sp)
}

private val FieldShape = RoundedCornerShape(10.dp)

/** Uppercase label over an inset field, like the login form; the border lights up while editing. */
@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboard: KeyboardOptions,
    actions: KeyboardActions,
    transformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.padding(bottom = Spacing.l)) {
        Text(label.uppercase(), style = EyebrowStyle, color = FuchoColors.textMuted)
        Spacer(Modifier.height(Spacing.s))
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            placeholder = placeholder?.let { { Text(it, color = FuchoColors.textFaint) } },
            trailingIcon = trailing,
            visualTransformation = transformation,
            keyboardOptions = keyboard,
            keyboardActions = actions,
            shape = FieldShape,
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
            modifier = fieldModifier
                .fillMaxWidth()
                .border(1.dp, if (focused) FuchoColors.accent else FuchoColors.border, FieldShape)
                .onFocusChanged { focused = it.isFocused },
        )
    }
}
