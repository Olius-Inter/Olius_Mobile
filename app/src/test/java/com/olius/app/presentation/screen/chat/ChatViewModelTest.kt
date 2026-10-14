package com.olius.app.presentation.screen.chat

import com.olius.app.MainDispatcherRule
import com.olius.app.data.repository.MockOlivIARepository
import com.olius.app.domain.entity.AttachmentKind
import com.olius.app.domain.entity.ChatAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
