package com.medicitas.movil.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentScope
import com.medicitas.movil.domain.repository.AppointmentRepository
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Panel del paciente: pestañas "Próximas" e "Historial" (RN-34). La pantalla llama a [load]
 * cada vez que se muestra, para reflejar lo hecho en el detalle o en el asistente de reserva.
 */
class AppointmentsViewModel(private val repository: AppointmentRepository) : ViewModel() {

    private val _scope = MutableStateFlow(AppointmentScope.UPCOMING)
    val scope: StateFlow<AppointmentScope> = _scope.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<Appointment>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Appointment>>> = _state.asStateFlow()

    fun selectScope(scope: AppointmentScope) {
        if (scope == _scope.value) return
        _scope.value = scope
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = repository.list(_scope.value).toUiState() }
    }
}
