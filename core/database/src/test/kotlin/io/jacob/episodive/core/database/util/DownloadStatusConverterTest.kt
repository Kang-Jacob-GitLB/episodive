package io.jacob.episodive.core.database.util

import io.jacob.episodive.core.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStatusConverterTest {

    private val converter = DownloadStatusConverter()

    @Test
    fun `fromDownloadStatus converts each status to its value`() {
        assertEquals("pending", converter.fromDownloadStatus(DownloadStatus.PENDING))
        assertEquals("downloading", converter.fromDownloadStatus(DownloadStatus.DOWNLOADING))
        assertEquals("completed", converter.fromDownloadStatus(DownloadStatus.COMPLETED))
        assertEquals("failed", converter.fromDownloadStatus(DownloadStatus.FAILED))
    }

    @Test
    fun `fromDownloadStatus returns null when status is null`() {
        assertNull(converter.fromDownloadStatus(null))
    }

    @Test
    fun `toDownloadStatus converts each value to its status`() {
        assertEquals(DownloadStatus.PENDING, converter.toDownloadStatus("pending"))
        assertEquals(DownloadStatus.DOWNLOADING, converter.toDownloadStatus("downloading"))
        assertEquals(DownloadStatus.COMPLETED, converter.toDownloadStatus("completed"))
        assertEquals(DownloadStatus.FAILED, converter.toDownloadStatus("failed"))
    }

    @Test
    fun `toDownloadStatus returns null when value is null`() {
        assertNull(converter.toDownloadStatus(null))
    }

    @Test
    fun `toDownloadStatus returns null for unknown value`() {
        assertNull(converter.toDownloadStatus("unknown"))
    }

    @Test
    fun `roundtrip conversion preserves status`() {
        DownloadStatus.entries.forEach { status ->
            val value = converter.fromDownloadStatus(status)
            val restored = converter.toDownloadStatus(value)
            assertEquals(status, restored)
        }
    }
}
