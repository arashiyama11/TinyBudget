package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.tinybudget.Transaction
import io.github.arashiyama11.tinybudget.ui.main.MainScreen
import io.github.arashiyama11.tinybudget.util.toJPYString
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionList(
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
fun TransactionListItem(
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

        Text(
            text = transaction.amount.value.toJPYString(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
