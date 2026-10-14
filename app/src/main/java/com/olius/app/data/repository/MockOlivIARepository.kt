package com.olius.app.data.repository

import com.olius.app.domain.entity.AttachmentKind
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAReplyChunk
import com.olius.app.domain.repository.OlivIARepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Respostas prontas da OlivIA só pra visualizar o chat enquanto a API não
 * existe (mesmas conversas de ref_chat_message.png / ref_chat_plus.png).
 * O texto chega palavra por palavra, simulando o streaming da API real.
 */
class MockOlivIARepository(
    private val wordDelayMillis: Long = 45L,
    private val attachmentDelayMillis: Long = 1_800L
) : OlivIARepository {

    override fun reply(message: String, attachment: ChatAttachment?): Flow<OlivIAReplyChunk> = flow {
        val normalized = message.lowercase()
        when {
            attachment != null -> streamText(
                "Recebi \"${attachment.name}\"! Assim que minha integração estiver pronta, " +
                    "vou conseguir analisar esse arquivo e te dar um parecer sobre ele."
            )

            "impacto" in normalized -> streamText(IMPACT_ANSWER)

            "óleo" in normalized || "oleo" in normalized || "relatório" in normalized -> {
                streamText(REPORT_INTRO)
                emit(OlivIAReplyChunk.GeneratingAttachment)
                delay(attachmentDelayMillis)
                emit(OlivIAReplyChunk.Attachment(ChatAttachment(REPORT_FILE_NAME, AttachmentKind.REPORT)))
                streamText(REPORT_OUTRO)
            }

            else -> streamText(DEFAULT_ANSWER)
        }
    }

    private suspend fun FlowCollector<OlivIAReplyChunk>.streamText(text: String) {
        text.split(" ").forEachIndexed { index, word ->
            delay(wordDelayMillis)
            emit(OlivIAReplyChunk.Text(if (index == 0) word else " $word"))
        }
    }

    companion object {
        const val REPORT_FILE_NAME = "Relatorio_Oleo.txt"

        const val REPORT_INTRO = "67L de óleo é um ótimo avanço para um consumidor sustentável B2B, Naldo!\n" +
            "Aqui está o seu resumo financeiro:"

        const val REPORT_OUTRO = "Se quiser, posso criar um PDF ou um Markdown. 😉"

        const val IMPACT_ANSWER = "Cada litro de óleo descartado no ralo pode contaminar até 25 mil litros de água, " +
            "entope a rede de esgoto e, ao se decompor, libera metano — um gás que esquenta o planeta muito " +
            "mais que o CO². Registrando o seu óleo, ele vira biodiesel e você acompanha tudo isso na Home."

        const val DEFAULT_ANSWER = "Posso te ajudar com rotas e pontos de coleta, análise do seu negócio, " +
            "dúvidas sobre o descarte de óleo e o seu impacto sustentável. Sobre o que quer conversar?"
    }
}
