package com.olius.app.data.repository

import com.olius.app.data.remote.ApiClient
import com.olius.app.data.remote.ApiConfig
import com.olius.app.data.remote.ApiException
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAException
import com.olius.app.domain.entity.OlivIAReplyChunk
import com.olius.app.domain.repository.OlivIARepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

/** Chat da OlivIA pela Core: POST [ApiConfig.Main.CHAT] {message, conversationId?} → {reply, conversationId}. */
class HttpOlivIARepository(private val client: ApiClient) : OlivIARepository {
    override fun reply(message: String, attachment: ChatAttachment?, conversationId: String?): Flow<OlivIAReplyChunk> = flow {
        if (message.isBlank()) {
            emit(OlivIAReplyChunk.Text(ATTACHMENT_ONLY_TEXT))
            return@flow
        }
        val body = JSONObject().put("message", message)
        conversationId?.let { body.put("conversationId", it) }

        val json = call { JSONObject(client.post(ApiConfig.Main.CHAT, body.toString())) }
        val reply = json.optStringOrNull("reply")?.takeIf { it.isNotBlank() }
            ?: throw OlivIAException.Unknown()

        json.optStringOrNull("conversationId")?.takeIf { it.isNotBlank() }?.let {
            emit(OlivIAReplyChunk.Conversation(it))
        }
        emit(OlivIAReplyChunk.Text(reply))
        if (attachment != null) emit(OlivIAReplyChunk.Text("\n\n" + attachmentNotSentText(attachment.name)))
    }

    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: ApiException) {
            throw e.toOlivIAException()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw OlivIAException.Timeout(e)
        } catch (e: IOException) {
            throw OlivIAException.Offline(e)
        } catch (e: JSONException) {
            throw OlivIAException.Unknown(e)
        }

    private fun ApiException.toOlivIAException(): OlivIAException = when (code) {
        HTTP_NOT_FOUND, HTTP_METHOD_NOT_ALLOWED -> OlivIAException.NotAvailable()
        HTTP_UNAVAILABLE -> OlivIAException.Unavailable()
        HTTP_GATEWAY_TIMEOUT -> OlivIAException.Timeout(this)
        HTTP_TOO_MANY_REQUESTS -> OlivIAException.TooManyRequests()
        HTTP_UNAUTHORIZED -> OlivIAException.SessionExpired()
        else -> OlivIAException.Unknown(this)
    }

    companion object {
        const val ATTACHMENT_ONLY_TEXT = "Ainda não consigo ler arquivos. Escreva sua pergunta em texto que eu te ajudo."

        fun attachmentNotSentText(fileName: String) =
            "Obs.: o arquivo \"$fileName\" não foi enviado. Por enquanto eu só leio texto."

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_METHOD_NOT_ALLOWED = 405
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_UNAVAILABLE = 503
        private const val HTTP_GATEWAY_TIMEOUT = 504
    }
}
