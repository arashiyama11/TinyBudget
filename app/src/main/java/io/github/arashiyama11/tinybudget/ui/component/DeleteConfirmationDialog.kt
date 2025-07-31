package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.arashiyama11.tinybudget.Transaction

@Composable
fun DeleteConfirmationDialog(
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
