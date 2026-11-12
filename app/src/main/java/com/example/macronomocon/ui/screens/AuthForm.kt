package com.example.macronomocon.ui.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.ui.account.AccountState
import com.example.macronomocon.ui.common.ViewState

@Composable
fun AuthForm(
    state: ViewState<AccountState>,
    configured: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogle: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords must not enter saved instance state.
    var password by remember { mutableStateOf("") }
    var registering by rememberSaveable { mutableStateOf(false) }
    val valid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
        password.isNotEmpty() &&
        (!registering || password.length >= 8)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(if (registering) R.string.auth_create_account else R.string.auth_sign_in)
        )
        if (!configured) Text(stringResource(R.string.auth_not_configured))
        if (state.data.registrationSubmitted) Text(stringResource(R.string.auth_check_email))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = configured && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            supportingText = { if (registering) Text(stringResource(R.string.auth_password_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = configured && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (registering) onRegister(email, password) else onLogin(email, password)
                password = ""
            },
            enabled = configured && valid && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    if (registering) R.string.auth_create_account else R.string.auth_sign_in
                )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.auth_or),
                style = MaterialTheme.typography.labelMedium
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        // Google creates the account on first use, so it serves both sign-in and registration.
        OutlinedButton(
            onClick = onGoogle,
            enabled = configured && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(R.string.auth_continue_with_google))
        }
        TextButton(onClick = { registering = !registering }, enabled = !state.isLoading) {
            Text(
                stringResource(
                    if (registering) R.string.auth_have_account else R.string.auth_need_account
                )
            )
        }
    }
}
