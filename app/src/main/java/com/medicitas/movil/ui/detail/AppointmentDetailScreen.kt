package com.medicitas.movil.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.components.DangerButton
import com.medicitas.movil.ui.components.ErrorBox
import com.medicitas.movil.ui.components.InlineMessage
import com.medicitas.movil.ui.components.LoadingBox
import com.medicitas.movil.ui.components.PrimaryButton
import com.medicitas.movil.ui.components.StatusBadge
import com.medicitas.movil.ui.theme.MediColors
import com.medicitas.movil.util.DateFormats

@Composable
fun AppointmentDetailScreen(viewModel: AppointmentDetailViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    var showCancel by rememberSaveable { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }

    when (val s = state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
        is UiState.Success -> DetailContent(
            appointment = s.data,
            action = action,
            onConfirm = viewModel::confirm,
            onCancel = { viewModel.clearMessages(); showCancel = true },
        )
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Cancelar cita") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cuéntenos por qué cancela. El médico recibirá un aviso.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(255) },
                        label = { Text("Motivo de la cancelación") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    action.error?.let { InlineMessage(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (viewModel.cancel(reason)) { showCancel = false; reason = "" } }) {
                    Text("Cancelar la cita", color = MediColors.DangerDark)
                }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Volver") } },
        )
    }
}

@Composable
private fun DetailContent(
    appointment: Appointment,
    action: DetailActionState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MediColors.Surface),
            border = BorderStroke(1.dp, MediColors.Border),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cita #${appointment.id}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    StatusBadge(appointment.status)
                }
                Field("Fecha", DateFormats.longDate(appointment.startsAt))
                Field("Hora", DateFormats.timeRange(appointment.startsAt, appointment.endsAt))
                Field("Médico", "Dr(a). ${appointment.doctorName}")
                Field("Especialidad", appointment.specialty)
                Field("Motivo de la consulta", appointment.reason ?: "Sin motivo registrado")
                if (appointment.status == AppointmentStatus.CANCELLED) {
                    Field("Motivo de la cancelación", appointment.cancellationReason ?: "—")
                }
            }
        }

        action.message?.let { InlineMessage(it, isError = false) }
        if (action.error != null && !action.working) InlineMessage(action.error)

        if (appointment.canConfirm) {
            PrimaryButton("Confirmar asistencia", onClick = onConfirm, loading = action.working)
        }
        if (appointment.canCancel) {
            DangerButton("Cancelar cita", onClick = onCancel, enabled = !action.working)
        }
        if (!appointment.canCancel && appointment.status in setOf(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED)) {
            // RN-24: el paciente no puede cancelar con 24 h o menos de anticipación.
            Text(
                "Esta cita ya no se puede cancelar desde la app: faltan 24 horas o menos, o ya pasó. " +
                    "Si no puede asistir, comuníquese con el centro médico.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediColors.TextMuted,
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MediColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
