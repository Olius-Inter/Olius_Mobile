package com.olius.app.domain.repository

import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAReplyChunk
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do chat com a OlivIA. [MockOlivIARepository][com.olius.app.data.repository.MockOlivIARepository]
 * (respostas prontas) ou [HttpOlivIARepository][com.olius.app.data.repository.HttpOlivIARepository]
 * (Core → IA), escolhido pela flag `ApiConfig.USE_MOCK_OLIVIA`.
 * [conversationId]: null na 1ª mensagem; depois, o que veio em [OlivIAReplyChunk.Conversation].
 * Falhas sobem como [com.olius.app.domain.entity.OlivIAException].
 */
fun interface OlivIARepository {
    fun reply(message: String, attachment: ChatAttachment?, conversationId: String?): Flow<OlivIAReplyChunk>
}
