package com.olius.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.olius.app.R
import com.olius.app.presentation.theme.OliusAmarelo

/**
 * Estado + ações do header compartilhado por todas as abas (logo, sino de
 * notificações e avatar). Quem cria é o `MainScaffoldScreen`, a partir do
 * `HomeViewModel` — assim o sino/avatar abrem os mesmos bottom sheets em
 * qualquer aba, e a bolinha de "não lida" fica sincronizada.
 */
data class OliusTopBarState(
    val hasUnreadNotifications: Boolean = false,
    val onNotificationsClick: () -> Unit = {},
    val onProfileClick: () -> Unit = {}
)

/** Header das abas pós-login: logo à esquerda, notificações + avatar à direita. */
@Composable
fun OliusTopBar(state: OliusTopBarState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.logo_sem_fundo),
            contentDescription = "Olius",
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = state.onNotificationsClick) {
            Image(
                painter = painterResource(
                    if (state.hasUnreadNotifications) R.drawable.notification_true else R.drawable.notification_false
                ),
                contentDescription = "Notificações",
                modifier = Modifier.size(22.dp)
            )
        }
        ProfileAvatar(onClick = state.onProfileClick)
    }
}

/**
 * TODO: trocar pelo avatar real do usuário (ex.: Coil + `profileImageUrl`)
 * quando existir integração com o backend. Por enquanto usa um placeholder
 * com ícone, igual referência (círculo amarelo).
 */
@Composable
private fun ProfileAvatar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(OliusAmarelo)
            .border(BorderStroke(2.dp, OliusAmarelo), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}
