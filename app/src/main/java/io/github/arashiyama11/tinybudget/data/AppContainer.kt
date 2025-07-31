package io.github.arashiyama11.tinybudget.data

import android.content.Context
import io.github.arashiyama11.tinybudget.data.local.database.AppDatabase
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.data.repository.dataStore

interface AppContainer {
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(AppDatabase.getDatabase(context).categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(AppDatabase.getDatabase(context).transactionDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }
}
