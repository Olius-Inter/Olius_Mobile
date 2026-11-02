package com.olius.app.domain.entity

/**
 * Falhas de login/cadastro na Core, já traduzidas do HTTP (código + `code`
 * do ProblemDetail). O ViewModel só escolhe o texto da tela a partir delas.
 * Sem rede, a falha continua sendo a `IOException` original.
 */
sealed class AuthException(message: String) : Exception(message)

/** 401 no /auth/mobile/login: e-mail ou senha errados (ou conta inativa). */
class InvalidCredentialsException : AuthException("Credenciais inválidas")

/** 409 REGISTRATION_CONFLICT: e-mail, CPF ou CNPJ já cadastrado (a Core não diz qual). */
class AccountConflictException : AuthException("Cadastro em conflito")

/** 409 DUPLICATE_TELEPHONE. */
class DuplicateTelephoneException : AuthException("Telefone já cadastrado")

/** 400 VALIDATION_ERROR: [fields] são os nomes dos campos recusados (ex.: "password", "cpf"). */
class InvalidRegistrationException(val fields: List<String>) : AuthException("Campos inválidos: $fields")

/** 429: limite de tentativas da Core (60 por minuto por IP). */
class TooManyAttemptsException : AuthException("Muitas tentativas")

/**
 * Login com Google de quem ainda não tem conta na Core (404 com código
 * PROFILE_REQUIRED): o app abre "Completar cadastro".
 */
class ProfileRequiredException : AuthException("Conta ainda não cadastrada")

/** A rota ainda não existe na Core (404 sem PROFILE_REQUIRED). Ex.: login com Google hoje. */
class AuthUnavailableException : AuthException("Recurso ainda não disponível na Core")

/** A sessão acabou (refresh recusado): é preciso entrar de novo. */
class SessionExpiredException : AuthException("Sessão expirada")

/**
 * Esqueci a senha: código errado ou vencido (400 no /verify) ou o
 * `resetToken` do passo 3 já venceu/foi usado (400 no /confirm sem erro na senha).
 */
class InvalidResetCodeException : AuthException("Código inválido ou expirado")

/** Esqueci a senha: a Core recusou a nova senha (400 no /confirm com `newPassword` inválido). */
class WeakPasswordException : AuthException("Senha fora da regra")
