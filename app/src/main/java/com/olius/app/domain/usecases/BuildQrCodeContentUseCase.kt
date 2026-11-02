package com.olius.app.domain.usecases

/**
 * Conteúdo do QR de identificação: o `qrToken` do perfil, **exato** (guia da Core,
 * `docs/mobile-jornada-b2c.md` seção 2). Quem atende lê o QR e manda o mesmo texto em
 * `POST /qr/resolutions`; uma URL em volta faria a Core não achar o cidadão.
 */
class BuildQrCodeContentUseCase {

    operator fun invoke(qrToken: String?): String? = qrToken?.takeIf { it.isNotBlank() }
}
