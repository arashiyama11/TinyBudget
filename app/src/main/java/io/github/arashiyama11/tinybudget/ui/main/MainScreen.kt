package io.github.arashiyama11.tinybudget.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
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
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction as TransactionEntity
import io.github.arashiyama11.tinybudget.data.local.entity.Category as CategoryEntity
import io.github.arashiyama11.tinybudget.ui.component.Footer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

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
        val monthSummaries: Map<YearMonth, MonthlySummary?>,
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
        val scope = rememberCoroutineScope()

        val prefetchState by produceState(
            // 初期値
            initialValue = PrefetchState(emptyMap(), persistentListOf()),
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

                PrefetchState(summaries, currentTx)
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
        val summaries: Map<YearMonth, MonthlySummary?>,
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

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthlySummaryPager(
    baseYear: Int,
    baseMonth: Int,
    onMonthChanged: (delta: Int) -> Unit,
    summaryFor: (Int, Int) -> MonthlySummary?,
    modifier: Modifier = Modifier,
) {
    val pageCount = 240                       // ±10 年
    val centerPage = pageCount / 2
    val pagerState = rememberPagerState(      // pageCount はここで渡す
        initialPage = centerPage,
        pageCount = { pageCount }
    )
    val scope = rememberCoroutineScope()      // ← 追加

    /* ページが“落ち着いた”瞬間にだけ反応する */
    LaunchedEffect(pagerState.settledPage) {
        val delta = pagerState.settledPage - centerPage
        if (delta != 0) {
            onMonthChanged(delta)             // Presenter へ通知
            // 中央へスナップバック（scrollToPage は非アニメ）
            scope.launch { pagerState.scrollToPage(centerPage) }
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 16.dp,
        modifier = modifier,
    ) { page ->
        /* page → 年月変換 */
        val cal = remember(page, baseYear, baseMonth) {
            Calendar.getInstance().apply {
                set(baseYear, baseMonth - 1, 1)
                add(Calendar.MONTH, page - centerPage)
            }
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1

        /* カルーセル演出 */
        val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)

        MonthlySummaryCard(
            summary = summaryFor(y, m),
            year = y,
            month = m,
            onMonthChange = {},               // ← ボタン無効化
            modifier = Modifier.graphicsLayer {
                val scale = lerp(0.85f, 1f, 1f - pageOffset)
                val alpha = lerp(0.5f, 1f, 1f - pageOffset)
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
        )
    }
}


@Composable
private fun MonthlySummaryCard(
    summary: MonthlySummary?,
    year: Int,
    month: Int,
    modifier: Modifier = Modifier,
    onMonthChange: (Int) -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onMonthChange(-1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
                }
                Text(
                    text = "$year/$month",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { onMonthChange(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
                }
            }

            if (summary != null) {
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
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "No data for this month.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}


@Composable
private fun TransactionList(
    transactions: ImmutableList<Transaction>,
    eventSink: (MainScreen.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(transactions) { transaction ->
            TransactionListItem(
                transaction = transaction,
                onClick = {
                    eventSink(MainScreen.Event.OnClickTransaction(it))
                },
                onLongClick = {
                    eventSink(MainScreen.Event.ShowDeleteConfirmDialog(it))
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (Transaction) -> Unit,
    onLongClick: (Transaction) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick(transaction) },
                    onLongPress = { onLongClick(transaction) }
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier) {
                Text(text = transaction.category.name, style = MaterialTheme.typography.titleMedium)

                Text(
                    text = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (transaction.note.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = transaction.note, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(text = "${transaction.amount.value} JPY", style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun MainUi(state: MainScreen.State, modifier: Modifier) {
    val context = LocalContext.current

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

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, OverlayService::class.java)
                context.startForegroundService(intent)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "支出を記録する")
            }
        },
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
            MonthlySummaryPager(
                baseYear = state.currentYear,
                baseMonth = state.currentMonth,
                onMonthChanged = { delta ->
                    state.eventSink(MainScreen.Event.ChangeMonth(delta))
                },
                summaryFor = { y, m -> state.monthSummaries[YearMonth(y, m)] }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Transactions", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            TransactionList(transactions = state.transactions, eventSink = state.eventSink)
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    transaction: Transaction,
    onConfirm: (Transaction) -> Unit,
    onDismiss: (Transaction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss(transaction) },
        title = { Text("Delete Transaction") },
        text = { Text("Are you sure you want to delete this transaction?") },
        confirmButton = {
            Button(onClick = { onConfirm(transaction) }) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss(transaction) }) {
                Text("Cancel")
            }
        }
    )
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