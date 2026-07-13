package com.quickcleanpro.phonecleaner.feature.files.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class FileUriTest {
    @Test
    fun `preserves serialized uri as identity`() {
        val serialized = "content://media/external/images/media/42"
        val uri = FileUri(serialized)

        assertEquals(serialized, uri.value)
        assertEquals(serialized, uri.toString())
        assertEquals(uri, FileUri(serialized))
    }
}
