package io.github.arashiyama11.tinybudget

import android.app.Application
import android.content.Context
import io.github.arashiyama11.tinybudget.di.AppContainer
import io.github.arashiyama11.tinybudget.di.DefaultAppContainer
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.tinyBudgetApp get() = applicationContext as TinyBudgetApp
val Context.appContainer get() = tinyBudgetApp.appContainer

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
