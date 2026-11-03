package com.olius.app.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olius.app.data.remote.AppContainer
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAException
import com.olius.app.domain.entity.OlivIAReplyChunk
import com.olius.app.domain.repository.OlivIARepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val INTERRUPTED_REPLY_TEXT = "Resposta interrompida."

/** Texto que aparece na conversa, no lugar da resposta, quando a OlivIA falha. */
fun OlivIAException.toChatText(): String = when (this) {
    is OlivIAException.NotAvailable -> "O assistente ainda não está disponível no servidor."
    is OlivIAException.Unavailable -> "O assistente está indisponível, tente de novo."
    is OlivIAException.Timeout -> "A OlivIA demorou demais para responder. Tente de novo."
    is OlivIAException.Offline -> "Sem conexão com o servidor. Confira sua internet e tente de novo."
    is OlivIAException.TooManyRequests -> "Muitas mensagens seguidas. Espere um minuto e tente de novo."
    is OlivIAException.SessionExpired -> "Sua sessão expirou. Entre de novo para conversar com a OlivIA."
    is OlivIAException.Unknown -> "Não consegui responder agora. Tente de novo."
}

/** Chat na Core (que repassa para a IA). */
class ChatViewModel(
    private val repository: OlivIARepository = AppContainer.olivIARepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var replyJob: Job? = null
    private var nextMessageId = 0L

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onSend() {
        val state = _uiState.value
        if (!state.canSend) return
        send(state.input.trim(), state.pendingAttachment)
    }

    /** Sugestão tocada na tela de boas-vindas — envia direto, sem passar pelo campo. */
    fun onSuggestionClick(text: String) {
        if (_uiState.value.isGenerating) return
        send(text, null)
    }

    /** Botão de parar (quadrado) enquanto a OlivIA responde: cancela a chamada HTTP também. */
    fun onStopGenerating() {
        replyJob?.cancel()
    }

    fun onToggleAttachmentMenu() {
        _uiState.update { it.copy(isAttachmentMenuOpen = !it.isAttachmentMenuOpen) }
    }

    fun onDismissAttachmentMenu() {
        _uiState.update { it.copy(isAttachmentMenuOpen = false) }
    }

    /** Arquivo/foto escolhido no menu "+" — fica pendente até a pessoa enviar. */
    fun onAttachmentPicked(attachment: ChatAttachment) {
        _uiState.update { it.copy(pendingAttachment = attachment, isAttachmentMenuOpen = false) }
    }

    fun onRemovePendingAttachment() {
        _uiState.update { it.copy(pendingAttachment = null) }
    }

    private fun send(text: String, attachment: ChatAttachment?) {
        val userParts = buildList {
            attachment?.let { add(ChatMessagePart.File(it)) }
            if (text.isNotEmpty()) add(ChatMessagePart.Text(text))
        }
        val userMessage = ChatMessage(nextMessageId++, ChatAuthor.USER, userParts)
        val reply = ChatMessage(nextMessageId++, ChatAuthor.OLIVIA, emptyList())

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + reply,
                input = "",
                pendingAttachment = null,
                isGenerating = true,
                isAttachmentMenuOpen = false
            )
        }

        replyJob = viewModelScope.launch {
            try {
                repository.reply(text, attachment, _uiState.value.conversationId).collect { chunk ->
                    if (chunk is OlivIAReplyChunk.Conversation) {
                        _uiState.update { it.copy(conversationId = chunk.conversationId) }
                    } else {
                        updateMessage(reply.id) { it.append(chunk) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ensureActive()
                val error = e as? OlivIAException ?: OlivIAException.Unknown(e)
                updateMessage(reply.id) { it.append(OlivIAReplyChunk.Text(error.toChatText())) }
            } finally {
                finishReply(reply.id)
            }
        }
    }

    /** Fim da resposta (normal ou interrompida): tira o "Gerando..." que sobrou. */
    private fun finishReply(messageId: Long) {
        updateMessage(messageId) { message ->
            val parts = message.parts.filterNot { it == ChatMessagePart.GeneratingFile }
            message.copy(parts = parts.ifEmpty { listOf(ChatMessagePart.Text(INTERRUPTED_REPLY_TEXT)) })
        }
        _uiState.update { it.copy(isGenerating = false) }
    }

    private fun updateMessage(messageId: Long, transform: (ChatMessage) -> ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { if (it.id == messageId) transform(it) else it })
        }
    }
}
