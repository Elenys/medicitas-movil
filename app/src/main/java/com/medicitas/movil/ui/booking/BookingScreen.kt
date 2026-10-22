package com.medicitas.movil.ui.booking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicitas.movil.domain.model.AvailabilityDay
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.components.EmptyState
import com.medicitas.movil.ui.components.ErrorBox
import com.medicitas.movil.ui.components.InlineMessage
import com.medicitas.movil.ui.components.LoadingBox
import com.medicitas.movil.ui.components.PrimaryButton
import com.medicitas.movil.ui.theme.MediColors
import com.medicitas.movil.util.DateFormats
import java.time.DayOfWeek

@Composable
fun BookingScreen(viewModel: BookingViewModel, onOpenAppointment: (Int) -> Unit, onFinished: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != BookingStep.SPECIALTY && state.step != BookingStep.DONE) { viewModel.back() }

    Column(Modifier.fillMaxSize()) {
        if (state.step != BookingStep.DONE) {
            StepHeader(state, onBack = { viewModel.back() })
        }
        state.error?.let { InlineMessage(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        Box(Modifier.weight(1f)) {
            when (state.step) {
                BookingStep.SPECIALTY -> Listing(state.specialties, viewModel::loadSpecialties, "No hay especialidades disponibles.") { list ->
                    list.forEach { sp ->
                        OptionCard(sp.name, sp.description, onClick = { viewModel.selectSpecialty(sp) })
                    }
                }
                BookingStep.DOCTOR -> Listing(state.doctors, { state.specialty?.let(viewModel::selectSpecialty) }, "No hay médicos con agenda en esta especialidad.") { list ->
                    list.forEach { d ->
                        val next = d.nextAvailableDate?.let { "Próximo cupo: ${DateFormats.shortDate(it)}" } ?: "Sin cupos en los próximos 60 días"
                        OptionCard("Dr(a). ${d.fullName}", "${d.specialtyName} · citas de ${d.durationMinutes} min\n$next", onClick = { viewModel.selectDoctor(d) })
                    }
                }
                BookingStep.DAY -> DayStep(state, viewModel)
                BookingStep.SLOT -> SlotStep(state, viewModel)
                BookingStep.CONFIRM -> ConfirmStep(state, viewModel)
                BookingStep.DONE -> DoneStep(
                    state,
                    onOpen = { state.booked?.let { onOpenAppointment(it.id) } },
                    onAnother = viewModel::restart,
                    onFinished = onFinished,
                )
            }
        }
    }
}

@Composable
private fun StepHeader(state: BookingUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MediColors.Surface).padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.step != BookingStep.SPECIALTY) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Paso anterior") }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Text("Agendar cita — ${state.step.title}", style = MaterialTheme.typography.titleMedium)
        }
        val summary = listOfNotNull(
            state.specialty?.name,
            state.doctor?.let { "Dr(a). ${it.fullName}" }.takeIf { state.step.ordinal > BookingStep.DOCTOR.ordinal },
            state.date?.let { DateFormats.shortDate(it) }.takeIf { state.step.ordinal > BookingStep.DAY.ordinal },
        ).takeIf { state.step != BookingStep.SPECIALTY }
        if (!summary.isNullOrEmpty()) {
            Text(
                summary.joinToString(" › "),
                style = MaterialTheme.typography.bodyMedium,
                color = MediColors.TextMuted,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun <T> Listing(
    state: UiState<List<T>>,
    onRetry: () -> Unit,
    emptyText: String,
    content: @Composable (List<T>) -> Unit,
) {
    when (state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.message, onRetry)
        is UiState.Success ->
            if (state.data.isEmpty()) {
                EmptyState("Sin resultados", emptyText)
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content(state.data) }
            }
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediColors.Surface),
        border = BorderStroke(1.dp, MediColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MediColors.TextMuted) }
        }
    }
}

@Composable
private fun DayStep(state: BookingUiState, viewModel: BookingViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.prevMonth() }, enabled = state.canGoPrevMonth) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Mes anterior")
            }
            Text(
                DateFormats.month(state.month),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.nextMonth() }, enabled = state.canGoNextMonth) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Mes siguiente")
            }
        }
        Spacer(Modifier.height(8.dp))
        when (val days = state.days) {
            UiState.Loading -> Box(Modifier.fillMaxWidth().height(240.dp)) { LoadingBox() }
            is UiState.Error -> ErrorBox(days.message, onRetry = viewModel::reloadMonth)
            is UiState.Success -> {
                MonthGrid(state, days.data, onSelect = viewModel::selectDate)
                Spacer(Modifier.height(12.dp))
                Text(
                    "En verde, los días con cupos libres (el número indica cuántos). Los días en gris no tienen cupos " +
                        "o están fuera de la ventana de reserva (desde 2 horas hasta 60 días).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(state: BookingUiState, days: List<AvailabilityDay>, onSelect: (AvailabilityDay) -> Unit) {
    val byDate = days.associateBy { it.date }
    val first = state.month.atDay(1)
    val offset = first.dayOfWeek.value - DayOfWeek.MONDAY.value // la semana empieza el lunes
    val cells = List(offset) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    Row(Modifier.fillMaxWidth()) {
        listOf("L", "M", "M", "J", "V", "S", "D").forEach {
            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MediColors.TextMuted)
        }
    }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { i ->
                val date = week.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        val day = byDate[date]
                        val selectable = day?.isSelectable == true
                        val selected = date == state.date
                        Column(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        selected -> MediColors.PrimaryDark
                                        selectable -> MediColors.SuccessLight
                                        else -> Color.Transparent
                                    },
                                )
                                .border(1.dp, if (selectable) MediColors.Success else MediColors.Border, RoundedCornerShape(8.dp))
                                .clickable(enabled = selectable) { day?.let(onSelect) }
                                .semantics {
                                    contentDescription = DateFormats.longDate(date) +
                                        if (selectable) ", ${day?.availableSlots} cupos libres" else ", sin cupos"
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontWeight = if (selectable) FontWeight.SemiBold else FontWeight.Normal,
                                color = when {
                                    selected -> Color.White
                                    selectable -> Color(0xFF1B5E20)
                                    else -> MediColors.TextDisabled
                                },
                            )
                            if (selectable) {
                                Text(
                                    "${day?.availableSlots}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) Color.White else MediColors.Success,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotStep(state: BookingUiState, viewModel: BookingViewModel) {
    when (val slots = state.slots) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(slots.message, onRetry = viewModel::loadSlots)
        is UiState.Success -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(state.date?.let(DateFormats::longDate).orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(
                "Elija una hora. Las tachadas ya están ocupadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            if (slots.data.none { it.available }) {
                Text("Ya no quedan horas libres este día. Vuelva y elija otro.", style = MaterialTheme.typography.bodyLarge)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.data.forEach { slot ->
                    OutlinedButton(
                        onClick = { viewModel.selectSlot(slot) },
                        enabled = slot.available,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (slot.available) MediColors.Primary else MediColors.Border),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            DateFormats.time(slot.startsAt),
                            textDecoration = if (slot.available) null else TextDecoration.LineThrough,
                            color = if (slot.available) MediColors.PrimaryDark else MediColors.TextDisabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmStep(state: BookingUiState, viewModel: BookingViewModel) {
    val doctor = state.doctor ?: return
    val slot = state.slot ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MediColors.PrimaryLight),
            border = BorderStroke(1.dp, MediColors.Primary),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Resumen de la cita", style = MaterialTheme.typography.titleMedium)
                Text("Dr(a). ${doctor.fullName} · ${doctor.specialtyName}")
                Text(DateFormats.longDate(slot.startsAt))
                Text(DateFormats.timeRange(slot.startsAt, slot.endsAt), fontWeight = FontWeight.SemiBold)
            }
        }
        OutlinedTextField(
            value = state.reason,
            onValueChange = viewModel::onReasonChange,
            label = { Text("Motivo de la consulta (opcional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton("Reservar cita", onClick = viewModel::submit, loading = state.submitting)
        Text(
            "Podrá confirmar o cancelar la cita desde \"Mis citas\". La cancelación solo es posible hasta 24 horas antes.",
            style = MaterialTheme.typography.bodySmall,
            color = MediColors.TextMuted,
        )
    }
}

@Composable
private fun DoneStep(state: BookingUiState, onOpen: () -> Unit, onAnother: () -> Unit, onFinished: () -> Unit) {
    val booked = state.booked ?: return
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MediColors.Success, modifier = Modifier.height(56.dp).width(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("¡Cita reservada!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Dr(a). ${booked.doctorName}\n${DateFormats.longDate(booked.startsAt)}\n${DateFormats.timeRange(booked.startsAt, booked.endsAt)}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Ver la cita", onClick = onOpen)
        TextButton(onClick = onAnother) { Text("Agendar otra cita") }
        TextButton(onClick = onFinished) { Text("Ir a mis citas") }
    }
}
