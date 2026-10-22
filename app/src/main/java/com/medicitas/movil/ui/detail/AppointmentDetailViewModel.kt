package com.medicitas.movil.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.repository.AppointmentRepository
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailActionState(
    val working: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/** Detalle de una cita con las acciones del paciente: confirmar (CU-11) y cancelar (CU-12). */
class AppointmentDetailViewModel(
    private val appointmentId: Int,
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Appointment>>(UiState.Loading)
    val state: StateFlow<UiState<Appointment>> = _state.asStateFlow()

    private val _action = MutableStateFlow(DetailActionState())
    val action: StateFlow<DetailActionState> = _action.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = repository.detail(appointmentId).toUiState() }
    }

    fun confirm() = runAction("Su asistencia quedó confirmada.") { repository.confirm(appointmentId) }

    /** RN-24.1: el motivo es obligatorio (3 a 255 caracteres). El servidor vuelve a validarlo. */
    fun cancel(reason: String): Boolean {
        if (reason.trim().length < MIN_REASON) {
            _action.update { it.copy(error = "Indique el motivo de la cancelación (mínimo $MIN_REASON caracteres).") }
            return false
        }
        runAction("La cita fue cancelada.") { repository.cancel(appointmentId, reason) }
        return true
    }

    fun clearMessages() = _action.update { it.copy(message = null, error = null) }

    private fun runAction(success: String, call: suspend () -> Result<Appointment>) {
        _action.value = DetailActionState(working = true)
        viewModelScope.launch {
            call()
                .onSuccess {
                    _state.value = UiState.Success(it)
                    _action.value = DetailActionState(message = success)
                }
                .onFailure { e -> _action.value = DetailActionState(error = e.message) }
        }
    }

    companion object {
        const val MIN_REASON = 3
    }
}
