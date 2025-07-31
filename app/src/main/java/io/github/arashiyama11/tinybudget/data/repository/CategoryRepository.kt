package io.github.arashiyama11.tinybudget.data.repository

import io.github.arashiyama11.tinybudget.data.local.dao.CategoryDao
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAll()
    }

    fun getEnabledCategories(): Flow<List<Category>> {
        return categoryDao.getEnabled()
    }

    suspend fun getCategory(id: Int): Category? {
        return categoryDao.findById(id)
    }

    suspend fun addCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
}
