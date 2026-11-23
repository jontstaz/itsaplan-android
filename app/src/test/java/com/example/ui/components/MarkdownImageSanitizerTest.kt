package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownImageSanitizerTest {

    @Test
    fun `third-party images become text`() {
        val input = "Look ![chart](https://tracker.example.com/pixel.gif)"
        assertEquals(
            "Look [image: chart]",
            sanitizeImages(input, trustedImageHost = "itsaplan.example.com", loadUntrusted = false)
        )
    }

    @Test
    fun `images on the configured server are kept`() {
        val input = "![shot](https://itsaplan.example.com/attachments/1/raw)"
        assertEquals(
            input,
            sanitizeImages(input, trustedImageHost = "itsaplan.example.com", loadUntrusted = false)
        )
    }

    @Test
    fun `relative images are kept`() {
        val input = "![shot](/attachments/1/raw)"
        assertEquals(input, sanitizeImages(input, null, loadUntrusted = false))
    }

    @Test
    fun `fenced code is untouched`() {
        val input = "```\n![x](https://tracker.example.com/a.png)\n```"
        assertEquals(input, sanitizeImages(input, "itsaplan.example.com", loadUntrusted = false))
    }

    @Test
    fun `the escape hatch keeps everything`() {
        val input = "![x](https://tracker.example.com/a.png)"
        assertEquals(input, sanitizeImages(input, null, loadUntrusted = true))
    }

    @Test
    fun `host trust is exact`() {
        assertTrue(isTrustedImage("https://itsaplan.example.com/a.png", "itsaplan.example.com"))
        assertFalse(isTrustedImage("https://evil.example.com/a.png", "itsaplan.example.com"))
        assertFalse(isTrustedImage("https://itsaplan.example.com.evil.com/a.png", "itsaplan.example.com"))
        assertFalse(isTrustedImage("https://anything.example.com/a.png", null))
    }
}
