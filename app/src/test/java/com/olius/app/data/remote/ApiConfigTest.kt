package com.olius.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** As base URLs vêm do local.properties (BuildConfig) e sempre terminam com "/". */
class ApiConfigTest {
    @Test
    fun `withTrailingSlash adds the slash only when missing`() {
        assertEquals("http://host:8080/", ApiConfig.withTrailingSlash("http://host:8080"))
        assertEquals("http://host:8080/", ApiConfig.withTrailingSlash("http://host:8080/"))
    }

    @Test
    fun `base urls come from BuildConfig and end with a slash`() {
        assertEquals(ApiConfig.withTrailingSlash(com.olius.app.BuildConfig.OLIUS_CORE_BASE_URL), ApiConfig.MAIN_BASE_URL)
        assertEquals(ApiConfig.withTrailingSlash(com.olius.app.BuildConfig.OLIUS_RANKING_BASE_URL), ApiConfig.RANKING_BASE_URL)
        assertTrue(ApiConfig.MAIN_BASE_URL.endsWith("/"))
        assertTrue(ApiConfig.RANKING_BASE_URL.endsWith("/"))
    }
}
