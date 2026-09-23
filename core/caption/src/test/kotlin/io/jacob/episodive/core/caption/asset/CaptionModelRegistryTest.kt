package io.jacob.episodive.core.caption.asset

import io.jacob.episodive.core.model.caption.CaptionLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionModelRegistryTest {

    @Test
    fun `every CaptionLanguage has a registered spec`() {
        CaptionLanguage.entries.forEach { language ->
            val spec = CaptionModelRegistry.spec(language)
            assertEquals(language, spec.language)
        }
    }

    @Test
    fun `spec id matches language value`() {
        CaptionLanguage.entries.forEach { language ->
            val spec = CaptionModelRegistry.spec(language)
            assertEquals(language.value, spec.id)
        }
    }

    @Test
    fun `every asset file has a 64-char hex sha256`() {
        CaptionLanguage.entries.forEach { language ->
            val spec = CaptionModelRegistry.spec(language)
            spec.files.forEach { file ->
                assertEquals(
                    "${spec.id}/${file.fileName} sha256 length",
                    64,
                    file.sha256.length,
                )
                assertTrue(
                    "${spec.id}/${file.fileName} sha256 must be hex",
                    file.sha256.matches(Regex("[0-9a-f]{64}")),
                )
            }
        }
    }

    @Test
    fun `every asset file has a positive size`() {
        CaptionLanguage.entries.forEach { language ->
            val spec = CaptionModelRegistry.spec(language)
            spec.files.forEach { file ->
                assertTrue(
                    "${spec.id}/${file.fileName} sizeBytes must be positive",
                    file.sizeBytes > 0L,
                )
            }
        }
    }

    @Test
    fun `maxLineChars is positive`() {
        CaptionLanguage.entries.forEach { language ->
            val spec = CaptionModelRegistry.spec(language)
            assertTrue(spec.maxLineChars > 0)
        }
    }

    @Test
    fun `assetUrl builds huggingface resolve url`() {
        val spec = CaptionModelRegistry.spec(CaptionLanguage.ENGLISH)
        val url = spec.assetUrl(spec.tokens)

        assertEquals(
            "https://huggingface.co/${spec.repo}/resolve/${spec.revision}/${spec.tokens.fileName}",
            url,
        )
    }
}
