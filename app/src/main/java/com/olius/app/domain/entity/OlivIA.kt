package com.olius.app.domain.entity

/** De onde veio o anexo: câmera, arquivos ou galeria (menu "+" do chat), ou gerado pela OlivIA. */
enum class AttachmentKind { CAMERA, FILE, PHOTO, REPORT }

data class ChatAttachment(
    val name: String,
    val kind: AttachmentKind,
    val uri: String? = null
)

/**
 * Pedaço da resposta da OlivIA, na ordem em que chega. A resposta vem em
 * partes (streaming) pra tela ir montando o texto aos poucos — igual a API
 * multiagente deve fazer quando existir.
 */
sealed interface OlivIAReplyChunk {
    /** Mais um trecho de texto, pra concatenar no que já chegou. */
    data class Text(val text: String) : OlivIAReplyChunk

    /** A OlivIA começou a gerar um arquivo (mostra "Gerando relatório..."). */
    data object GeneratingAttachment : OlivIAReplyChunk

    /** Arquivo pronto, anexado à resposta. */
    data class Attachment(val attachment: ChatAttachment) : OlivIAReplyChunk
}
