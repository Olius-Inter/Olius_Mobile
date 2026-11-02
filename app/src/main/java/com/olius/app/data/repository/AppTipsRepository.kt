package com.olius.app.data.repository

import com.olius.app.domain.entity.Tip
import com.olius.app.domain.repository.TipsRepository

/** Os 4 cards da aba "Dicas" da Home B2C. */
class AppTipsRepository : TipsRepository {
    override suspend fun getTips(): Result<List<Tip>> = Result.success(TIPS)

    companion object {
        val TIPS = listOf(
            Tip(
                id = "armazenamento-correto",
                title = "Armazenamento correto",
                description = "Espere o óleo esfriar, coe os restos de comida e guarde em garrafa PET bem fechada, " +
                    "longe do sol e do calor. Assim ele não vaza, não atrai insetos e chega bom para reciclar."
            ),
            Tip(
                id = "volume-maximo",
                title = "Volume máximo",
                description = "Junte até 2 litros (uma garrafa PET) e entregue em até 3 meses. Guardado por mais " +
                    "tempo, o óleo fica rançoso, cheira mal, atrai pragas e aumenta o risco de vazar em casa."
            ),
            Tip(
                id = "nao-descarte-inadequadamente",
                title = "Não descarte inadequadamente",
                description = "Na pia ou na privada, o óleo gruda nos canos, entope o esgoto e chega a rios e mares. " +
                    "Um litro de óleo pode contaminar até 25 mil litros de água."
            ),
            Tip(
                id = "tipo-de-oleo-aceito",
                title = "Tipo de óleo aceito",
                description = "Pode: óleo vegetal de cozinha usado (soja, milho, girassol, canola), frio e coado. " +
                    "Não pode: óleo de motor ou óleo misturado com água, detergente ou restos de comida."
            )
        )
    }
}
