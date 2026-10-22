package com.medicitas.movil.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicitas.movil.domain.model.Notification
import com.medicitas.movil.ui.UiState
import com.medicitas.movil.ui.components.EmptyState
import com.medicitas.movil.ui.components.ErrorBox
import com.medicitas.movil.ui.components.InlineMessage
import com.medicitas.movil.ui.components.LoadingBox
import com.medicitas.movil.ui.theme.MediColors
import com.medicitas.movil.util.DateFormats

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel, onOpenAppointment: (Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    when (val s = state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message, onRetry = viewModel::load)
        is UiState.Success -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (s.data.unreadCount == 0) "Todo al día" else "${s.data.unreadCount} sin leer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (s.data.unreadCount > 0) {
                    TextButton(onClick = viewModel::markAllRead) { Text("Marcar todas como leídas") }
                }
            }
            error?.let { InlineMessage(it, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (s.data.items.isEmpty()) {
                EmptyState("Sin avisos", "Aquí verá los avisos de sus reservas, confirmaciones y cancelaciones.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.data.items, key = { it.id }) { n ->
                        NotificationItem(n) {
                            viewModel.open(n)
                            n.appointmentId?.let(onOpenAppointment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MediColors.Surface else MediColors.PrimaryLight,
        ),
        border = BorderStroke(1.dp, if (notification.isRead) MediColors.Border else MediColors.Primary),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = (if (notification.isRead) "Leído. " else "Sin leer. ") + notification.message
        },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (notification.isRead) MediColors.Border else MediColors.Primary,
                    modifier = Modifier.size(10.dp),
                ) {}
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                )
                Text(
                    DateFormats.dateTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediColors.TextMuted,
                )
            }
        }
    }
}
