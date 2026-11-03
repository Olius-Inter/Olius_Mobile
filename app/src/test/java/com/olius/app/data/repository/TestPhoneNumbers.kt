package com.olius.app.data.repository

import io.michaelrocks.libphonenumber.android.MetadataLoader
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.io.File
import java.util.Locale
import java.util.Properties

/** libphonenumber de verdade nos testes (JVM). */
object TestPhoneNumbers {
    private val assetsDir: File by lazy {
        val properties = Properties()
        val stream = javaClass.classLoader!!.getResourceAsStream("com/android/tools/test_config.properties")
            ?: error("test_config.properties não encontrado: falta isIncludeAndroidResources no build.gradle.kts")
        stream.use(properties::load)
        File(properties.getProperty("android_merged_assets"))
    }

    private val metadataLoader = MetadataLoader { fileName -> File(assetsDir, fileName.trimStart('/')).inputStream() }

    val util: PhoneNumberUtil by lazy { PhoneNumberUtil.createInstance(metadataLoader) }

    fun repository() = LibPhoneNumberRepository(utilProvider = { util }, displayLocale = Locale("pt", "BR"))
}
