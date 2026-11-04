package com.example.macronomocon.ui.account

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import com.example.macronomocon.ui.common.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAccounts()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun registrationAwaitingConfirmationMarksTheSubmission() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertTrue(vm.uiState.value.data.registrationSubmitted)
    }

    @Test
    fun registrationAwaitingConfirmationDoesNotSignTheUserIn() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun registrationTrimsTheEmailAddress() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.register(" student@example.com ", "password123").join()
        assertEquals("student@example.com", repository.email)
    }

    @Test
    fun registrationRejectionShowsTheSpecificReason() = runTest(dispatcher) {
        repository.registerFailure = AccountException(AccountException.Reason.EMAIL_RATE_LIMITED)
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals(UiMessage.ERROR_EMAIL_RATE_LIMITED, vm.uiState.value.errorMessage)
    }

    @Test
    fun aRejectedRegistrationIsNotMarkedAsSubmitted() = runTest(dispatcher) {
        repository.registerFailure = AccountException(AccountException.Reason.EMAIL_RATE_LIMITED)
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertFalse(vm.uiState.value.data.registrationSubmitted)
    }

    @Test
    fun anUnexpectedRegistrationFailureShowsAGenericError() = runTest(dispatcher) {
        repository.registerFailure = IllegalStateException("Sensitive transport error")
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals(UiMessage.ERROR_REGISTER, vm.uiState.value.errorMessage)
    }

    @Test
    fun autoConfirmedRegistrationLoadsTheUser() = runTest(dispatcher) {
        repository.autoConfirm = true
        val vm = AccountViewModel(repository)
        vm.register("student@example.com", "password123").join()
        assertEquals("id", vm.uiState.value.data.user?.userId)
    }

    @Test
    fun signOutClearsTheAccount() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        vm.logOut().join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun sessionInvalidationClearsTheAccount() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "password123").join()
        repository.signedOutEvents.emit(Unit)
        advanceUntilIdle()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun googleSignInStartsTheRedirectFlow() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        assertTrue(repository.googleStarted)
    }

    @Test
    fun googleSignInDoesNotSignInBeforeTheRedirectReturns() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun theGoogleRedirectSignedInEventLoadsTheUser() = runTest(dispatcher) {
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        repository.current = User("google-id", "student@gmail.com", "Student")
        repository.signedInEvents.emit(Unit)
        advanceUntilIdle()
        assertEquals("google-id", vm.uiState.value.data.user?.userId)
    }

    @Test
    fun aFailedGoogleLaunchShowsASafeError() = runTest(dispatcher) {
        repository.fail = true
        val vm = AccountViewModel(repository)
        vm.signInWithGoogle().join()
        assertEquals(UiMessage.ERROR_GOOGLE_SIGN_IN, vm.uiState.value.errorMessage)
    }

    @Test
    fun aFailedLoginDoesNotSignTheUserIn() = runTest(dispatcher) {
        repository.fail = true
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "wrong").join()
        assertNull(vm.uiState.value.data.user)
    }

    @Test
    fun aFailedLoginShowsASafeError() = runTest(dispatcher) {
        repository.fail = true
        val vm = AccountViewModel(repository)
        vm.logIn("student@example.com", "wrong").join()
        assertEquals(UiMessage.ERROR_SIGN_IN, vm.uiState.value.errorMessage)
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
        override suspend fun deleteUser() {
            current = null
        }
    }
}
