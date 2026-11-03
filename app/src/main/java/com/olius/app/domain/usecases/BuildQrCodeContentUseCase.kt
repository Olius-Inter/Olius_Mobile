package com.olius.app.domain.usecases

/** Conteúdo do QR de identificação: o `qrToken` do perfil, **exato**. */
class BuildQrCodeContentUseCase {
    operator fun invoke(qrToken: String?): String? = qrToken?.takeIf { it.isNotBlank() }
}
