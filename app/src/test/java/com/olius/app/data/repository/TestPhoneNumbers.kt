package com.olius.app.data.repository

import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.io.File
import java.util.Locale
import java.util.Properties

/**
 * libphonenumber de verdade nos testes (JVM). No app ela lê os metadados dos
 * assets com o Context; aqui eles vêm da pasta de assets mesclados que o AGP
 * informa no `test_config.properties` (`unitTests.isIncludeAndroidResources`).
 */
object TestPhoneNumbers {

    private val assetsDir: File by lazy {
        val properties = Properties()
        val stream = javaClass.classLoader!!.getResourceAsStream("com/android/tools/test_config.properties")
            ?: error("test_config.properties não encontrado: falta isIncludeAndroidResources no build.gradle.kts")
        stream.use(properties::load)
        File(properties.getProperty("android_merged_assets"))
    }

    // A lib pede caminhos com "/" na frente (ex.: "/io/michaelrocks/.../PhoneNumberMetadataProto_BR").
    private val metadataLoader = MetadataLoader { fileName -> File(assetsDir, fileName.trimStart('/')).inputStream() }

    val util: PhoneNumberUtil by lazy { PhoneNumberUtil.createInstance(metadataLoader) }

    fun repository() = LibPhoneNumberRepository(utilProvider = { util }, displayLocale = Locale("pt", "BR"))
}
