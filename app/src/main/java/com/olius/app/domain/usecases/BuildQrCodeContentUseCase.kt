package com.olius.app.domain.usecases

import java.net.URLEncoder

/** Conteúdo do QR de identificação: o link do Forms com o `qrToken` do perfil. Nenhum outro dado vai no link. */
class BuildQrCodeContentUseCase(private val formsBaseUrl: String) {
    operator fun invoke(qrToken: String?): String? {
        val token = qrToken?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return "$formsBaseUrl?qrToken=${URLEncoder.encode(token, Charsets.UTF_8.name())}"
    }
}
