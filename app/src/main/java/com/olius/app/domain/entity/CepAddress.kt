package com.olius.app.domain.entity

/**
 * Endereço devolvido pela consulta de CEP. [state] é a UF (2 letras), que a
 * Core exige no `address` e a tela não pede mais. Rua e bairro podem vir
 * vazios (CEP geral de cidade pequena).
 */
data class CepAddress(
    val cep: String,
    val state: String,
    val city: String,
    val neighborhood: String,
    val street: String
)
