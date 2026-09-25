package com.olius.app.domain.repository

/**
 * [UserRepository] em memória para testes: devolve o [Result] configurado em
 * cada campo e registra os argumentos recebidos, sem tocar no Firebase.
 */
class FakeUserRepository : UserRepository {

    var signInResult: Result<Unit> = Result.success(Unit)
    var registerResult: Result<Unit> = Result.success(Unit)
    var googleSignInResult: Result<Unit> = Result.success(Unit)
    var loggedIn: Boolean = false

    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastIdToken: String? = null
    var signOutCalls: Int = 0

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        lastEmail = email
        lastPassword = password
        return signInResult
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        lastEmail = email
        lastPassword = password
        return registerResult
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        lastIdToken = idToken
        return googleSignInResult
    }

    override fun signOut() {
        signOutCalls++
        loggedIn = false
    }

    override fun isUserLoggedIn(): Boolean = loggedIn
}
