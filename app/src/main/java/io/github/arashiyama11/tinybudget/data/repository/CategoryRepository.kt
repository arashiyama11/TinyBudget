package io.github.arashiyama11.tinybudget.data.repository

import io.github.arashiyama11.tinybudget.data.local.dao.CategoryDao
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getEnabledCategories(): Flow<List<Category>>
    suspend fun getCategory(id: Int): Category?
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

class CategoryRepositoryImpl(private val categoryDao: CategoryDao) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAll()
    }

    override fun getEnabledCategories(): Flow<List<Category>> {
        return categoryDao.getEnabled()
    }

    override suspend fun getCategory(id: Int): Category? {
        return categoryDao.findById(id)
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insert(category)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
}
