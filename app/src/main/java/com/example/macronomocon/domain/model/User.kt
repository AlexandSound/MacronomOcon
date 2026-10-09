package com.example.macronomocon.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class User(
    val userId: String,
    var email: String,
    var passwordHash: String,
    var displayName: String,
    var twoFactorEnabled: Boolean,
    var failedLoginAttempts: Int,
    var lockedUntil: Instant
) {
    fun validatePassword(pw: String): Boolean = TODO("Validate the password against its hash")

    fun recordFailedLogin(): Unit = TODO("Record a failed login attempt")

    fun isLockedOut(): Boolean = TODO("Check whether the user is locked out")

    fun favorite(r: Recipe): Unit = TODO("Favorite the recipe")

    fun unfavorite(r: Recipe): Unit = TODO("Remove the recipe from favorites")

    fun block(r: Recipe): Unit = TODO("Block the recipe")

    fun isBlocked(r: Recipe): Boolean = TODO("Check whether the recipe is blocked")
}
