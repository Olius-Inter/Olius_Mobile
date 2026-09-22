package com.olius.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.olius.app.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

/**
 * Implementação de [UserRepository] em cima do Firebase Authentication.
 * `.await()` vem de `kotlinx-coroutines-play-services` — transforma o
 * `Task<T>` do SDK do Google numa função suspensa (ver AUTH_01, seção 2).
 */
class UserRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatching {
            // GoogleAuthProvider.getCredential: transforma o ID token assinado
            // pelo Google numa credencial que o FirebaseAuth entende — mesma
            // ideia de "e-mail + senha", só que a "senha" aqui é o token do
            // Google. Segundo parâmetro null: o Credential Manager só entrega
            // ID token, não access token do OAuth clássico (ver AUTH_02, seção 7).
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Unit
        }

    override fun signOut() = auth.signOut()

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null
}
