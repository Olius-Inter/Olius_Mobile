package com.olius.app.domain.repository

import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAReplyChunk
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do chat com a OlivIA. Hoje só existe a implementação mockada
 * ([com.olius.app.data.repository.MockOlivIARepository]); quando a API
 * multiagente existir, basta uma nova implementação que devolva o mesmo Flow.
 */
interface OlivIARepository {
    fun reply(message: String, attachment: ChatAttachment?): Flow<OlivIAReplyChunk>
}
