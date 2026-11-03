package com.olius.app.data.repository

import org.json.JSONArray
import org.json.JSONObject

/** `optString` devolve "null" para JSON null; aqui vira null de verdade. */
internal fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key)

internal fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (isNull(key)) null else optDouble(key)

/** Lista de objetos de um JSONArray opcional (ausente = lista vazia). */
internal fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }
