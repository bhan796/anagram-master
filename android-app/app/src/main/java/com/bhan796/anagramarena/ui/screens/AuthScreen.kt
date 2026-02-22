package com.bhan796.anagramarena.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun AuthScreen(
    contentPadding: PaddingValues,
    isSubmitting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onContinueGuest: () -> Unit,
    onGoogleToken: (String) -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val googleClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (!token.isNullOrBlank()) {
                onGoogleToken(token)
            } else {
                onAuthError("Google sign-in did not return a token.")
            }
        } catch (_: Exception) {
            onAuthError("Google sign-in failed.")
        }
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle(if (mode == "login") "Sign In" else "Create Account")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.35f), RoundedCornerShape(sdp(6.dp)))
                .padding(sdp(12.dp)),
            verticalArrangement = Arrangement.spacedBy(sdp(10.dp))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(sdp(8.dp))) {
                ModeButton(
                    text = "SIGN IN",
                    selected = mode == "login",
                    enabled = !isSubmitting,
                    onClick = { mode = "login" },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    text = "CREATE",
                    selected = mode == "register",
                    enabled = !isSubmitting,
                    onClick = { mode = "register" },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorCyan,
                    unfocusedBorderColor = ColorDimText.copy(alpha = 0.55f),
                    focusedTextColor = ColorWhite,
                    unfocusedTextColor = ColorWhite
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorCyan,
                    unfocusedBorderColor = ColorDimText.copy(alpha = 0.55f),
                    focusedTextColor = ColorWhite,
                    unfocusedTextColor = ColorWhite
                )
            )

            if (!error.isNullOrBlank()) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = ColorRed)
            }

            ArcadeButton(
                text = if (isSubmitting) "PLEASE WAIT..." else if (mode == "login") "SIGN IN" else "CREATE ACCOUNT",
                onClick = {
                    val normalizedEmail = email.trim()
                    if (normalizedEmail.isBlank() || password.isBlank()) return@ArcadeButton
                    if (mode == "login") onLogin(normalizedEmail, password) else onRegister(normalizedEmail, password)
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
            ArcadeButton(
                text = "CONTINUE WITH GOOGLE",
                onClick = {
                    if (activity == null) {
                        onAuthError("Google sign-in is unavailable on this screen.")
                        return@ArcadeButton
                    }
                    if (googleClientId.isBlank()) {
                        onAuthError("Google sign-in is not configured.")
                        return@ArcadeButton
                    }
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(googleClientId)
                        .build()
                    val client = GoogleSignIn.getClient(activity, options)
                    googleAuthLauncher.launch(client.signInIntent)
                },
                enabled = !isSubmitting && googleClientId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            ArcadeButton(
                text = "CONTINUE WITH FACEBOOK",
                onClick = {
                    onAuthError("Facebook sign-in is currently available on web.")
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            ArcadeButton(
                text = "CONTINUE AS GUEST",
                onClick = onContinueGuest,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) ColorCyan.copy(alpha = 0.85f) else ColorDimText.copy(alpha = 0.45f)
    val textColor = if (selected) ColorWhite else ColorDimText
    ArcadeButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        accentColor = border
    )
}
