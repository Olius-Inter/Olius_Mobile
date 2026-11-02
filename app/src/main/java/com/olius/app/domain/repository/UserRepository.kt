package com.olius.app.domain.repository

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.EstablishmentType

/**
 * Contrato de autenticação do domínio — não conhece HTTP nem nenhum SDK, só
 * descreve as operações que o app precisa. Quem implementa de verdade (com a
 * Core: JWT + refresh) é [com.olius.app.data.repository.UserRepositoryImpl].
 * As falhas conhecidas chegam como [com.olius.app.domain.entity.AuthException].
 */
interface UserRepository {

    /** Login com e-mail/senha (POST /auth/mobile/login). */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Cadastro com senha na Core e, aceito, login automático com a mesma senha. */
    suspend fun register(registration: AccountRegistration, password: String): Result<Unit>

    /**
     * Login com o ID token do Google (Credential Manager). Conta que ainda não
     * existe na Core → falha com ProfileRequiredException ("Completar cadastro").
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    /** "Completar cadastro" do Google: mesmos dados do cadastro, sem senha. Já devolve a sessão. */
    suspend fun completeGoogleRegistration(idToken: String, registration: AccountRegistration): Result<Unit>

    /** Tipos de estabelecimento para o cadastro (rota pública). */
    suspend fun establishmentTypes(): Result<List<EstablishmentType>>

    /**
     * Encerra a sessão: apaga os tokens do aparelho na hora e avisa a Core
     * (POST /auth/mobile/logout) em segundo plano.
     */
    fun signOut()

    /** true se há uma sessão salva e ainda válida (usado pela Splash pra decidir a rota inicial). */
    fun isUserLoggedIn(): Boolean
}
