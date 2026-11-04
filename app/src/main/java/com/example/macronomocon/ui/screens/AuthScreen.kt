package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.ui.account.AccountState
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.StateFeedback

@Composable
fun AuthScreen(
    state: ViewState<AccountState>,
    configured: Boolean,
    checkingSession: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    stringResource(R.string.auth_welcome),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (checkingSession) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.auth_restoring_session))
                } else {
                    StateFeedback(state, onDismiss)
                    AuthForm(state, configured, onLogin, onRegister, onGoogle)
                }
            }
        }
    }
}
