package com.olius.app.presentation.screen.chat

import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAReplyChunk

enum class ChatAuthor { USER, OLIVIA }

/** Pedaços de uma mensagem, na ordem em que aparecem (texto, arquivo, texto...). */
sealed interface ChatMessagePart {
    data class Text(val text: String) : ChatMessagePart
    data class File(val attachment: ChatAttachment) : ChatMessagePart

    /** "Gerando relatório..." enquanto a OlivIA prepara um arquivo. */
    data object GeneratingFile : ChatMessagePart
}

data class ChatMessage(
    val id: Long,
    val author: ChatAuthor,
    val parts: List<ChatMessagePart>
) {
    /** Junta mais um pedaço da resposta da OlivIA nesta mensagem. */
    fun append(chunk: OlivIAReplyChunk): ChatMessage = when (chunk) {
        is OlivIAReplyChunk.Text -> {
            val last = parts.lastOrNull()
            if (last is ChatMessagePart.Text) {
                copy(parts = parts.dropLast(1) + last.copy(text = last.text + chunk.text))
            } else {
                copy(parts = parts + ChatMessagePart.Text(chunk.text.trimStart()))
            }
        }

        OlivIAReplyChunk.GeneratingAttachment -> copy(parts = parts + ChatMessagePart.GeneratingFile)

        is OlivIAReplyChunk.Attachment ->
            copy(parts = parts.filterNot { it == ChatMessagePart.GeneratingFile } + ChatMessagePart.File(chunk.attachment))

        is OlivIAReplyChunk.Conversation -> this
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val pendingAttachment: ChatAttachment? = null,
    val isGenerating: Boolean = false,
    val isAttachmentMenuOpen: Boolean = false,
    /** Id da conversa no servidor (vem na 1ª resposta e é reenviado nas próximas). */
    val conversationId: String? = null
) {
    val canSend: Boolean
        get() = !isGenerating && (input.isNotBlank() || pendingAttachment != null)
}
