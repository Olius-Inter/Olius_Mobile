package com.olius.app.domain.repository

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.EstablishmentType

/** Contrato de autenticação do domínio. */
interface UserRepository {
    /** Login com e-mail/senha (POST /auth/mobile/login). */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Cadastro com senha na Core e, aceito, login automático com a mesma senha. */
    suspend fun register(registration: AccountRegistration, password: String): Result<Unit>

    /** Login com o ID token do Google (Credential Manager). */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    /** "Completar cadastro" do Google: mesmos dados do cadastro, sem senha. */
    suspend fun completeGoogleRegistration(idToken: String, registration: AccountRegistration): Result<Unit>

    /** Tipos de estabelecimento para o cadastro (rota pública). */
    suspend fun establishmentTypes(): Result<List<EstablishmentType>>

    fun signOut()

    /** true se há uma sessão salva e ainda válida (usado pela Splash pra decidir a rota inicial). */
    fun isUserLoggedIn(): Boolean
}
