package com.olius.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Leitura da resposta do ViaCEP (sem rede). */
class ViaCepRepositoryTest {

    @Test
    fun `resposta valida vira endereco com UF`() {
        val body = """{"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé",
            "localidade":"São Paulo","uf":"SP"}"""

        val address = ViaCepRepository.parse(body)!!

        assertEquals("01001000", address.cep)
        assertEquals("SP", address.state)
        assertEquals("São Paulo", address.city)
        assertEquals("Sé", address.neighborhood)
        assertEquals("Praça da Sé", address.street)
    }

    @Test
    fun `erro true ou texto true vira CEP inexistente`() {
        assertNull(ViaCepRepository.parse("""{"erro": true}"""))
        assertNull(ViaCepRepository.parse("""{"erro": "true"}"""))
    }

    @Test
    fun `CEP geral sem rua e bairro mantem vazios`() {
        val address = ViaCepRepository.parse("""{"cep":"78175-000","logradouro":"","bairro":"","localidade":"Poconé","uf":"MT"}""")!!

        assertEquals("MT", address.state)
        assertEquals("", address.street)
        assertEquals("", address.neighborhood)
    }
}
