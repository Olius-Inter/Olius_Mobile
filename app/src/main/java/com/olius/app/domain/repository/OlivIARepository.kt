package com.olius.app.domain.repository

import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAReplyChunk
import kotlinx.coroutines.flow.Flow

/** Contrato do chat com a OlivIA: no app, o [HttpOlivIARepository] (Core → IA). */
fun interface OlivIARepository {
    fun reply(message: String, attachment: ChatAttachment?, conversationId: String?): Flow<OlivIAReplyChunk>
}
