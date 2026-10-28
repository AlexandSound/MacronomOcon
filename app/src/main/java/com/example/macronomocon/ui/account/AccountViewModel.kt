package com.example.macronomocon.ui.account

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage

data class AccountState(
    val user: User? = null,
    val registrationSubmitted: Boolean = false,
    val requiresOtp: Boolean = false
)

class AccountViewModel(private val repository: AccountRepository) :
    StateViewModel<AccountState>(AccountState()) {
    fun loadAccount() = action(UiMessage.ERROR_LOAD_YOUR_ACCOUNT) {
        val user = repository.findUser()
        updateData { it.copy(user = user) }
    }

    fun register(email: String, password: String) = action(UiMessage.ERROR_REGISTER) {
        repository.register(email.trim(), password)
        updateData { it.copy(registrationSubmitted = true) }
    }

    fun logIn(email: String, password: String) = action(UiMessage.ERROR_SIGN_IN) {
        val result = repository.authenticate(email.trim(), password)
        updateData {
            when (result) {
                is AuthenticationResult.SignedIn -> AccountState(user = result.user)
                AuthenticationResult.AwaitingOtp -> AccountState(requiresOtp = true)
            }
        }
    }

    fun verifyOtp(code: String) = action(UiMessage.ERROR_VERIFY_THE_CODE) {
        val user = repository.verifyOtp(code.trim())
        updateData { AccountState(user) }
    }

    fun logOut() = action(UiMessage.ERROR_SIGN_OUT) {
        repository.signOut()
        updateData { AccountState() }
    }

    fun updateAccount(user: User) = action(UiMessage.ERROR_UPDATE_YOUR_ACCOUNT) {
        val saved = repository.saveUser(user)
        updateData { it.copy(user = saved) }
    }

    fun updatePreferences(preferences: UserPreferences) = action(UiMessage.ERROR_SAVE_PREFERENCES) {
        val saved = repository.savePreferences(preferences)
        updateData { it.copy(user = saved) }
    }

    fun deleteAccount() = action(UiMessage.ERROR_DELETE_YOUR_ACCOUNT) {
        repository.deleteUser()
        updateData { AccountState() }
    }
}
