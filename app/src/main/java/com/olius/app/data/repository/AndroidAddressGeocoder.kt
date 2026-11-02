package com.olius.app.data.repository

import android.content.Context
import android.location.Geocoder
import com.olius.app.domain.entity.GeoPoint
import com.olius.app.domain.repository.AddressGeocoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Geocodificação do próprio Android (sem chave de API). Sem serviço no aparelho → null. */
class AndroidAddressGeocoder(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AddressGeocoder {

    // A versão com callback só existe a partir da API 33; esta roda numa thread de IO.
    @Suppress("DEPRECATION")
    override suspend fun locate(query: String): GeoPoint? = withContext(ioDispatcher) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching { Geocoder(context, Locale.forLanguageTag("pt-BR")).getFromLocationName(query, 1) }
            .getOrNull()
            ?.firstOrNull()
            ?.let { GeoPoint(it.latitude, it.longitude) }
    }
}
