package com.olius.app.domain.entity

/** De onde veio o anexo: câmera, arquivos ou galeria (menu "+" do chat), ou gerado pela OlivIA. */
enum class AttachmentKind { CAMERA, FILE, PHOTO, REPORT }

data class ChatAttachment(
    val name: String,
    val kind: AttachmentKind,
    val uri: String? = null
)

/** Pedaço da resposta da OlivIA, na ordem em que chega. */
sealed interface OlivIAReplyChunk {
    /** Mais um trecho de texto, pra concatenar no que já chegou. */
    data class Text(val text: String) : OlivIAReplyChunk

    /** A OlivIA começou a gerar um arquivo (mostra "Gerando relatório..."). */
    data object GeneratingAttachment : OlivIAReplyChunk

    /** Arquivo pronto, anexado à resposta. */
    data class Attachment(val attachment: ChatAttachment) : OlivIAReplyChunk

    /** Id da conversa no servidor: o app reenvia nas próximas mensagens da mesma conversa. */
    data class Conversation(val conversationId: String) : OlivIAReplyChunk
}

/** Por que a OlivIA não respondeu. */
sealed class OlivIAException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** A rota do chat ainda não existe no servidor (404/405). */
    class NotAvailable : OlivIAException("Chat ainda não publicado no servidor")

    /** O servidor está de pé, mas a IA não respondeu (503). */
    class Unavailable : OlivIAException("IA indisponível")

    /** Passou do tempo de espera (60 s). */
    class Timeout(cause: Throwable? = null) : OlivIAException("Tempo de resposta esgotado", cause)

    /** Sem internet ou servidor fora do ar. */
    class Offline(cause: Throwable? = null) : OlivIAException("Sem conexão", cause)

    /** Muitas mensagens em pouco tempo (429). */
    class TooManyRequests : OlivIAException("Limite de mensagens")

    /** Sem sessão válida (401 mesmo depois do refresh). */
    class SessionExpired : OlivIAException("Sessão expirada")

    /** Qualquer outro erro (5xx, resposta fora do contrato). */
    class Unknown(cause: Throwable? = null) : OlivIAException("Erro no chat", cause)
}
