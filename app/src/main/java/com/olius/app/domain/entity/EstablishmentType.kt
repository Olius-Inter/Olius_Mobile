package com.olius.app.domain.entity

/** Tipo de estabelecimento (GET /api/v1/establishment-types): restaurante, hotel, escola... */
data class EstablishmentType(
    /** UUID enviado como `typeId` no cadastro. */
    val id: String,
    val name: String,
    val description: String? = null
)
