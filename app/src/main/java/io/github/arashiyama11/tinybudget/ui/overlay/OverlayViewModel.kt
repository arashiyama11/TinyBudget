package io.github.arashiyama11.tinybudget.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.tinybudget.di.AppContainer
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverlayUiState(
    val isNumericInputMode: Boolean = false,
    val amount: Long = 0L,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val closeOverlay: Boolean = false,
    val showSaveConfirmation: Boolean = false,
    val sync: Boolean = false,
    val sensitivity: Float = 1f,
    val frictionMultiplier: Float = 1f,
    val amountStep: Long = 10L,
)

class OverlayViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val stopSelf: suspend () -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = combine(
        _uiState,
        settingsRepository.sensitivity,
        settingsRepository.frictionMultiplier,
        settingsRepository.amountStep
    ) { state, sensitivity, frictionMultiplier, amountStep ->
        state.copy(
            sensitivity = sensitivity,
            frictionMultiplier = frictionMultiplier,
            amountStep = amountStep,
        )
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.Eagerly,
        OverlayUiState()
    )

    init {
        viewModelScope.launch {
            val lastModeIsNumeric = settingsRepository.isLastModeNumeric.first()
            _uiState.update { it.copy(isNumericInputMode = lastModeIsNumeric) }

            categoryRepository.getEnabledCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }

                if (_uiState.value.selectedCategoryId == null && categories.isNotEmpty()) {
                    val lastCategoryId = settingsRepository.lastCategoryId.first()
                    val defaultCategoryId = settingsRepository.defaultCategoryId.first()

                    val selectedId = when {
                        lastCategoryId != null && categories.any { it.id == lastCategoryId } -> lastCategoryId
                        defaultCategoryId != null && categories.any { it.id == defaultCategoryId } -> defaultCategoryId
                        else -> categories.first().id
                    }

                    _uiState.update { it.copy(selectedCategoryId = selectedId) }
                }
            }
        }
    }

    fun onAmountChange(newAmount: Long) {
        val finalAmount =
            if (newAmount < 0) 0L else newAmount
        _uiState.update { it.copy(amount = finalAmount) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onToggleNumericInputMode() {
        val newMode = !_uiState.value.isNumericInputMode
        _uiState.update { it.copy(isNumericInputMode = newMode) }
        viewModelScope.launch {
            settingsRepository.setLastModeNumeric(newMode)
        }
    }

    fun onCategorySelected(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            settingsRepository.setLastCategoryId(categoryId)
        }
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

                _uiState.update {
                    it.copy(
                        amount = 0L,
                        showSaveConfirmation = true,
                        sync = true,
                        note = ""
                    )
                }

                delay(50)
                _uiState.update { it.copy(showSaveConfirmation = false, sync = false) }
                stopSelf()
            }
        }
    }
}

class OverlayViewModelFactory(
    private val appContainer: AppContainer,
    private val stopSelf: suspend () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OverlayViewModel(
                appContainer.categoryRepository,
                appContainer.transactionRepository,
                appContainer.settingsRepository,
                stopSelf
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
