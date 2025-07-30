package io.github.arashiyama11.tinybudget

import android.app.Application
import androidx.room.Room
import io.github.arashiyama11.tinybudget.data.local.database.AppDatabase
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TinyBudgetApp : Application() {

    lateinit var categoryRepository: CategoryRepository
    lateinit var transactionRepository: TransactionRepository

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getDatabase(this)

        categoryRepository = CategoryRepository(database.categoryDao())
        transactionRepository = TransactionRepository(database.transactionDao())

        // Initialize default categories
        CoroutineScope(Dispatchers.IO).launch {
            initializeDefaultCategories()
        }
    }

    private suspend fun initializeDefaultCategories() {
        val categories = categoryRepository.getAllCategories().first()
        if (categories.isEmpty()) {
            categoryRepository.addCategory(Category(name = "食費"))
            categoryRepository.addCategory(Category(name = "その他"))
        }
    }
}
