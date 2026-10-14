package com.olius.app.presentation.screen.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olius.app.R
import com.olius.app.domain.entity.AttachmentKind
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.presentation.components.BOTTOM_NAV_HEIGHT
import com.olius.app.presentation.components.OliusPageTitle
import com.olius.app.presentation.components.OliusTabScaffold
import com.olius.app.presentation.components.OliusTopBarState
import com.olius.app.presentation.theme.OliusAmarelo
import com.olius.app.presentation.theme.OliusAmareloBorda
import com.olius.app.presentation.theme.OliusAmareloFundoSuave
import com.olius.app.presentation.theme.OliusBalaoUsuario
import com.olius.app.presentation.theme.OliusBotaoEscuro
import com.olius.app.presentation.theme.OliusCampoBorda
import com.olius.app.presentation.theme.OliusTextoPrimario
import com.olius.app.presentation.theme.OliusTextoSecundario
import com.olius.app.presentation.theme.OliusTituloMarrom

// Perguntas de exemplo da tela de boas-vindas — as mesmas das referências,
// pra dar pra ver as respostas mockadas (relatório e impactos) com um toque.
private const val SUGGESTION_REPORT =
    "OlivIA, registrei 67 litros de óleo no mês passado, isso é bom? Faça um relatório sustentável."
private const val SUGGESTION_IMPACT = "Quais são os impactos ambientais de não registrar o meu óleo?"

/**
 * Tela da OlivIA. Sem mensagens mostra as boas-vindas (ref_chat.png); com
 * mensagens, a conversa (ref_chat_message.png). O "+" abre o menu de
 * Câmera/Arquivos/Fotos (ref_chat_plus.png), cada um abrindo o app/seletor
 * correspondente do celular.
 */
@Composable
fun ChatScreen(topBarState: OliusTopBarState, viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pickers = rememberAttachmentPickers(context, onPicked = viewModel::onAttachmentPicked)

    OliusTabScaffold(topBarState = topBarState) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty()) {
                WelcomeContent(onSuggestionClick = viewModel::onSuggestionClick)
            } else {
                MessagesList(messages = state.messages, isGenerating = state.isGenerating)
            }

            if (state.isAttachmentMenuOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = viewModel::onDismissAttachmentMenu
                        )
                )
            }
            // Fecha o menu ao escolher — se a pessoa cancelar o seletor, não fica aberto.
            AttachmentMenu(
                visible = state.isAttachmentMenuOpen,
                onCamera = { viewModel.onDismissAttachmentMenu(); pickers.openCamera() },
                onFiles = { viewModel.onDismissAttachmentMenu(); pickers.openFiles() },
                onPhotos = { viewModel.onDismissAttachmentMenu(); pickers.openPhotos() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 8.dp)
            )
        }

        state.pendingAttachment?.let { attachment ->
            PendingAttachmentChip(attachment = attachment, onRemove = viewModel::onRemovePendingAttachment)
            Spacer(Modifier.height(8.dp))
        }
        ChatInputBar(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            isGenerating = state.isGenerating,
            canSend = state.canSend,
            isMenuOpen = state.isAttachmentMenuOpen,
            onPlusClick = viewModel::onToggleAttachmentMenu,
            onSend = viewModel::onSend,
            onStop = viewModel::onStopGenerating,
            onVoice = { Toast.makeText(context, "Mensagem de voz em breve", Toast.LENGTH_SHORT).show() }
        )
        // O campo fica logo acima do menu inferior (que é sobreposto — ver MainScaffoldScreen).
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(BOTTOM_NAV_HEIGHT)
        )
    }
}

// ---------------------------------------------------------------------------
// Boas-vindas (ref_chat.png)
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeContent(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        OliusPageTitle(
            title = "OlivIA: Assistente virtual",
            subtitle = "Assistente com sistema multiagente do Olius."
        )
        Spacer(Modifier.height(48.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            OlivIAIllustration()
            Spacer(Modifier.height(20.dp))
            Text("Seja bem-vindo ao OlivIA!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OliusTituloMarrom)
            Spacer(Modifier.height(4.dp))
            Text(
                "Agente de rotas e mapas, analista de negócios, assessor de óleo, assessor de " +
                    "sustentabilidade e agente de FAQ em um único assistente: OlivIA!",
                fontSize = 13.sp,
                color = OliusTextoSecundario,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(20.dp))
            SuggestionChip("Faça um relatório do meu óleo", onClick = { onSuggestionClick(SUGGESTION_REPORT) })
            Spacer(Modifier.height(8.dp))
            SuggestionChip("Impactos de não registrar o óleo", onClick = { onSuggestionClick(SUGGESTION_IMPACT) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Logo da Olius + estrela de 4 pontas (ref_chat.png). Usa a logo grande em
 * vez do ícone da navbar (`chat_olius_selected`), que é pequeno e fica
 * pixelado ampliado.
 */
@Composable
private fun OlivIAIllustration() {
    Box(Modifier.size(width = 190.dp, height = 180.dp)) {
        Image(
            painter = painterResource(R.drawable.logo_sem_fundo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(150.dp)
                .alpha(0.55f)
        )
        Canvas(
            Modifier
                .align(Alignment.TopEnd)
                .size(52.dp)
        ) {
            val c = size.width / 2
            val inner = size.width * 0.12f
            val star = Path().apply {
                moveTo(c, 0f)
                quadraticTo(c + inner, c - inner, size.width, c)
                quadraticTo(c + inner, c + inner, c, size.height)
                quadraticTo(c - inner, c + inner, 0f, c)
                quadraticTo(c - inner, c - inner, c, 0f)
                close()
            }
            drawPath(star, OliusAmarelo.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 13.sp,
        color = OliusTextoPrimario,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(OliusAmareloFundoSuave)
            .border(BorderStroke(1.dp, OliusAmareloBorda), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// Conversa (ref_chat_message.png)
// ---------------------------------------------------------------------------

@Composable
private fun MessagesList(messages: List<ChatMessage>, isGenerating: Boolean) {
    val listState = rememberLazyListState()
    // Acompanha a resposta enquanto ela chega: rola até o fim a cada mudança.
    LaunchedEffect(messages) {
        listState.scrollToItem(messages.lastIndex, scrollOffset = 10_000)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            when (message.author) {
                ChatAuthor.USER -> UserBubble(message)
                ChatAuthor.OLIVIA -> OlivIAMessage(message, isTyping = isGenerating && message.parts.isEmpty())
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(OliusBalaoUsuario)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            message.parts.forEach { MessagePart(it) }
        }
    }
}

@Composable
private fun OlivIAMessage(message: ChatMessage, isTyping: Boolean) {
    Column(
        modifier = Modifier
            .widthIn(max = 290.dp)
            .padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isTyping) PulsingText("Digitando...")
        message.parts.forEach { MessagePart(it) }
    }
}

@Composable
private fun MessagePart(part: ChatMessagePart) {
    val context = LocalContext.current
    when (part) {
        is ChatMessagePart.Text -> Text(part.text, fontSize = 14.sp, color = OliusTextoPrimario, lineHeight = 20.sp)
        ChatMessagePart.GeneratingFile -> PulsingText("Gerando relatório...")
        is ChatMessagePart.File -> if (part.attachment.kind == AttachmentKind.REPORT) {
            Text(
                part.attachment.name,
                fontSize = 14.sp,
                color = OliusTextoPrimario,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Download disponível quando a API da OlivIA estiver pronta", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            AttachmentLabel(part.attachment)
        }
    }
}

@Composable
private fun AttachmentLabel(attachment: ChatAttachment, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(attachment.kind.icon, contentDescription = null, tint = OliusTextoPrimario, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            attachment.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = OliusTextoPrimario,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val AttachmentKind.icon: ImageVector
    get() = when (this) {
        AttachmentKind.CAMERA -> Icons.Outlined.PhotoCamera
        AttachmentKind.PHOTO -> Icons.Outlined.Image
        AttachmentKind.FILE -> Icons.Outlined.AttachFile
        AttachmentKind.REPORT -> Icons.Outlined.Description
    }

@Composable
private fun PulsingText(text: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Text(text, fontSize = 14.sp, color = OliusTextoSecundario, modifier = Modifier.alpha(alpha))
}

// ---------------------------------------------------------------------------
// Campo de texto + menu "+" (ref_chat_plus.png)
// ---------------------------------------------------------------------------

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    canSend: Boolean,
    isMenuOpen: Boolean,
    onPlusClick: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit
) {
    val plusRotation by animateFloatAsState(if (isMenuOpen) 45f else 0f, label = "plusRotation")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, OliusCampoBorda), RoundedCornerShape(26.dp))
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlusClick, modifier = Modifier.size(40.dp)) {
            Image(
                painter = painterResource(R.drawable.plus),
                contentDescription = if (isMenuOpen) "Fechar anexos" else "Anexar",
                modifier = Modifier
                    .size(16.dp)
                    .rotate(plusRotation)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            if (value.isEmpty()) {
                Text("Pergunte ao OlivIA", fontSize = 15.sp, color = OliusTextoSecundario)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                maxLines = 4,
                textStyle = TextStyle(fontSize = 15.sp, color = OliusTextoPrimario, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth()
            )
        }
        val (icon, description, action) = when {
            isGenerating -> Triple(Icons.Filled.Stop, "Parar resposta", onStop)
            canSend -> Triple(Icons.Filled.ArrowUpward, "Enviar", onSend)
            else -> Triple(Icons.Filled.GraphicEq, "Mensagem de voz", onVoice)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OliusBotaoEscuro)
                .clickable(onClick = action),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AttachmentMenu(
    visible: Boolean,
    onCamera: () -> Unit,
    onFiles: () -> Unit,
    onPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        AttachmentMenuCard(onCamera = onCamera, onFiles = onFiles, onPhotos = onPhotos)
    }
}

@Composable
private fun AttachmentMenuCard(onCamera: () -> Unit, onFiles: () -> Unit, onPhotos: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.width(180.dp)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            AttachmentMenuItem("Câmera", Icons.Outlined.PhotoCamera, onCamera)
            AttachmentMenuItem("Arquivos", Icons.Outlined.AttachFile, onFiles)
            AttachmentMenuItem("Fotos", Icons.Outlined.Image, onPhotos)
        }
    }
}

@Composable
private fun AttachmentMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = OliusTextoPrimario, modifier = Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = OliusTextoPrimario, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun PendingAttachmentChip(attachment: ChatAttachment, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(OliusAmareloFundoSuave)
            .border(BorderStroke(1.dp, OliusAmareloBorda), RoundedCornerShape(16.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttachmentLabel(attachment, modifier = Modifier.widthIn(max = 240.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remover anexo", tint = OliusTextoSecundario, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Seletores do sistema (câmera, arquivos, galeria)
// ---------------------------------------------------------------------------

private class AttachmentPickers(
    val openCamera: () -> Unit,
    val openFiles: () -> Unit,
    val openPhotos: () -> Unit
)

/**
 * Nenhum dos 3 precisa de permissão: a câmera é aberta pelo app de câmera do
 * sistema (TakePicturePreview) e arquivos/fotos pelos seletores do Android.
 */
@Composable
private fun rememberAttachmentPickers(context: Context, onPicked: (ChatAttachment) -> Unit): AttachmentPickers {
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            onPicked(ChatAttachment("Foto_${System.currentTimeMillis()}.jpg", AttachmentKind.CAMERA))
        }
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(ChatAttachment(displayName(context, it) ?: "Arquivo", AttachmentKind.FILE, it.toString())) }
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(ChatAttachment(displayName(context, it) ?: "Foto", AttachmentKind.PHOTO, it.toString())) }
    }
    return AttachmentPickers(
        openCamera = {
            try {
                camera.launch(null)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Nenhum app de câmera encontrado", Toast.LENGTH_SHORT).show()
            }
        },
        openFiles = { files.launch(arrayOf("*/*")) },
        openPhotos = { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
