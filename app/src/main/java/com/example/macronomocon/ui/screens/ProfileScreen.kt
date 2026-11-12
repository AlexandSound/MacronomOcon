package com.example.macronomocon.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.account.AccountState
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.ScreenHeading
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.components.WarmButton

@Composable
fun ProfileScreen(
    state: ViewState<AccountState>,
    onNotifications: (Boolean) -> Unit,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    liveAuth: Boolean = false,
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onUpdateAccount: suspend (User) -> Boolean = { false },
    onChangeEmail: suspend (String) -> Boolean = { false },
    onChangePassword: suspend (String) -> Boolean = { false },
    onDismissNotice: () -> Unit = {}
) {
    val user = state.data.user
    var editProfile by rememberSaveable(user?.userId) { mutableStateOf(false) }
    var credential by rememberSaveable(user?.userId) { mutableStateOf<String?>(null) }
    var confirmDeletion by rememberSaveable(user?.userId) { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ScreenHeading(
                stringResource(R.string.profile_title),
                stringResource(R.string.profile_subtitle)
            )
        }
        item { StateFeedback(state, onDismiss) }
        state.data.notice?.let { notice ->
            item {
                Column {
                    Text(notice.resolve())
                    TextButton(onClick = onDismissNotice) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }
        }
        if (user != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(user.email)
                        Text(
                            pluralStringResource(
                                R.plurals.profile_saved,
                                user.favoriteRecipeIds.size,
                                user.favoriteRecipeIds.size
                            )
                        )
                    }
                }
            }
            item {
                WarmButton(stringResource(R.string.browse_saved), onSaved, Modifier.fillMaxWidth())
            }
            item {
                WarmButton(
                    stringResource(R.string.edit_profile),
                    {
                        onDismiss()
                        editProfile = true
                    },
                    enabled = !state.isLoading
                )
            }
            if (liveAuth) {
                item {
                    Column {
                        TextButton(onClick = {
                            onDismiss()
                            onDismissNotice()
                            credential = "email"
                        }, enabled = !state.isLoading) {
                            Text(stringResource(R.string.change_email))
                        }
                        TextButton(onClick = {
                            onDismiss()
                            onDismissNotice()
                            credential = "password"
                        }, enabled = !state.isLoading) {
                            Text(stringResource(R.string.change_password))
                        }
                        TextButton(onClick = {
                            confirmDeletion = true
                        }, enabled = !state.isLoading) {
                            Text(
                                stringResource(R.string.delete_account),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onLogout,
                        enabled = !state.isLoading,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.auth_sign_out))
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.preferences_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                ReminderPreference(
                    user.preferences.notificationsEnabled,
                    !state.isLoading,
                    onNotifications
                )
            }
        }
    }
    if (user != null) {
        if (editProfile) {
            EditProfileDialog(user, state, onUpdateAccount, onDismiss) { editProfile = false }
        }
        credential?.let { mode ->
            ChangeCredentialDialog(
                passwordMode = mode == "password",
                state = state,
                onSubmit = if (mode == "password") onChangePassword else onChangeEmail,
                onDismissError = onDismiss,
                onDismiss = { credential = null }
            )
        }
        if (confirmDeletion) {
            AlertDialog(
                onDismissRequest = { if (!state.isLoading) confirmDeletion = false },
                title = { Text(stringResource(R.string.delete_account_confirmation_title)) },
                text = { Text(stringResource(R.string.delete_account_confirmation_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDeletion = false
                        onDeleteAccount()
                    }, enabled = !state.isLoading) {
                        Text(
                            stringResource(R.string.delete_account),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        confirmDeletion = false
                    }, enabled = !state.isLoading) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
private fun ReminderPreference(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.notifications_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val label = stringResource(R.string.notifications_title)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}
