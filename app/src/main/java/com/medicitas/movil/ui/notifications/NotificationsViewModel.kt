package com.medicitas.movil.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicitas.movil.domain.model.Notification
import com.medicitas.movil.domain.model.NotificationPage
import com.medicitas.movil.domain.repository.NotificationRepository
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Avisos internos del paciente (CU-25): reservas, confirmaciones y cancelaciones. */
class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NotificationPage>>(UiState.Loading)
    val state: StateFlow<UiState<NotificationPage>> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = repository.list().toUiState() }
    }

    /** Marca el aviso como leído (sin bloquear la navegación) y actualiza el contador. */
    fun open(notification: Notification) {
        if (notification.isRead) return
        viewModelScope.launch {
            repository.markRead(notification.id)
                .onSuccess { updateLocal { page -> page.markRead(setOf(notification.id)) } }
                .onFailure { _error.value = it.message }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess { updateLocal { page -> page.markRead(page.items.map { it.id }.toSet()) } }
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }

    private fun updateLocal(transform: (NotificationPage) -> NotificationPage) {
        val current = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(transform(current.data))
    }
}

private fun NotificationPage.markRead(ids: Set<Int>): NotificationPage {
    val newlyRead = items.count { it.id in ids && !it.isRead }
    return copy(
        items = items.map { if (it.id in ids) it.copy(isRead = true) else it },
        unreadCount = (unreadCount - newlyRead).coerceAtLeast(0),
    )
}
