package com.medicitas.movil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medicitas.movil.domain.model.Appointment
import com.medicitas.movil.domain.model.AppointmentStatus
import com.medicitas.movil.ui.theme.MediColors
import com.medicitas.movil.util.DateFormats

/**
 * Etiqueta de estado de cita. Único lugar con el mapeo estado → color (06 §1.2); el estado
 * siempre se muestra también como texto, nunca solo por color.
 */
@Composable
fun StatusBadge(status: AppointmentStatus, modifier: Modifier = Modifier) {
    val (text, bg, border) = when (status) {
        AppointmentStatus.BOOKED -> Triple(Color(0xFF1565C0), Color(0xFFE3F2FD), Color(0xFF1E88E5))
        AppointmentStatus.CONFIRMED -> Triple(Color(0xFF1B5E20), Color(0xFFE8F5E9), Color(0xFF2E7D32))
        AppointmentStatus.COMPLETED -> Triple(Color(0xFF424242), Color(0xFFF5F5F5), Color(0xFF9E9E9E))
        AppointmentStatus.CANCELLED -> Triple(Color(0xFFB71C1C), Color(0xFFFFEBEE), Color(0xFFE53935))
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Estado: ${status.label}" },
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            status.label,
            color = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MediColors.Surface),
        border = BorderStroke(1.dp, MediColors.Border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    DateFormats.longDate(appointment.startsAt),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(appointment.status)
            }
            Text(
                DateFormats.timeRange(appointment.startsAt, appointment.endsAt),
                style = MaterialTheme.typography.bodyLarge,
                color = MediColors.PrimaryDark,
            )
            Text("Dr(a). ${appointment.doctorName} · ${appointment.specialty}", style = MaterialTheme.typography.bodyMedium)
            appointment.reason?.let {
                Text("Motivo: $it", style = MaterialTheme.typography.bodyMedium, color = MediColors.TextMuted)
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Mensaje de error de la API (el `message` en español que manda el servidor) con reintento. */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MediColors.Danger, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
        }
    }
}

/** Aviso en línea (por ejemplo, el error de un formulario). */
@Composable
fun InlineMessage(message: String, isError: Boolean = true, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isError) MediColors.DangerLight else MediColors.SuccessLight,
        border = BorderStroke(1.dp, if (isError) MediColors.Danger else MediColors.Success),
    ) {
        Text(
            message,
            color = if (isError) Color(0xFFB71C1C) else Color(0xFF1B5E20),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Event, contentDescription = null, tint = MediColors.Primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MediColors.TextMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MediColors.PrimaryDark),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MediColors.DangerDark),
    ) { Text(text) }
}
