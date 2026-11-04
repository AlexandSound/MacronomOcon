package com.example.macronomocon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.macronomocon.data.mock.MockAppContainer
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.di.AppContainer
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.MacronomoconApp
import com.example.macronomocon.ui.theme.MacronomOconTheme
import org.junit.Rule
import org.junit.Test

class AuthNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loginOpensKitchenAndProfileLogoutReturnsToEntryScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun text(id: Int) = context.getString(id)
        val demo = MockAppContainer.getInstance(context.resources)
        val accounts = object : AccountRepository by demo.accounts {
            private var user: User? = null
            override suspend fun findUser() = user
            override suspend fun authenticate(
                email: String,
                password: String
            ): AuthenticationResult {
                user = User("test-user", email, "Student")
                return AuthenticationResult.SignedIn(requireNotNull(user))
            }
            override suspend fun signOut() {
                user = null
            }
        }
        val container = object : AppContainer by demo {
            override val liveAuth = true
            override val accounts = accounts
        }
        compose.setContent { MacronomOconTheme { MacronomoconApp(container) } }
        compose.onNodeWithText(text(R.string.nav_pantry)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.auth_email)).performTextInput("student@example.com")
        compose.onNodeWithText(text(R.string.auth_password)).performTextInput("password123")
        compose.onNode(hasText(text(R.string.auth_sign_in)) and hasClickAction()).performClick()
        compose.onNodeWithText(text(R.string.nav_pantry)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_profile)).performClick()
        compose.onNodeWithText(text(R.string.auth_sign_out)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.auth_email)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_pantry)).assertDoesNotExist()
    }
}
