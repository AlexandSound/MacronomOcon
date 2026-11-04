package com.example.macronomocon.domain.model

/** A rejection the user can act on; the repository maps transport errors to a [Reason]. */
class AccountException(val reason: Reason) : Exception(reason.name) {
    enum class Reason {
        EMAIL_IN_USE,
        INVALID_EMAIL,
        WEAK_PASSWORD,
        EMAIL_NOT_AUTHORIZED,
        EMAIL_RATE_LIMITED,
        SIGNUP_DISABLED
    }
}
