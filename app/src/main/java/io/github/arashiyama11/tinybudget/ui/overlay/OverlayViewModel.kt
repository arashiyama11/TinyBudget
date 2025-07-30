package io.github.arashiyama11.tinybudget.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverlayUiState(
    val amount: Long = 0L, // StringからLongに変更
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val closeOverlay: Boolean = false
)

class OverlayViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    val amountStep: Long = 10 // 金額の変動単位

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onAmountChange(newAmount: Long) {
        val finalAmount = if (newAmount < 0) 0 else newAmount
        _uiState.update { it.copy(amount = finalAmount) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onCategorySelected(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount
            val categoryId = state.selectedCategoryId

            if (amount > 0 && categoryId != null) {
                transactionRepository.addTransaction(
                    Transaction(
                        amount = amount,
                        categoryId = categoryId,
                        note = state.note,
                        timestamp = System.currentTimeMillis()
                    )
                )
                close()
            }
        }
    }

    fun close() {
        _uiState.update { it.copy(closeOverlay = true) }
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