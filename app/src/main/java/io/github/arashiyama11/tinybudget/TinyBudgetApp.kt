package io.github.arashiyama11.tinybudget

import android.app.Application
import io.github.arashiyama11.tinybudget.data.AppContainer
import io.github.arashiyama11.tinybudget.data.DefaultAppContainer
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TinyBudgetApp : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)

        // Initialize default categories
        CoroutineScope(Dispatchers.IO).launch {
            initializeDefaultCategories()
        }
    }

    private suspend fun initializeDefaultCategories() {
        val categories = appContainer.categoryRepository.getAllCategories().first()
        if (categories.isEmpty()) {
            appContainer.categoryRepository.addCategory(Category(name = "食費"))
            appContainer.categoryRepository.addCategory(Category(name = "その他"))
        }
    }
}
