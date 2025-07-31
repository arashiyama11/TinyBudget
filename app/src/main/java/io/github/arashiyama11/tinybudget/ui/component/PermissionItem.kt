package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PermissionItem(name: String, isGranted: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            Text(
                if (isGranted) "許可済み" else "未許可",
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clickable(enabled = !isGranted, onClick = onClick)
    )
}
