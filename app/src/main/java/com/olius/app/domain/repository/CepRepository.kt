package com.olius.app.domain.repository

import com.olius.app.domain.entity.CepAddress

/** Consulta de CEP (hoje o ViaCEP), usada no "Vire Coletor" para preencher o endereço. */
interface CepRepository {

    /**
     * [cep] com 8 dígitos. Sucesso com null = CEP não existe;
     * falha = sem internet ou serviço fora do ar.
     */
    suspend fun lookup(cep: String): Result<CepAddress?>
}
