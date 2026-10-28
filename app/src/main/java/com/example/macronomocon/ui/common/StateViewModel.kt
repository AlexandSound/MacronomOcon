package com.example.macronomocon.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ViewState<T>(
    val data: T,
    val isLoading: Boolean = false,
    val errorMessage: UiMessage? = null
)

/** Serializes actions so an earlier request cannot overwrite a later action's result. */
abstract class StateViewModel<T>(initial: T) : ViewModel() {
    private val mutableState = MutableStateFlow(ViewState(initial))
    private val actions = Mutex()
    val uiState = mutableState.asStateFlow()

    protected val data: T get() = mutableState.value.data

    protected fun updateData(transform: (T) -> T) {
        mutableState.update { it.copy(data = transform(it.data)) }
    }

    fun clearError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    protected fun action(errorMessage: UiMessage, block: suspend () -> Unit): Job =
        viewModelScope.launch {
            actions.withLock {
                mutableState.update { it.copy(isLoading = true, errorMessage = null) }
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.update { it.copy(errorMessage = errorMessage) }
                } finally {
                    mutableState.update { it.copy(isLoading = false) }
                }
            }
        }
}
