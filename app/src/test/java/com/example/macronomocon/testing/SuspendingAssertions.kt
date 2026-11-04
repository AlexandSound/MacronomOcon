package com.example.macronomocon.testing

/**
 * JUnit's `assertThrows` cannot call suspending code, which would otherwise force coroutine tests
 * to fall back on `@Test(expected = ...)`. This keeps every failure case asserted the same way.
 */
suspend inline fun <reified T : Throwable> assertThrows(block: suspend () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError(
            "Expected ${T::class.java.name} but was ${error::class.java.name}",
            error
        )
    }
    throw AssertionError("Expected ${T::class.java.name} but nothing was thrown")
}
