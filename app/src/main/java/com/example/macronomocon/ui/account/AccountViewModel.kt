package com.example.macronomocon.ui.account

import androidx.lifecycle.viewModelScope
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage
import kotlinx.coroutines.launch

data class AccountState(
    val user: User? = null,
    val registrationSubmitted: Boolean = false,
    val requiresOtp: Boolean = false
)

class AccountViewModel(private val repository: AccountRepository) :
    StateViewModel<AccountState>(AccountState()) {
    init {
        viewModelScope.launch {
            repository.signedOutEvents.collect { updateData { AccountState() } }
        }
        viewModelScope.launch {
            repository.signedInEvents.collect { loadAccount() }
        }
    }

    fun loadAccount() = action(UiMessage.ERROR_LOAD_YOUR_ACCOUNT) {
        val user = repository.findUser()
        updateData { it.copy(user = user) }
    }

    fun register(email: String, password: String) = action(
        UiMessage.ERROR_REGISTER,
        { (it as? AccountException)?.reason?.let(::registrationError) }
    ) {
        repository.register(email.trim(), password)
        val user = repository.findUser()
        updateData { it.copy(registrationSubmitted = true, user = user) }
    }

    private fun registrationError(reason: AccountException.Reason) = when (reason) {
        AccountException.Reason.EMAIL_IN_USE -> UiMessage.ERROR_EMAIL_IN_USE
        AccountException.Reason.INVALID_EMAIL -> UiMessage.ERROR_INVALID_EMAIL
        AccountException.Reason.WEAK_PASSWORD -> UiMessage.ERROR_WEAK_PASSWORD
        AccountException.Reason.EMAIL_NOT_AUTHORIZED -> UiMessage.ERROR_EMAIL_NOT_AUTHORIZED
        AccountException.Reason.EMAIL_RATE_LIMITED -> UiMessage.ERROR_EMAIL_RATE_LIMITED
        AccountException.Reason.SIGNUP_DISABLED -> UiMessage.ERROR_SIGNUP_DISABLED
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

    fun signInWithGoogle() = action(UiMessage.ERROR_GOOGLE_SIGN_IN) {
        repository.signInWithGoogle()
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
