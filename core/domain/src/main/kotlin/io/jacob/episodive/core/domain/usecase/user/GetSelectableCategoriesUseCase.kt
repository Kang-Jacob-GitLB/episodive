package io.jacob.episodive.core.domain.usecase.user

import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.SelectableCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetSelectableCategoriesUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<SelectableCategory>> =
        userRepository.getCategories().flatMapLatest { preferredCategories ->
            Category.entries.map { category ->
                SelectableCategory(
                    category = category,
                    isSelected = preferredCategories.contains(category),
                )
            }.let { flowOf(it) }
        }
}