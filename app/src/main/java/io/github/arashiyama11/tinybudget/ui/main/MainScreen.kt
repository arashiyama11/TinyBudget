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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startForegroundService
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.Amount
import io.github.arashiyama11.tinybudget.Category
import io.github.arashiyama11.tinybudget.CategoryId
import io.github.arashiyama11.tinybudget.MonthlySummary
import io.github.arashiyama11.tinybudget.OverlayService
import io.github.arashiyama11.tinybudget.Transaction
import io.github.arashiyama11.tinybudget.TransactionId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
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

    }
}

class MainPresenter() : Presenter<MainScreen.State> {
    private val mockTransactions = persistentListOf(
        Transaction(
            id = TransactionId("1"),
            amount = Amount(1500),
            date = 1696118400000, // Example timestamp
            category = Category(
                id = CategoryId("1"),
                name = "Food"
            ),
            note = "Lunch at restaurant"
        ),
        Transaction(
            id = TransactionId("2"),
            amount = Amount(8500),
            date = 1696204800000, // Example timestamp
            category = Category(
                id = CategoryId("2"),
                name = "Other Expenses"
            ),
            note = "Grocery shopping"
        )
    )

    private val mockCategoryWiseAmounts = persistentMapOf(
        Category(
            id = CategoryId("1"),
            name = "Food"
        ) to Amount(1500),
        Category(
            id = CategoryId("2"),
            name = "Other Expenses"
        ) to Amount(8500)
    )
    private val mockMonthlySummary = MonthlySummary(
        year = 2023,
        month = 10,
        totalAmount = Amount(10000),
        categoryWiseAmounts = mockCategoryWiseAmounts,
        transactions = mockTransactions
    )

    @Composable
    override fun present(): MainScreen.State {
        val transactions by remember { mutableStateOf(mockTransactions) }
        val monthlySummaries by remember {
            mutableStateOf(
                mockMonthlySummary
            )
        }
        return MainScreen.State(
            transactions = transactions,
            monthlySummaries = monthlySummaries,
        ) { event ->
        }
    }

    class Factory : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: com.slack.circuit.runtime.Navigator,
            context: com.slack.circuit.runtime.CircuitContext
        ): Presenter<*>? {
            return if (screen is MainScreen) {
                MainPresenter()
            } else {
                null
            }
        }
    }
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
            Divider()
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
        modifier = modifier
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