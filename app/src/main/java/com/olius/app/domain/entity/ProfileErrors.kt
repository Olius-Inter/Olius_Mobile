package com.olius.app.domain.entity

/**
 * Falhas do "Editar Perfil" (PATCH /users/me), já traduzidas do HTTP.
 * Sem rede, a falha continua sendo a `IOException` original.
 */
sealed class ProfileUpdateException(message: String) : Exception(message)

/** 404/405: a Core ainda não tem PATCH /users/me (hoje só existe o GET). */
class ProfileEditUnavailableException : ProfileUpdateException("Edição de perfil ainda não disponível na Core")

/** 409: o e-mail (ou o telefone) já é de outra conta. */
class ProfileConflictException : ProfileUpdateException("E-mail ou telefone já em uso")

/** 400 VALIDATION_ERROR: [fields] são os campos recusados (ex.: "email"). */
class InvalidProfileException(val fields: List<String>) : ProfileUpdateException("Campos inválidos: $fields")
