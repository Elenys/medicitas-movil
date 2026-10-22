package com.medicitas.movil.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicitas.movil.domain.model.ApiException
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AvailabilityDay
import com.medicitas.movil.domain.model.Doctor
import com.medicitas.movil.domain.model.Slot
import com.medicitas.movil.domain.model.Specialty
import com.medicitas.movil.domain.repository.AppointmentRepository
import com.medicitas.movil.domain.repository.CatalogRepository
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.toUiState
import com.medicitas.movil.util.DateFormats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Pasos del asistente de reserva (CU-06 → CU-10), igual que en la web. */
enum class BookingStep(val title: String) {
    SPECIALTY("1. Especialidad"),
    DOCTOR("2. Médico"),
    DAY("3. Día"),
    SLOT("4. Hora"),
    CONFIRM("5. Confirmar"),
    DONE("Cita reservada"),
}

data class BookingUiState(
    val step: BookingStep = BookingStep.SPECIALTY,
    val specialties: UiState<List<Specialty>> = UiState.Loading,
    val specialty: Specialty? = null,
    val doctors: UiState<List<Doctor>> = UiState.Loading,
    val doctor: Doctor? = null,
    val month: YearMonth = YearMonth.now(DateFormats.ZONE),
    val days: UiState<List<AvailabilityDay>> = UiState.Loading,
    val date: LocalDate? = null,
    val slots: UiState<List<Slot>> = UiState.Loading,
    val slot: Slot? = null,
    val reason: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val booked: Appointment? = null,
    val canGoPrevMonth: Boolean = false,
    val canGoNextMonth: Boolean = true,
)

class BookingViewModel(
    private val catalog: CatalogRepository,
    private val appointments: AppointmentRepository,
    private val today: () -> LocalDate = DateFormats::today,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState(month = YearMonth.from(today())))
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    /** RN-15: no se reserva a más de 60 días; el calendario no pasa de ese mes. */
    private val lastMonth: YearMonth get() = YearMonth.from(today().plusDays(MAX_ADVANCE_DAYS))
    private val firstMonth: YearMonth get() = YearMonth.from(today())

    init { loadSpecialties() }

    fun loadSpecialties() {
        _state.update { it.copy(specialties = UiState.Loading) }
        viewModelScope.launch {
            val result = catalog.specialties().toUiState()
            _state.update { it.copy(specialties = result) }
        }
    }

    fun selectSpecialty(specialty: Specialty) {
        _state.update { it.copy(specialty = specialty, step = BookingStep.DOCTOR, doctors = UiState.Loading, error = null) }
        viewModelScope.launch {
            val result = catalog.doctors(specialty.id).toUiState()
            _state.update { it.copy(doctors = result) }
        }
    }

    fun selectDoctor(doctor: Doctor) {
        val start = doctor.nextAvailableDate?.let(YearMonth::from)?.coerceIn(firstMonth, lastMonth) ?: firstMonth
        _state.update { it.copy(doctor = doctor, step = BookingStep.DAY, error = null) }
        loadMonth(start)
    }

    fun nextMonth() = _state.value.month.plusMonths(1).takeIf { it <= lastMonth }?.let(::loadMonth)
    fun prevMonth() = _state.value.month.minusMonths(1).takeIf { it >= firstMonth }?.let(::loadMonth)

    fun reloadMonth() = loadMonth(_state.value.month)

    private fun loadMonth(month: YearMonth) {
        val doctor = _state.value.doctor ?: return
        _state.update {
            it.copy(
                month = month,
                days = UiState.Loading,
                canGoPrevMonth = month > firstMonth,
                canGoNextMonth = month < lastMonth,
            )
        }
        viewModelScope.launch {
            val result = catalog.availability(doctor.id, month).toUiState()
            _state.update { it.copy(days = result) }
        }
    }

    fun selectDate(day: AvailabilityDay) {
        if (!day.isSelectable) return
        _state.update { it.copy(date = day.date, step = BookingStep.SLOT, slot = null, error = null) }
        loadSlots()
    }

    fun loadSlots() {
        val doctor = _state.value.doctor ?: return
        val date = _state.value.date ?: return
        _state.update { it.copy(slots = UiState.Loading) }
        viewModelScope.launch {
            val result = catalog.slots(doctor.id, date).toUiState()
            _state.update { it.copy(slots = result) }
        }
    }

    fun selectSlot(slot: Slot) {
        if (!slot.available) return
        _state.update { it.copy(slot = slot, step = BookingStep.CONFIRM, error = null) }
    }

    fun onReasonChange(value: String) = _state.update { it.copy(reason = value.take(MAX_REASON)) }

    fun submit() {
        val s = _state.value
        val doctor = s.doctor ?: return
        val slot = s.slot ?: return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            appointments.book(doctor.id, slot.startsAt, s.reason)
                .onSuccess { appt -> _state.update { it.copy(submitting = false, booked = appt, step = BookingStep.DONE) } }
                .onFailure { e ->
                    val code = (e as? ApiException)?.code
                    if (code == "SLOT_TAKEN" || code == "PATIENT_APPOINTMENT_TOO_CLOSE") {
                        // El cupo se ocupó mientras el paciente decidía (RN-22): volver a las horas.
                        _state.update { it.copy(submitting = false, error = e.message, step = BookingStep.SLOT, slot = null) }
                        loadSlots()
                    } else {
                        _state.update { it.copy(submitting = false, error = e.message) }
                    }
                }
        }
    }

    /** Retrocede un paso. Devuelve false si ya está en el primero (la pantalla debe cerrarse). */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            BookingStep.SPECIALTY, BookingStep.DONE -> return false
            BookingStep.DOCTOR -> BookingStep.SPECIALTY
            BookingStep.DAY -> BookingStep.DOCTOR
            BookingStep.SLOT -> BookingStep.DAY
            BookingStep.CONFIRM -> BookingStep.SLOT
        }
        _state.update { it.copy(step = previous, error = null) }
        return true
    }

    fun restart() {
        _state.value = BookingUiState(month = YearMonth.from(today()))
        loadSpecialties()
    }

    companion object {
        const val MAX_ADVANCE_DAYS = 60L
        const val MAX_REASON = 255
    }
}

private fun YearMonth.coerceIn(min: YearMonth, max: YearMonth): YearMonth =
    if (this < min) min else if (this > max) max else this
