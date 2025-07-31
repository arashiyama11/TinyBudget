package io.github.arashiyama11.tinybudget.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.Amount
import io.github.arashiyama11.tinybudget.Category
import io.github.arashiyama11.tinybudget.CategoryId
import io.github.arashiyama11.tinybudget.Transaction
import io.github.arashiyama11.tinybudget.toEntity
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class EditTransactionScreen(val transaction: Transaction) : Screen {
    data class State(
        val transaction: Transaction,
        val categories: List<Category>,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object GoBack : Event
        data class UpdateTransaction(val transaction: Transaction) : Event
        data class DeleteTransaction(val transaction: Transaction) : Event
    }
}

class EditTransactionPresenter(
    private val navigator: Navigator,
    private val screen: EditTransactionScreen,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : Presenter<EditTransactionScreen.State> {
    @Composable
    override fun present(): EditTransactionScreen.State {
        val scope = rememberStableCoroutineScope()
        val categories by categoryRepository.getAllCategories()
            .collectAsRetainedState(initial = emptyList())

        return EditTransactionScreen.State(
            transaction = screen.transaction,
            categories = categories.map { Category(CategoryId(it.id.toString()), it.name, true) }
        ) { event ->
            when (event) {
                is EditTransactionScreen.Event.GoBack -> navigator.pop()
                is EditTransactionScreen.Event.UpdateTransaction -> {
                    scope.launch {
                        transactionRepository.updateTransaction(event.transaction.toEntity())
                        navigator.pop()
                    }
                }

                is EditTransactionScreen.Event.DeleteTransaction -> {
                    scope.launch {
                        transactionRepository.deleteTransaction(event.transaction.toEntity())
                        navigator.pop()
                    }
                }
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return when (screen) {
                is EditTransactionScreen -> EditTransactionPresenter(
                    navigator,
                    screen,
                    transactionRepository,
                    categoryRepository
                )

                else -> null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionUi(state: EditTransactionScreen.State, modifier: Modifier) {
    var amount by remember { mutableStateOf(state.transaction.amount.value.toString()) }
    var selectedCategory by remember { mutableStateOf(state.transaction.category) }
    var note by remember { mutableStateOf(state.transaction.note) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("支出を編集") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(EditTransactionScreen.Event.GoBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金額") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // カテゴリ選択ドロップダウン
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory.name,
                    onValueChange = { },
                    label = { Text("カテゴリ") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        Box {
                            IconButton(onClick = { categoryDropdownExpanded = true }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "カテゴリを選択"
                                )
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                            ) {
                                state.categories.filter { it.isEnabled }.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            selectedCategory = category
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    if (interaction is PressInteraction.Press) {
                                        categoryDropdownExpanded = true
                                    }
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("メモ") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    val updatedTransaction = state.transaction.copy(
                        amount = Amount(amount.toLong()),
                        category = selectedCategory,
                        note = note
                    )
                    state.eventSink(EditTransactionScreen.Event.UpdateTransaction(updatedTransaction))
                }) {
                    Text("更新")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    state.eventSink(EditTransactionScreen.Event.DeleteTransaction(state.transaction))
                }) {
                    Text("削除")
                }
            }
        }
    }
}
