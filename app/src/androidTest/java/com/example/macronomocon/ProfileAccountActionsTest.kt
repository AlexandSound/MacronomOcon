package com.example.macronomocon

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.account.AccountState
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.screens.ProfileScreen
import com.example.macronomocon.ui.theme.MacronomOconTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileAccountActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun openDelete(onDelete: () -> Unit) {
        compose.setContent {
            MacronomOconTheme {
                ProfileScreen(
                    state = ViewState(AccountState(user = User("id", "test@example.com", "Test"))),
                    onNotifications = {},
                    onSaved = {},
                    onDismiss = {},
                    liveAuth = true,
                    onDeleteAccount = onDelete
                )
            }
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(
            hasText(text(R.string.delete_account))
        )
        compose.onNodeWithText(text(R.string.delete_account)).performClick()
    }

    @Test
    fun cancellingDeletionDoesNotCallTheAccountAction() {
        var deletions = 0
        openDelete { deletions++ }
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(
            text(R.string.delete_account_confirmation_title)
        ).assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, deletions) }
    }

    @Test
    fun deletionOnlyRunsAfterExplicitConfirmation() {
        var deletions = 0
        openDelete { deletions++ }
        compose.runOnIdle { assertEquals(0, deletions) }
        compose.onNode(
            hasText(text(R.string.delete_account)) and hasAnyAncestor(isDialog())
        ).performClick()
        compose.runOnIdle { assertEquals(1, deletions) }
    }
}
