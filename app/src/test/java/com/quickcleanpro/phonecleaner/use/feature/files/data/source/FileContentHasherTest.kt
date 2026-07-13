package com.quickcleanpro.phonecleaner.feature.files.shared.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileContentHasherTest {
    @Test
    fun `sha256 returns stable lowercase digest`() {
        val digest = FileContentHasher.sha256(ByteArrayInputStream("quick-clean".toByteArray()))

        assertEquals("109233f72c0dd98bb080e20a501b2a0c42f26a982aa15dc8aaea7fe96fe0d7c5", digest)
    }

    @Test
    fun `sha256 checks cancellation while reading`() {
        var checks = 0

        FileContentHasher.sha256(ByteArrayInputStream(ByteArray(150_000))) { checks++ }

        assertTrue(checks >= 3)
    }
}
