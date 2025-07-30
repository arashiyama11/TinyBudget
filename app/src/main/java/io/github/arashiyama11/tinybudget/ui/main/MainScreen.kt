package io.github.arashiyama11.tinybudget.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import androidx.compose.runtime.produceState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.Amount
import io.github.arashiyama11.tinybudget.Category
import io.github.arashiyama11.tinybudget.CategoryId
import io.github.arashiyama11.tinybudget.MonthlySummary
import io.github.arashiyama11.tinybudget.OverlayService
import io.github.arashiyama11.tinybudget.Transaction
import io.github.arashiyama11.tinybudget.TransactionId
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.component.Footer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.combine
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Parcelize
data object MainScreen : Screen {
    data class State(
        val transactions: ImmutableList<Transaction>,
        val monthlySummaries: MonthlySummary,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class NavigateTo(val screen: Screen) : Event
    }
}

class MainPresenter(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val navigator: Navigator
) : Presenter<MainScreen.State> {
    @Composable
    override fun present(): MainScreen.State {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        val transactions by produceState<ImmutableList<Transaction>>(initialValue = persistentListOf()) {
            val categoriesFlow = categoryRepository.getAllCategories()
            val transactionsFlow = transactionRepository.getTransactionsByMonth(year, month)

            combine(transactionsFlow, categoriesFlow) { transactions, categories ->
                val categoryMap = categories.associateBy { it.id }
                transactions.map { transaction ->
                    transaction.toUiModel(categoryMap[transaction.categoryId])
                }.toImmutableList()
            }.collect { value = it }
        }

        val monthlySummaries = transactions.toMonthlySummary(year, month)

        return MainScreen.State(
            transactions = transactions,
            monthlySummaries = monthlySummaries,
        ) { event ->
            when (event) {
                is MainScreen.Event.NavigateTo -> {
                    navigator.goTo(event.screen)
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
            context: com.slack.circuit.runtime.CircuitContext
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

private fun io.github.arashiyama11.tinybudget.data.local.entity.Transaction.toUiModel(
    category: io.github.arashiyama11.tinybudget.data.local.entity.Category?
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

@Composable
private fun MonthlySummaryCard(summary: MonthlySummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${summary.year}/${summary.month}",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: ${summary.totalAmount.value} JPY",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "By Category:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            summary.categoryWiseAmounts.forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = category.name)
                    Text(text = "${amount.value} JPY")
                }
            }
        }
    }
}

@Composable
private fun TransactionList(
    transactions: ImmutableList<Transaction>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(transactions) { transaction ->
            TransactionListItem(transaction = transaction)
            HorizontalDivider()
        }
    }
}

@Composable
private fun TransactionListItem(transaction: Transaction, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.category.name, style = MaterialTheme.typography.titleMedium)
            if (transaction.note.isNotEmpty()) {
                Text(text = transaction.note, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date(transaction.date)),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(text = "${transaction.amount.value} JPY", style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun MainUi(state: MainScreen.State, modifier: Modifier) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Footer(
                currentScreen = MainScreen, navigate = {
                    state.eventSink(MainScreen.Event.NavigateTo(it))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            MonthlySummaryCard(summary = state.monthlySummaries)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Transactions", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            TransactionList(transactions = state.transactions)

            val context = LocalContext.current

            Button({
                val intent = Intent(context, OverlayService::class.java)
                context.startForegroundService(intent)
            }) {
                Text("launch overlay")
            }
        }
    }
}