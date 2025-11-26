package io.jacob.episodive.core.data.repository

import android.content.Context
import io.jacob.episodive.core.domain.repository.ImageRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ImageRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given dependencies, when creating instance, then ImageRepository is created`() {
        // Given
        val context = mockk<Context>(relaxed = true)

        // When
        val repository: ImageRepository = ImageRepositoryImpl(
            context = context,
        )

        // Then
        assertNotNull(repository)
    }
}