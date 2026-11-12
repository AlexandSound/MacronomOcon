package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.RecipePreferences
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.account.AccountState
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.StateFeedback
import kotlinx.coroutines.launch

private object ProfileLimits {
    const val DISPLAY_NAME = 100
    const val PREFERENCE_TEXT = 500
    const val MIN_PASSWORD = 6
}

private fun entries(text: String): List<String> =
    text.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()

@Composable
internal fun EditProfileDialog(
    user: User,
    state: ViewState<AccountState>,
    onSave: suspend (User) -> Boolean,
    onDismissError: () -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember(user.userId) { mutableStateOf(user.displayName) }
    var restrictions by remember(user.userId) {
        mutableStateOf(user.preferences.dietaryRestrictions.joinToString(", "))
    }
    var cuisines by remember(user.userId) {
        mutableStateOf(user.preferences.preferredCuisines.joinToString(", "))
    }
    var methods by remember(user.userId) {
        mutableStateOf(user.preferences.cookingMethods.joinToString(", "))
    }
    var submitting by remember { mutableStateOf(false) }
    val busy = state.isLoading || submitting
    val scope = rememberCoroutineScope()
    val preferences = user.preferences.copy(
        dietaryRestrictions = entries(restrictions),
        preferredCuisines = entries(cuisines),
        cookingMethods = entries(methods)
    )
    val valid = displayName.isNotBlank() && displayName.length <= ProfileLimits.DISPLAY_NAME &&
        restrictions.length <= ProfileLimits.PREFERENCE_TEXT &&
        cuisines.length <= ProfileLimits.PREFERENCE_TEXT &&
        methods.length <= ProfileLimits.PREFERENCE_TEXT &&
        runCatching { RecipePreferences.from(preferences) }.isSuccess
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StateFeedback(state, onDismissError)
                OutlinedTextField(
                    displayName,
                    { displayName = it },
                    label = { Text(stringResource(R.string.profile_display_name)) },
                    enabled = !busy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    restrictions,
                    { restrictions = it },
                    label = { Text(stringResource(R.string.profile_dietary_restrictions)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.profile_dietary_help))
                OutlinedTextField(
                    cuisines,
                    { cuisines = it },
                    label = { Text(stringResource(R.string.profile_cuisines)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.profile_cuisines_help))
                OutlinedTextField(
                    methods,
                    { methods = it },
                    label = { Text(stringResource(R.string.profile_cooking_methods)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.profile_methods_help))
                if (!valid) Text(stringResource(R.string.profile_invalid_input))
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = {
                    submitting = true
                    scope.launch {
                        try {
                            if (onSave(
                                    user.copy(
                                        displayName = displayName.trim(),
                                        preferences = preferences
                                    )
                                )
                            ) {
                                onDismiss()
                            }
                        } finally {
                            submitting = false
                        }
                    }
                }
            ) { Text(stringResource(R.string.save_profile)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun ChangeCredentialDialog(
    passwordMode: Boolean,
    state: ViewState<AccountState>,
    onSubmit: suspend (String) -> Boolean,
    onDismissError: () -> Unit,
    onDismiss: () -> Unit
) {
    // Passwords are transient, never saved across process recreation or persisted in ViewModel state.
    var value by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val busy = state.isLoading || submitting
    val scope = rememberCoroutineScope()
    val valid = if (passwordMode) {
        value.length >= ProfileLimits.MIN_PASSWORD && value == confirmation
    } else {
        android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() &&
            !value.trim().equals(state.data.user?.email, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                stringResource(
                    if (passwordMode) R.string.change_password else R.string.change_email
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StateFeedback(state, onDismissError)
                Text(
                    stringResource(
                        if (passwordMode) {
                            R.string.password_change_help
                        } else {
                            R.string.email_change_help
                        }
                    )
                )
                OutlinedTextField(
                    value,
                    { value = it },
                    label = {
                        Text(
                            stringResource(
                                if (passwordMode) R.string.new_password else R.string.new_email
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (passwordMode) {
                            KeyboardType.Password
                        } else {
                            KeyboardType.Email
                        }
                    ),
                    visualTransformation = if (passwordMode) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    enabled = !busy,
                    singleLine = true
                )
                if (passwordMode) {
                    OutlinedTextField(
                        confirmation,
                        { confirmation = it },
                        label = { Text(stringResource(R.string.confirm_new_password)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !busy,
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = {
                    val submitted = if (passwordMode) value else value.trim()
                    value = ""
                    confirmation = ""
                    submitting = true
                    scope.launch {
                        try {
                            if (onSubmit(submitted)) onDismiss()
                        } finally {
                            submitting = false
                        }
                    }
                }
            ) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
