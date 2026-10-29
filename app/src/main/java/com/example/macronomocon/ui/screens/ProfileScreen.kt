package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val user = state.data.user
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
        if (user != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
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
            item {
                WarmButton(
                    stringResource(R.string.log_out),
                    onLogOut,
                    Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
            }
            item {
                TextButton(
                    onClick = { confirmDeletion = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text(
                        stringResource(R.string.delete_account),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.demo_badge),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(stringResource(R.string.demo_explanation))
                }
            }
        }
    }
    if (confirmDeletion && user != null) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
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
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
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