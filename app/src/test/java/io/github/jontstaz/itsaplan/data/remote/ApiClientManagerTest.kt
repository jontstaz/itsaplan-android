package io.github.jontstaz.itsaplan.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientManagerTest {

    @Test
    fun `https is always accepted`() {
        ApiClientManager.assertAcceptableTransport("https://example.com")
        ApiClientManager.assertAcceptableTransport("https://192.168.1.10:8443")
    }

    @Test
    fun `plain http is rejected for public hosts`() {
        listOf("http://example.com", "http://192.168.1.10", "http://10.1.2.3").forEach { url ->
            val failed = runCatching { ApiClientManager.assertAcceptableTransport(url) }.isFailure
            assertTrue("expected rejection for $url", failed)
        }
    }

    @Test
    fun `plain http is allowed for loopback and emulator hosts`() {
        listOf("http://localhost:3001", "http://127.0.0.1:8080", "http://10.0.2.2:8080").forEach { url ->
            runCatching { ApiClientManager.assertAcceptableTransport(url) }
                .onFailure { throw AssertionError("unexpected rejection for $url", it) }
        }
    }

    @Test
    fun `a host without a scheme defaults to https`() {
        assertEquals("https://example.com", ApiClientManager.normalizeServerUrl("example.com"))
    }

    @Test
    fun `local development hosts are recognised`() {
        assertTrue(ApiClientManager.isLocalDevelopmentHost("localhost"))
        assertTrue(ApiClientManager.isLocalDevelopmentHost("10.0.2.2"))
        assertFalse(ApiClientManager.isLocalDevelopmentHost("example.com"))
    }
}
