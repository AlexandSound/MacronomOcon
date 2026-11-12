package com.example.macronomocon.ui.account

import androidx.lifecycle.viewModelScope
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.RecipePreferences
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage
import kotlinx.coroutines.launch

data class CommentState(val recipeId: String? = null, val comments: List<Comment> = emptyList())

data class AccountState(
    val user: User? = null,
    val registrationSubmitted: Boolean = false,
    val requiresOtp: Boolean = false,
    val commentRecipeId: String? = null,
    val notice: UiMessage? = null
)

class AccountViewModel(private val repository: AccountRepository) :
    StateViewModel<AccountState>(AccountState()) {
    val user: User? get() = data.user

    init {
        viewModelScope.launch {
            repository.signedOutEvents.collect { updateData { AccountState() } }
        }
        viewModelScope.launch {
            repository.signedInEvents.collect { loadAccount() }
        }
    }

    fun loadAccount() = action(UiMessage.ERROR_LOAD_YOUR_ACCOUNT) {
        val user = mergeSession(repository.findUser())
        updateData { it.copy(user = user) }
    }

    fun register(email: String, password: String) = action(
        UiMessage.ERROR_REGISTER,
        { (it as? AccountException)?.reason?.let(::registrationError) }
    ) {
        repository.register(email.trim(), password)
        val user = mergeSession(repository.findUser())
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
        require(user.displayName.isNotBlank())
        RecipePreferences.from(user.preferences)
        val saved = mergeSession(repository.saveUser(user))
        updateData { it.copy(user = saved) }
    }

    fun updatePreferences(preferences: UserPreferences) = action(UiMessage.ERROR_SAVE_PREFERENCES) {
        val saved = mergeSession(repository.savePreferences(preferences))
        updateData { it.copy(user = saved) }
    }

    fun clearNotice() = updateData { it.copy(notice = null) }

    fun changeEmail(email: String) = action(
        UiMessage.ERROR_UPDATE_EMAIL,
        { (it as? AccountException)?.reason?.let(::registrationError) }
    ) {
        clearNotice()
        val trimmed = email.trim()
        require(trimmed.isNotBlank() && '@' in trimmed && trimmed.none { it.isWhitespace() })
        val effectiveEmail = repository.updateEmail(trimmed)
        updateData {
            it.copy(
                user = it.user?.let { user ->
                    if (effectiveEmail.isBlank()) user else user.copy(email = effectiveEmail)
                },
                notice = UiMessage.EMAIL_CHANGE_REQUESTED
            )
        }
    }

    fun changePassword(password: String) = action(
        UiMessage.ERROR_UPDATE_PASSWORD,
        { (it as? AccountException)?.reason?.let(::registrationError) }
    ) {
        clearNotice()
        require(password.isNotBlank())
        repository.updatePassword(password)
        updateData { it.copy(notice = UiMessage.PASSWORD_UPDATED) }
    }

    /** Refresh remote profile fields without dropping this session's pantry or loaded comments. */
    private fun mergeSession(remote: User?): User? {
        val current = data.user
        return if (remote != null && current?.userId == remote.userId) {
            remote.copy(pantry = current.pantry, comments = current.comments)
        } else {
            remote
        }
    }

    /** Loads every user's comments on [recipeId] into the signed-in [User]. */
    fun loadComments(recipeId: String) = action(UiMessage.ERROR_LOAD_COMMENTS) {
        requireNotNull(data.user)
        require(recipeId.isNotBlank())
        updateData { it.copy(commentRecipeId = recipeId) }
        val shared = repository.loadComments(recipeId)
        updateData { state ->
            state.copy(user = state.user?.showComments(recipeId, shared))
        }
    }

    fun publishComment(text: String, imageUrl: String? = null) =
        action(UiMessage.ERROR_PUBLISH_THIS_COMMENT) {
            val recipeId = requireNotNull(data.commentRecipeId)
            requireNotNull(data.user)
            require(text.isNotBlank() || !imageUrl.isNullOrBlank())
            val published = repository.publishComment(recipeId, text.trim(), imageUrl)
            updateData { it.copy(user = requireNotNull(it.user).publishComment(published)) }
        }

    fun deleteComment(comment: Comment) = action(UiMessage.ERROR_DELETE_THIS_COMMENT) {
        // Validates authorship before the request; the server enforces it again.
        requireNotNull(data.user).deleteComment(comment.commentId)
        repository.deleteComment(comment)
        updateData { it.copy(user = requireNotNull(it.user).deleteComment(comment.commentId)) }
    }

    fun likeComment(comment: Comment) = action(UiMessage.ERROR_LIKE_THIS_COMMENT) {
        // Rejects self-likes before the request; the server enforces it again.
        requireNotNull(data.user).likeComment(comment.commentId)
        val liked = repository.likeComment(comment)
        updateData { it.copy(user = requireNotNull(it.user).likeComment(liked)) }
    }

    fun deleteAccount() = action(UiMessage.ERROR_DELETE_YOUR_ACCOUNT) {
        repository.deleteUser()
        updateData { AccountState() }
    }
}
