package com.olius.app.data.repository

import com.olius.app.data.remote.ApiException
import com.olius.app.data.remote.CoreAuthApi
import com.olius.app.domain.entity.AccountConflictException
import com.olius.app.domain.entity.AccountRegistration
import com.olius.app.domain.entity.AuthUnavailableException
import com.olius.app.domain.entity.DuplicateTelephoneException
import com.olius.app.domain.entity.EstablishmentType
import com.olius.app.domain.entity.InvalidCredentialsException
import com.olius.app.domain.entity.InvalidRegistrationException
import com.olius.app.domain.entity.ProfileRequiredException
import com.olius.app.domain.entity.TooManyAttemptsException
import com.olius.app.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException

/** Implementação de [UserRepository] com a Core (sem Firebase). */
class UserRepositoryImpl(
    private val api: CoreAuthApi,
    private val session: CoreSessionRepository
) : UserRepository {
    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> = authCall {
        session.start(api.login(email.trim(), password))
    }

    override suspend fun register(registration: AccountRegistration, password: String): Result<Unit> = authCall {
        api.register(registration, password)
        session.start(api.login(registration.email, password))
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = authCall {
        session.start(api.googleLogin(idToken))
    }

    override suspend fun completeGoogleRegistration(idToken: String, registration: AccountRegistration): Result<Unit> =
        authCall { session.start(api.googleRegister(idToken, registration)) }

    override suspend fun establishmentTypes(): Result<List<EstablishmentType>> =
        authCall { api.establishmentTypes() }

    override fun signOut() = session.signOut()

    override fun isUserLoggedIn(): Boolean = session.hasSession()

    private inline fun <T> authCall(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: ApiException) {
            Result.failure(e.toAuthException())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun ApiException.toAuthException(): Exception = when {
        code == HTTP_UNAUTHORIZED -> InvalidCredentialsException()
        code == HTTP_NOT_FOUND && problemCode == PROFILE_REQUIRED -> ProfileRequiredException()
        code == HTTP_NOT_FOUND -> AuthUnavailableException()
        code == HTTP_CONFLICT && problemCode == DUPLICATE_TELEPHONE -> DuplicateTelephoneException()
        code == HTTP_CONFLICT -> AccountConflictException()
        code == HTTP_BAD_REQUEST && problemCode == VALIDATION_ERROR -> InvalidRegistrationException(problemFields)
        code == HTTP_TOO_MANY_REQUESTS -> TooManyAttemptsException()
        else -> this
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val PROFILE_REQUIRED = "PROFILE_REQUIRED"
        const val DUPLICATE_TELEPHONE = "DUPLICATE_TELEPHONE"
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
    }
}
