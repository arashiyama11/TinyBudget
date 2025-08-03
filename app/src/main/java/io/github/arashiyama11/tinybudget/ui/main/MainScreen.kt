package io.github.arashiyama11.tinybudget.ui.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.retained.rememberRetained
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
import io.github.arashiyama11.tinybudget.MonthlySummary
import io.github.arashiyama11.tinybudget.Transaction
import io.github.arashiyama11.tinybudget.TransactionId
import io.github.arashiyama11.tinybudget.data.local.entity.Category as CategoryEntity
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction as TransactionEntity
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.component.DeleteConfirmationDialog
import io.github.arashiyama11.tinybudget.ui.component.MonthlySummaryPager
import io.github.arashiyama11.tinybudget.ui.component.TransactionList
import io.github.arashiyama11.tinybudget.ui.component.TransactionListItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Calendar

/** 月ズレ計算を Calendar に頼らず型安全に扱うための小さな値オブジェクト */
data class YearMonth(val year: Int, val month: Int) {
    /** delta (±n) か月ずらした YearMonth を返す */
    fun shifted(delta: Int): YearMonth {
        val c = Calendar.getInstance().apply { set(year, month - 1, 1); add(Calendar.MONTH, delta) }
        return YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }
}


@Parcelize
data object MainScreen : Screen {
    data class State(
        val monthSummaries: ImmutableMap<YearMonth, MonthlySummary?>,
        val monthlySummary: MonthlySummary?,
        val transactions: ImmutableList<Transaction>,
        val currentYear: Int,
        val currentMonth: Int,
        val transactionToDelete: Transaction? = null,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class NavigateTo(val screen: Screen) : Event
        data class OnClickTransaction(val transaction: Transaction) : Event
        data class ShowDeleteConfirmDialog(val transaction: Transaction) : Event
        data class HideDeleteConfirmDialog(val transaction: Transaction) : Event
        data class DeleteTransaction(val transaction: Transaction) : Event
        data class ChangeMonth(val delta: Int) : Event
    }
}

class MainPresenter(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val navigator: Navigator
) : Presenter<MainScreen.State> {

    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun present(): MainScreen.State {
        val today = rememberRetained { Calendar.getInstance() }
        var currentYm by rememberRetained {
            mutableStateOf(YearMonth(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1))
        }
        var transactionToDelete by rememberRetained { mutableStateOf<Transaction?>(null) }
        val scope = rememberStableCoroutineScope()

        val prefetchState by produceRetainedState(
            // 初期値
            initialValue = PrefetchState(persistentMapOf(), persistentListOf()),
            key1 = currentYm
        ) {
            val ymPrev = currentYm.shifted(-1)
            val ymNext = currentYm.shifted(+1)

            val categoriesFlow = categoryRepository.getAllCategories()

            val flows = listOf(ymPrev, currentYm, ymNext).associateWith { ym ->
                transactionRepository.getTransactionsByMonth(ym.year, ym.month)
            }

            combine(
                *(flows.values + categoriesFlow).toTypedArray()
            ) { results ->
                val categories = results.last() as List<CategoryEntity>
                val categoryMap = categories.associateBy { it.id }

                val summaries = mutableMapOf<YearMonth, MonthlySummary?>()
                var currentTx: ImmutableList<Transaction> = persistentListOf()

                flows.keys.forEachIndexed { index, ym ->
                    val entities = results[index] as List<TransactionEntity>
                    val uiTx = entities.map { it.toUiModel(categoryMap[it.categoryId]) }

                    summaries[ym] = uiTx.toMonthlySummary(ym.year, ym.month)
                    if (ym == currentYm) currentTx = uiTx.toImmutableList()
                }

                PrefetchState(summaries.toImmutableMap(), currentTx)
            }.collect { value = it }
        }

        /* ───────── State へマッピング ───────── */
        return MainScreen.State(
            monthSummaries = prefetchState.summaries,
            transactions = prefetchState.currentTx,
            currentYear = currentYm.year,
            currentMonth = currentYm.month,
            transactionToDelete = transactionToDelete,
            monthlySummary = prefetchState.summaries[currentYm],
        ) { event ->
            when (event) {
                is MainScreen.Event.ChangeMonth -> {
                    currentYm = currentYm.shifted(event.delta)
                }

                is MainScreen.Event.NavigateTo -> {
                    navigator.goTo(event.screen)
                }

                is MainScreen.Event.OnClickTransaction -> {
                    navigator.goTo(EditTransactionScreen(event.transaction))
                }

                is MainScreen.Event.ShowDeleteConfirmDialog -> {
                    transactionToDelete = event.transaction
                }

                is MainScreen.Event.HideDeleteConfirmDialog -> {
                    transactionToDelete = null
                }

                is MainScreen.Event.DeleteTransaction -> {
                    scope.launch {
                        transactionRepository.deleteTransaction(event.transaction.toEntity())
                        transactionToDelete = null
                    }
                }
            }
        }
    }


    /* 内部だけで使う一時データ */
    private data class PrefetchState(
        val summaries: ImmutableMap<YearMonth, MonthlySummary?>,
        val currentTx: ImmutableList<Transaction>
    )

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is MainScreen) {
                MainPresenter(transactionRepository, categoryRepository, navigator)
            } else {
                null
            }
        }
    }
}

private fun List<Transaction>.toMonthlySummary(
    year: Int,
    month: Int
): MonthlySummary {
    val totalAmount = Amount(sumOf { it.amount.value })
    val categoryWiseAmounts = this
        .groupBy { it.category }
        .mapValues { (_, transactions) ->
            Amount(transactions.sumOf { it.amount.value })
        }
        .toImmutableMap()
    return MonthlySummary(
        year = year,
        month = month,
        totalAmount = totalAmount,
        categoryWiseAmounts = categoryWiseAmounts,
        transactions = this.toImmutableList()
    )
}

private fun TransactionEntity.toUiModel(
    category: CategoryEntity?
): Transaction {
    return Transaction(
        id = TransactionId(id.toString()),
        amount = Amount(amount),
        date = timestamp,
        category = Category(
            id = CategoryId(categoryId.toString()),
            name = category?.name ?: "Unknown"
        ),
        note = note
    )
}

private fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.id.value.toInt(),
        amount = this.amount.value,
        categoryId = this.category.id.value.toInt(),
        note = this.note,
        timestamp = this.date
    )
}

@Composable
fun MainUi(state: MainScreen.State, modifier: Modifier) {
    if (state.transactionToDelete != null) {
        DeleteConfirmationDialog(
            transaction = state.transactionToDelete,
            onConfirm = {
                state.eventSink(MainScreen.Event.DeleteTransaction(it))
            },
            onDismiss = {
                state.eventSink(MainScreen.Event.HideDeleteConfirmDialog(it))
            }
        )
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        MonthlySummaryPager(
            baseYear = state.currentYear,
            baseMonth = state.currentMonth,
            onMonthChanged = { delta ->
                state.eventSink(MainScreen.Event.ChangeMonth(delta))
            },
            summaryFor = { y, m -> state.monthSummaries[YearMonth(y, m)] },
            modifier = Modifier.animateContentSize()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Transactions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        TransactionList(transactions = state.transactions, eventSink = state.eventSink)
    }
}

@Composable
@Preview
fun TransactionItemPreview() {
    val transaction = Transaction(
        id = TransactionId("1"),
        amount = Amount(1000),
        date = System.currentTimeMillis(),
        category = Category(id = CategoryId("1"), name = "Food"),
        note = "メモだよー"
    )
    TransactionListItem(
        transaction = transaction,
        onClick = {},
        onLongClick = {}
    )
}