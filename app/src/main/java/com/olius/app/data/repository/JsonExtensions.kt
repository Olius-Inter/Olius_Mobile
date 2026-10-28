package com.olius.app.data.repository

import org.json.JSONObject

/** `optString` devolve "null" para JSON null; aqui vira null de verdade. */
internal fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key)
