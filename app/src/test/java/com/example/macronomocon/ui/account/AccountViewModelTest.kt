package com.example.macronomocon.ui.account

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import com.example.macronomocon.ui.common.UiMessage
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAccounts()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun cleanup() = Dispatchers.resetMain()

    @Test
    fun registrationAwaitingConfirmationDoesNotPretendToSignIn() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.register(" student@example.com ", "password123").join()
        assertTrue(vm.uiState.value.data.registrationSubmitted)
        assertNull(vm.uiState.value.data.user)
        assertEquals("student@example.com", repository.email)
    }

    @Test
    fun registrationRejectionShowsSpecificReason() = runTest(dispatcher) {
        repository.registerFailure = AccountException(AccountException.Reason.EMAIL_RATE_LIMITED)
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals(UiMessage.ERROR_EMAIL_RATE_LIMITED, vm.uiState.value.errorMessage)
        assertTrue(!vm.uiState.value.data.registrationSubmitted)
    }

    @Test
    fun unexpectedRegistrationFailureShowsGenericError() = runTest(dispatcher) {
        repository.registerFailure = IllegalStateException("Sensitive transport error")
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals(UiMessage.ERROR_REGISTER, vm.uiState.value.errorMessage)
    }

    @Test
    fun autoConfirmedRegistrationLoadsUser() = runTest(dispatcher) {
        repository.autoConfirm = true
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals("id", vm.uiState.value.data.user?.userId)
    }

    @Test
    fun signOutClearsAccount() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        vm.logOut().join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun sessionInvalidationClearsAccount() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        repository.signedOutEvents.emit(Unit)
        advanceUntilIdle()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun googleRedirectSessionLoadsUser() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        assertTrue(repository.googleStarted)
        assertNull(vm.uiState.value.data.user)
        repository.current = User("google-id", "student@gmail.com", "Student")
        repository.signedInEvents.emit(Unit)
        advanceUntilIdle()
        assertEquals("google-id", vm.uiState.value.data.user?.userId)
    }

    @Test
    fun failedGoogleLaunchShowsSafeError() = runTest(dispatcher) {
        repository.fail = true
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        assertEquals(UiMessage.ERROR_GOOGLE_SIGN_IN, vm.uiState.value.errorMessage)
    }

    @Test
    fun failedLoginShowsSafeError() = runTest(dispatcher) {
        repository.fail = true
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "wrong").join()
        assertNull(vm.uiState.value.data.user)
        assertEquals(UiMessage.ERROR_SIGN_IN, vm.uiState.value.errorMessage)
    }

    @Test
    fun successfulDeletionClearsAccountButFailedDeletionKeepsIt() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        repository.fail = true
        vm.deleteAccount().join()
        assertEquals("id", vm.uiState.value.data.user?.userId)
        assertEquals(UiMessage.ERROR_DELETE_YOUR_ACCOUNT, vm.uiState.value.errorMessage)
        repository.fail = false
        vm.deleteAccount().join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun emailChangeDoesNotPretendConfirmationAlreadyHappened() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("old@example.com", "password123").join()
        vm.changeEmail(" new@example.com ").join()
        assertEquals("new@example.com", repository.email)
        assertEquals("old@example.com", vm.uiState.value.data.user?.email)
        assertEquals(UiMessage.EMAIL_CHANGE_REQUESTED, vm.uiState.value.data.notice)
    }

    @Test
    fun failedPasswordChangeShowsErrorAndNoSuccessNotice() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("old@example.com", "password123").join()
        repository.fail = true
        vm.changePassword("new-password").join()
        assertEquals(UiMessage.ERROR_UPDATE_PASSWORD, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.data.notice)
    }

    @Test
    fun profileSaveKeepsPreferencesAndRejectsUnsupportedRestrictions() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        val user = requireNotNull(vm.uiState.value.data.user)
        vm.updateAccount(
            user.copy(
                displayName = "Thomas",
                preferences = UserPreferences(
                    dietaryRestrictions = listOf("vegan"),
                    preferredCuisines = listOf("italian"),
                    cookingMethods = listOf("grilling")
                )
            )
        ).join()
        assertEquals("Thomas", vm.uiState.value.data.user?.displayName)
        assertEquals(listOf("grilling"), repository.current?.preferences?.cookingMethods)
        vm.updateAccount(
            user.copy(
                preferences = UserPreferences(
                    dietaryRestrictions = listOf("unsupported")
                )
            )
        ).join()
        assertEquals(UiMessage.ERROR_UPDATE_YOUR_ACCOUNT, vm.uiState.value.errorMessage)
        assertEquals(listOf("vegan"), repository.current?.preferences?.dietaryRestrictions)
    }

    @Test
    fun profileRefreshPreservesCommentsAndPantryForSameAccount() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        val pantry = requireNotNull(vm.user).pantry
        vm.loadComments("recipe").join()
        vm.publishComment("Helpful recipe").join()
        vm.updatePreferences(UserPreferences()).join()
        vm.loadAccount().join()
        assertTrue(pantry === requireNotNull(vm.user).pantry)
        val own = requireNotNull(vm.user).loadComments("recipe").single()
        assertEquals("Helpful recipe", own.text)
        assertEquals(own, repository.discussion.single())
        vm.deleteComment(own).join()
        assertTrue(requireNotNull(vm.user).comments.isEmpty())
        assertTrue(repository.discussion.isEmpty())
    }

    @Test
    fun loadedCommentsAreSharedAndOnlyOtherAuthorsCanBeLiked() = runTest(dispatcher) {
        val neighbor = Comment("neighbor", "recipe", "neighbor-id", "Add lemon", authorName = "Sam")
        repository.discussion += neighbor
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        vm.loadComments("recipe").join()
        assertEquals(listOf(neighbor), requireNotNull(vm.user).loadComments("recipe"))
        vm.likeComment(neighbor).join()
        assertEquals(setOf("id"), requireNotNull(vm.user).comments.single().likedByUserIds)
        vm.publishComment("Mine").join()
        val own = requireNotNull(vm.user).loadComments("recipe").single { it.authorId == "id" }
        vm.likeComment(own).join()
        assertEquals(UiMessage.ERROR_LIKE_THIS_COMMENT, vm.uiState.value.errorMessage)
        assertEquals(0, repository.discussion.single { it.commentId == own.commentId }.likeCount())
        vm.deleteComment(neighbor).join()
        assertEquals(UiMessage.ERROR_DELETE_THIS_COMMENT, vm.uiState.value.errorMessage)
        assertTrue(neighbor.commentId in repository.discussion.map { it.commentId })
    }

    @Test
    fun failedCommentLoadShowsSafeError() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        repository.fail = true
        vm.loadComments("recipe").join()
        assertEquals(UiMessage.ERROR_LOAD_COMMENTS, vm.uiState.value.errorMessage)
    }

    @Test
    fun changedAccountDoesNotInheritPreviousSessionCommentsOrPantry() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        val pantry = requireNotNull(vm.user).pantry
        vm.loadComments("recipe").join()
        vm.publishComment("Private session comment").join()
        repository.current = User("other-id", "other@example.com", "Other")
        vm.loadAccount().join()
        assertTrue(requireNotNull(vm.user).comments.isEmpty())
        assertTrue(pantry !== requireNotNull(vm.user).pantry)
    }

    @Test
    fun deletionAndSignOutClearLoadedCommentsAndSession() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        vm.loadComments("recipe").join()
        vm.publishComment("Before deletion").join()
        vm.deleteAccount().join()
        assertEquals(AccountState(), vm.uiState.value.data)
        vm.logIn("student@example.com", "password123").join()
        vm.loadComments("recipe").join()
        vm.logOut().join()
        assertEquals(AccountState(), vm.uiState.value.data)
    }

    private class FakeAccounts : AccountRepository {
        override val signedOutEvents = MutableSharedFlow<Unit>()
        override val signedInEvents = MutableSharedFlow<Unit>()
        var googleStarted = false
        var email = ""
        var autoConfirm = false
        var fail = false
        var registerFailure: Exception? = null
        var current: User? = null
        override suspend fun register(email: String, password: String) {
            registerFailure?.let { throw it }
            this.email = email
            if (autoConfirm) current = User("id", email, "Student")
        }
        override suspend fun authenticate(email: String, password: String): AuthenticationResult {
            check(!fail) { "Sensitive transport error" }
            current = User("id", email, "Student")
            return AuthenticationResult.SignedIn(requireNotNull(current))
        }
        override suspend fun signInWithGoogle() {
            check(!fail) { "No browser" }
            googleStarted = true
        }
        override suspend fun findUser() = current
        override suspend fun signOut() {
            current = null
        }
        override suspend fun verifyOtp(code: String) = requireNotNull(current)
        override suspend fun saveUser(user: User) = user.also { current = it }
        override suspend fun savePreferences(preferences: UserPreferences) =
            saveUser(requireNotNull(current).copy(preferences = preferences))
        override suspend fun updateEmail(email: String): String {
            check(!fail)
            this.email = email
            return requireNotNull(current).email
        }
        override suspend fun updatePassword(password: String) {
            check(!fail)
        }
        override suspend fun deleteUser() {
            check(!fail)
            current = null
        }
        override suspend fun loadPantryItems(): List<PantryItem> = emptyList()
        override suspend fun savePantryItem(item: PantryItem) = Unit
        override suspend fun deletePantryItem(item: PantryItem) = Unit

        /** Stands in for the shared server table: visible to every account. */
        val discussion = mutableListOf<Comment>()
        override suspend fun loadComments(recipeId: String): List<Comment> {
            check(!fail) { "Sensitive transport error" }
            return discussion.filter { it.recipeId == recipeId }
        }
        override suspend fun publishComment(
            recipeId: String,
            text: String,
            imageUrl: String?
        ): Comment {
            val author = requireNotNull(current)
            return Comment(UUID.randomUUID().toString(), recipeId, author.userId, text, imageUrl)
                .also { discussion += it }
        }
        override suspend fun deleteComment(comment: Comment) {
            check(discussion.removeAll { it.commentId == comment.commentId })
        }
        override suspend fun likeComment(comment: Comment): Comment {
            val index = discussion.indexOfFirst { it.commentId == comment.commentId }
            return discussion[index].like(requireNotNull(current)).also { discussion[index] = it }
        }
    }
}
