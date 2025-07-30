package io.github.arashiyama11.tinybudget.ui.overlay

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository

class OverlayViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    init {
        Log.d("OverlayViewModel", "Initialized")
    }
}

class OverlayViewModelFactory(
    private val categoryRepo: CategoryRepository,
    private val transactionRepo: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OverlayViewModel(categoryRepo, transactionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}