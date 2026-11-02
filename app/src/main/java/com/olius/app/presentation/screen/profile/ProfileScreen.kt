package com.olius.app.presentation.screen.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.domain.entity.ThemeMode
import com.olius.app.presentation.components.BOTTOM_NAV_CONTENT_PADDING
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusFundoTela
import com.olius.app.presentation.theme.OliusLaranja
import com.olius.app.presentation.theme.OliusSuperficie
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoPrimarioClaro
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusVerdeAberto

// Altura do cabeçalho amarelo e tamanho do avatar (Mobile V2/Perfil/Perfil.png).
private val HEADER_HEIGHT = 230.dp
private val AVATAR_SIZE = 96.dp

/**
 * Tela de Perfil (Figma "Mobile V2/Perfil"), dentro do MainScaffoldScreen: o
 * menu inferior continua na tela. Sem "Ajuda & Suporte" e sem trocar foto
 * (o banco não tem coluna de foto: o avatar mostra as iniciais).
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Roda de novo sempre que a tela volta (ex.: depois do "Editar Perfil").
    LaunchedEffect(Unit) { viewModel.reloadFromCache() }

    Box(
        Modifier
            .fillMaxSize()
            .background(OliusFundoTela)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = BOTTOM_NAV_CONTENT_PADDING)
        ) {
            ProfileHeader(initials = state.initials, onBack = onBack)
            Spacer(Modifier.height(10.dp))
            Text(
                state.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OliusTextoPrimario,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Text(
                listOf(state.email, state.phone).filter { it.isNotBlank() }.joinToString(" | "),
                fontSize = 13.sp,
                color = OliusTextoPrimario,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            AccountBadges(typeLabel = state.profileTypeLabel, isCollector = state.isCollector)
            Spacer(Modifier.height(20.dp))

            ProfileCard {
                ProfileRow(Icons.Outlined.ContactPage, "Editar informações de perfil", onClick = onEditProfile)
                ProfileRow(Icons.Outlined.DarkMode, "Tema", value = state.themeMode.label, onClick = viewModel::onThemeClick)
                // Só existe português: o item aparece, mas não abre nada.
                ProfileRow(Icons.Outlined.Translate, "Idioma", value = state.language)
                ProfileRow(
                    Icons.Outlined.Notifications,
                    "Notificações",
                    onClick = { viewModel.onNotificationsToggle(!state.notificationsEnabled) }
                ) {
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::onNotificationsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OliusAmarelo,
                            checkedBorderColor = OliusAmarelo
                        )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            ProfileCard {
                ProfileRow(Icons.Outlined.MailOutline, "Fale conosco") { openSupportEmail(context, state.supportEmail) }
                ProfileRow(Icons.Outlined.Lock, "Política de Privacidade", onClick = onPrivacyPolicy)
            }
            Spacer(Modifier.height(24.dp))
            LogoutButton(
                onClick = {
                    viewModel.onLogoutClick()
                    onLogout()
                }
            )
        }
    }

    if (state.isThemeDialogOpen) {
        ThemeDialog(
            selected = state.themeMode,
            onSelect = viewModel::onThemeSelected,
            onDismiss = viewModel::onDismissThemeDialog
        )
    }
}

/** Fundo amarelo com a borda de baixo curva, logo branca e o avatar por cima da curva. */
@Composable
private fun ProfileHeader(initials: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT + AVATAR_SIZE / 2)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT)
                .clip(CurvedBottomShape)
                .background(OliusAmarelo)
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Voltar", tint = OliusTextoPrimarioClaro)
        }
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_sem_fundo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Olius", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        // Sem foto no banco: iniciais no lugar da foto e sem o lápis de "trocar foto".
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(OliusAmarelo)
                .border(BorderStroke(3.dp, Color.White), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = OliusTextoPrimarioClaro, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Retângulo com a base em arco (a curva do cabeçalho na referência). */
private val CurvedBottomShape = GenericShape { size, _ ->
    val curve = size.height * 0.16f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - curve)
    quadraticTo(size.width / 2f, size.height + curve, 0f, size.height - curve)
    close()
}

/** "Estabelecimento"/"Cidadão" e "Coletor: Sim/Não" (não editáveis). */
@Composable
private fun AccountBadges(typeLabel: String, isCollector: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoBadge("Tipo de usuário", typeLabel.ifEmpty { "—" }, Modifier.weight(1f))
        InfoBadge(
            "Coletor",
            if (isCollector) "Sim" else "Não",
            Modifier.weight(1f),
            valueColor = if (isCollector) OliusVerdeAberto else OliusTextoPrimario
        )
    }
}

@Composable
private fun InfoBadge(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = OliusTextoPrimario) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OliusSuperficie)
            .border(1.dp, OliusCampoBorda, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(title, fontSize = 12.sp, color = OliusTextoSecundario)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OliusSuperficie,
        border = BorderStroke(1.dp, OliusCampoBorda),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) { content() }
    }
}

/** Linha do cartão: ícone, título e, à direita, o valor em laranja ou um controle ([trailing]). */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = if (trailing != null) 2.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OliusTextoPrimario, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, color = OliusTextoPrimario, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 14.sp, color = OliusLaranja)
        trailing?.invoke()
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, OliusCampoBorda, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = OliusTextoPrimario, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sair do aplicativo", color = OliusTextoPrimario, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Escolha do tema: claro (padrão), escuro ou o do aparelho. */
@Composable
private fun ThemeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OliusSuperficie,
        title = { Text("Tema", color = OliusTextoPrimario, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode == selected, role = Role.RadioButton) { onSelect(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = OliusAmarelo)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(mode.label, color = OliusTextoPrimario)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", color = OliusLaranja) }
        }
    )
}

/** "Fale conosco": abre o app de e-mail já com o endereço do Olius. */
private fun openSupportEmail(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        .putExtra(Intent.EXTRA_SUBJECT, "Contato pelo app Olius")
    startOrWarn(context, intent, "Nenhum app de e-mail encontrado. Escreva para $email")
}

private fun startOrWarn(context: Context, intent: Intent, warning: String) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, warning, Toast.LENGTH_LONG).show()
    }
}
