package com.example.macronomocon.domain.model

sealed interface AuthenticationResult {
    data class SignedIn(val user: User) : AuthenticationResult
    data object AwaitingOtp : AuthenticationResult
}
