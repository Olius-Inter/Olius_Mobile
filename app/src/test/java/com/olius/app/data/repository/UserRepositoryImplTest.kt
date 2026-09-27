package com.olius.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryImplTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val repository = UserRepositoryImpl(auth)

    @After
    fun tearDown() = unmockkAll()

    /** Task já concluída — `await()` devolve/lança na hora, sem precisar de listener. */
    private fun completedTask(error: Exception? = null): Task<AuthResult> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns error
        every { result } returns mockk()
    }

    @Test
    fun `signInWithEmail devolve sucesso quando o Firebase conclui`() = runTest {
        every { auth.signInWithEmailAndPassword("a@b.com", "123456") } returns completedTask()

        assertTrue(repository.signInWithEmail("a@b.com", "123456").isSuccess)
    }

    @Test
    fun `signInWithEmail devolve a excecao do Firebase como falha`() = runTest {
        val error = IllegalStateException("credenciais")
        every { auth.signInWithEmailAndPassword(any(), any()) } returns completedTask(error)

        val result = repository.signInWithEmail("a@b.com", "errada")

        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `registerWithEmail cria a conta no Firebase`() = runTest {
        every { auth.createUserWithEmailAndPassword("a@b.com", "123456") } returns completedTask()

        assertTrue(repository.registerWithEmail("a@b.com", "123456").isSuccess)
    }

    @Test
    fun `signInWithGoogleIdToken troca o token por credencial do Firebase`() = runTest {
        val credential = mockk<AuthCredential>()
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential("token", null) } returns credential
        every { auth.signInWithCredential(credential) } returns completedTask()

        assertTrue(repository.signInWithGoogleIdToken("token").isSuccess)
    }

    @Test
    fun `signOut encerra a sessao do Firebase`() {
        repository.signOut()

        verify { auth.signOut() }
    }

    @Test
    fun `isUserLoggedIn depende de existir usuario atual`() {
        every { auth.currentUser } returns null
        assertFalse(repository.isUserLoggedIn())

        every { auth.currentUser } returns mockk<FirebaseUser>()
        assertTrue(repository.isUserLoggedIn())
    }

    @Test
    fun `construtor padrao usa a instancia global do FirebaseAuth`() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns null

        assertFalse(UserRepositoryImpl().isUserLoggedIn())
    }
}
