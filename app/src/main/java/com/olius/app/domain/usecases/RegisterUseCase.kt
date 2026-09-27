package com.olius.app.domain.usecases

import com.olius.app.domain.repository.UserRepository

/**
 * Cadastro com e-mail/senha. Convertida de `RegisterUseCases.java` pra Kotlin
 * (era a única classe Java do projeto) — ver AUTH_03_ARQUITETURA_USECASES.md.
 *
 * Só cria a conta no Firebase Auth (e-mail/senha/uid). Nome, telefone e data
 * de nascimento digitados no cadastro NÃO têm pra onde ir aqui — precisam de
 * um backend/Firestore próprio (ver AUTH_01, seção 7, e o novo
 * AUTH_07_OBJETO_DE_CADASTRO.md sobre como montar esse envio).
 */
class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.registerWithEmail(email, password)
}
