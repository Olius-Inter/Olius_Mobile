package com.olius.app.domain.repository

/**
 * Contrato de autenticação do domínio — não conhece Firebase nem nenhum
 * outro SDK, só descreve as operações que o app precisa. Quem implementa de
 * verdade (hoje, com FirebaseAuth) é [com.olius.app.data.repository.UserRepositoryImpl].
 * Ver AUTH_03_ARQUITETURA_USECASES.md pro porquê dessa camada existir.
 */
interface UserRepository {

    /** Login com e-mail/senha. `Result.failure` carrega a exceção original do provedor. */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Cadastro com e-mail/senha. */
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>

    /** Troca um ID token do Google (Credential Manager) por uma sessão autenticada. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    /** Encerra a sessão atual. Síncrono — o provedor não precisa de chamada de rede pra isso. */
    fun signOut()

    /** true se já existe uma sessão válida (usado pela Splash pra decidir a rota inicial). */
    fun isUserLoggedIn(): Boolean
}
