package com.olius.app.domain.entity

/** Endereço devolvido pela consulta de CEP. */
data class CepAddress(
    val cep: String,
    val state: String,
    val city: String,
    val neighborhood: String,
    val street: String
)
