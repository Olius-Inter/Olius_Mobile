package com.olius.app.domain.repository

import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.EstablishmentType

/** [UserRepository] em memória para testes. */
class FakeUserRepository : UserRepository {
    var signInResult: Result<Unit> = Result.success(Unit)
    var registerResult: Result<Unit> = Result.success(Unit)
    var googleSignInResult: Result<Unit> = Result.success(Unit)
    var completeGoogleResult: Result<Unit> = Result.success(Unit)
    var establishmentTypesResult: Result<List<EstablishmentType>> =
        Result.success(listOf(EstablishmentType("t-1", "Restaurante"), EstablishmentType("t-2", "Hotel")))
    var loggedIn: Boolean = false

    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastIdToken: String? = null
    var lastRegistration: AccountRegistration? = null
    var signOutCalls: Int = 0
    var establishmentTypesCalls: Int = 0

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        lastEmail = email
        lastPassword = password
        return signInResult
    }

    override suspend fun register(registration: AccountRegistration, password: String): Result<Unit> {
        lastRegistration = registration
        lastEmail = registration.email
        lastPassword = password
        return registerResult
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        lastIdToken = idToken
        return googleSignInResult
    }

    override suspend fun completeGoogleRegistration(idToken: String, registration: AccountRegistration): Result<Unit> {
        lastIdToken = idToken
        lastRegistration = registration
        return completeGoogleResult
    }

    override suspend fun establishmentTypes(): Result<List<EstablishmentType>> {
        establishmentTypesCalls++
        return establishmentTypesResult
    }

    override fun signOut() {
        signOutCalls++
        loggedIn = false
    }

    override fun isUserLoggedIn(): Boolean = loggedIn
}
