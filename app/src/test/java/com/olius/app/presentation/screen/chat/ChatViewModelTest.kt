package com.olius.app.presentation.screen.chat

import com.olius.app.MainDispatcherRule
import com.olius.app.data.repository.MockOlivIARepository
import com.olius.app.domain.entity.AttachmentKind
import com.olius.app.domain.entity.ChatAttachment
import com.olius.app.domain.entity.OlivIAException
import com.olius.app.domain.entity.OlivIAReplyChunk
import com.olius.app.domain.repository.OlivIARepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = ChatViewModel(MockOlivIARepository(wordDelayMillis = 10, attachmentDelayMillis = 500))
    private val state get() = viewModel.uiState.value

    private fun ChatMessage.text() = parts.filterIsInstance<ChatMessagePart.Text>().joinToString("\n") { it.text }

    @Test
    fun `comeca sem mensagens e sem poder enviar`() {
        assertTrue(state.messages.isEmpty())
        assertFalse(state.canSend)
    }

    @Test
    fun `enviar texto em branco nao faz nada`() {
        viewModel.onInputChange("   ")
        viewModel.onSend()

        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `botao enviar so fica ativo com texto e nao durante a resposta`() = runTest {
        viewModel.onInputChange("")
        assertFalse(state.canSend)
        viewModel.onInputChange("  \n ")
        assertFalse(state.canSend)
        viewModel.onInputChange("Oi")
        assertTrue(state.canSend)

        viewModel.onSend()
        assertTrue(state.isGenerating)
        viewModel.onInputChange("Outra pergunta")
        assertFalse(state.canSend)

        advanceUntilIdle()
        assertTrue(state.canSend)
    }

    @Test
    fun `pergunta sobre oleo gera relatorio com arquivo entre dois textos`() = runTest {
        viewModel.onInputChange("Registrei 67 litros de óleo, faça um relatório")
        viewModel.onSend()

        assertEquals("", state.input)
        assertTrue(state.isGenerating)
        assertFalse(state.canSend)

        advanceUntilIdle()

        val (question, answer) = state.messages
        assertEquals(ChatAuthor.USER, question.author)
        assertEquals("Registrei 67 litros de óleo, faça um relatório", question.text())
        assertEquals(ChatAuthor.OLIVIA, answer.author)
        assertEquals(
            listOf(ChatMessagePart.Text::class, ChatMessagePart.File::class, ChatMessagePart.Text::class),
            answer.parts.map { it::class }
        )
        assertEquals(MockOlivIARepository.REPORT_INTRO, (answer.parts[0] as ChatMessagePart.Text).text)
        assertEquals(MockOlivIARepository.REPORT_FILE_NAME, (answer.parts[1] as ChatMessagePart.File).attachment.name)
        assertEquals(MockOlivIARepository.REPORT_OUTRO, (answer.parts[2] as ChatMessagePart.Text).text)
        assertFalse(state.isGenerating)
    }

    @Test
    fun `mostra gerando relatorio enquanto o arquivo nao fica pronto`() = runTest {
        viewModel.onSuggestionClick("Faça um relatório do meu óleo")
        advanceTimeBy(MockOlivIARepository.REPORT_INTRO.split(" ").size * 10L + 100)

        assertEquals(ChatMessagePart.GeneratingFile, state.messages.last().parts.last())
        assertTrue(state.isGenerating)
    }

    @Test
    fun `pergunta sobre impactos e outras perguntas recebem respostas mockadas`() = runTest {
        viewModel.onSuggestionClick("Quais são os impactos de não registrar?")
        advanceUntilIdle()
        assertEquals(MockOlivIARepository.IMPACT_ANSWER, state.messages.last().text())

        viewModel.onSuggestionClick("Oi")
        advanceUntilIdle()
        assertEquals(MockOlivIARepository.DEFAULT_ANSWER, state.messages.last().text())
        assertEquals(4, state.messages.size)
    }

    @Test
    fun `nao envia sugestao enquanto a OlivIA responde`() = runTest {
        viewModel.onSuggestionClick("Oi")
        viewModel.onSuggestionClick("Oi de novo")

        assertEquals(2, state.messages.size)
        advanceUntilIdle()
    }

    @Test
    fun `parar antes da primeira palavra marca a resposta como interrompida`() = runTest {
        viewModel.onSuggestionClick("Oi")

        viewModel.onStopGenerating()
        advanceUntilIdle()

        assertEquals(INTERRUPTED_REPLY_TEXT, state.messages.last().text())
        assertFalse(state.isGenerating)
    }

    @Test
    fun `parar durante o relatorio tira o gerando relatorio e mantem o texto`() = runTest {
        viewModel.onSuggestionClick("relatório do óleo")
        advanceTimeBy(MockOlivIARepository.REPORT_INTRO.split(" ").size * 10L + 100)

        viewModel.onStopGenerating()
        advanceUntilIdle()

        val answer = state.messages.last()
        assertEquals(listOf(ChatMessagePart.Text(MockOlivIARepository.REPORT_INTRO)), answer.parts)
    }

    @Test
    fun `menu de anexos abre, fecha e guarda o arquivo escolhido`() = runTest {
        val photo = ChatAttachment("praia.jpg", AttachmentKind.PHOTO, "content://foto")

        viewModel.onToggleAttachmentMenu()
        assertTrue(state.isAttachmentMenuOpen)
        viewModel.onToggleAttachmentMenu()
        assertFalse(state.isAttachmentMenuOpen)

        viewModel.onToggleAttachmentMenu()
        viewModel.onDismissAttachmentMenu()
        assertFalse(state.isAttachmentMenuOpen)

        viewModel.onToggleAttachmentMenu()
        viewModel.onAttachmentPicked(photo)
        assertFalse(state.isAttachmentMenuOpen)
        assertEquals(photo, state.pendingAttachment)
        assertTrue(state.canSend)

        viewModel.onRemovePendingAttachment()
        assertNull(state.pendingAttachment)
    }

    @Test
    fun `enviar so o anexo manda o arquivo e a OlivIA responde sobre ele`() = runTest {
        val file = ChatAttachment("notas.pdf", AttachmentKind.FILE)
        viewModel.onAttachmentPicked(file)

        viewModel.onSend()
        advanceUntilIdle()

        val (question, answer) = state.messages
        assertEquals(listOf(ChatMessagePart.File(file)), question.parts)
        assertTrue(answer.text().contains("notas.pdf"))
        assertNull(state.pendingAttachment)
    }

    // ---------------- Chat pela Core (HttpOlivIARepository) ----------------

    /** Repositório falso que guarda o conversationId recebido em cada chamada. */
    private class FakeCoreRepository(private val answer: suspend (call: Int) -> List<OlivIAReplyChunk>) : OlivIARepository {
        val conversationIds = mutableListOf<String?>()
        override fun reply(message: String, attachment: ChatAttachment?, conversationId: String?) = flow {
            conversationIds += conversationId
            answer(conversationIds.size).forEach { emit(it) }
        }
    }

    @Test
    fun `sucesso mostra a resposta e reenvia o conversationId na proxima mensagem`() = runTest {
        val repository = FakeCoreRepository { call ->
            listOf(OlivIAReplyChunk.Conversation("c-1"), OlivIAReplyChunk.Text("Resposta $call"))
        }
        val viewModel = ChatViewModel(repository)

        viewModel.onSuggestionClick("Oi")
        advanceUntilIdle()
        viewModel.onSuggestionClick("Tudo bem?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(null, "c-1"), repository.conversationIds)
        assertEquals("c-1", state.conversationId)
        assertEquals("Resposta 2", state.messages.last().text())
        assertFalse(state.isGenerating)
    }

    @Test
    fun `rota inexistente vira mensagem na conversa`() = runTest {
        val viewModel = ChatViewModel { _, _, _ -> flow<OlivIAReplyChunk> { throw OlivIAException.NotAvailable() } }

        viewModel.onSuggestionClick("Oi")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("O assistente ainda não está disponível no servidor.", state.messages.last().text())
        assertEquals(ChatAuthor.OLIVIA, state.messages.last().author)
        assertFalse(state.isGenerating)
    }

    @Test
    fun `503, timeout e sem rede viram mensagens na conversa`() = runTest {
        val errors = ArrayDeque(listOf(OlivIAException.Unavailable(), OlivIAException.Timeout(), OlivIAException.Offline()))
        val viewModel = ChatViewModel { _, _, _ ->
            flow<OlivIAReplyChunk> {
                delay(1_000)
                throw errors.removeFirst()
            }
        }
        val expected = listOf(
            "O assistente está indisponível, tente de novo.",
            "A OlivIA demorou demais para responder. Tente de novo.",
            "Sem conexão com o servidor. Confira sua internet e tente de novo."
        )

        expected.forEach { text ->
            viewModel.onSuggestionClick("Oi")
            advanceUntilIdle()
            assertEquals(text, viewModel.uiState.value.messages.last().text())
        }
        assertEquals(6, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `erro inesperado vira mensagem generica sem derrubar o app`() = runTest {
        val viewModel = ChatViewModel { _, _, _ -> flow<OlivIAReplyChunk> { throw IllegalStateException("json estranho") } }

        viewModel.onSuggestionClick("Oi")
        advanceUntilIdle()

        assertEquals("Não consegui responder agora. Tente de novo.", viewModel.uiState.value.messages.last().text())
    }

    @Test
    fun `parar cancela a chamada e mostra resposta interrompida`() = runTest {
        var cancelled = false
        val viewModel = ChatViewModel { _, _, _ ->
            flow<OlivIAReplyChunk> {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
        }

        viewModel.onSuggestionClick("Oi")
        advanceTimeBy(5_000)
        assertTrue(viewModel.uiState.value.isGenerating)

        viewModel.onStopGenerating()
        advanceUntilIdle()

        assertTrue(cancelled)
        assertEquals(INTERRUPTED_REPLY_TEXT, viewModel.uiState.value.messages.last().text())
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun `parar quando a conexao fechada vira erro de rede ainda mostra interrompida`() = runTest {
        // O HttpURLConnection fechado pelo cancelamento lança IOException → Offline.
        val viewModel = ChatViewModel { _, _, _ ->
            flow<OlivIAReplyChunk> {
                try {
                    delay(60_000)
                } catch (e: CancellationException) {
                    throw OlivIAException.Offline(e)
                }
            }
        }

        viewModel.onSuggestionClick("Oi")
        advanceTimeBy(1_000)
        viewModel.onStopGenerating()
        advanceUntilIdle()

        assertEquals(INTERRUPTED_REPLY_TEXT, viewModel.uiState.value.messages.last().text())
    }
}
