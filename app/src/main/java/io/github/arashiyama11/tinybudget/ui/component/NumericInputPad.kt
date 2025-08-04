package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumericInputPad(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var internalAmount by remember { mutableStateOf(amount.toString()) }

    LaunchedEffect(amount) {
        if (amount.toString() != internalAmount) {
            internalAmount = amount.toString()
        }
    }

    fun onNumberClick(number: Int) {
        val currentAmount = internalAmount
        internalAmount = if (currentAmount == "0") {
            number.toString()
        } else {
            (currentAmount + number.toString()).take(10)
        }
        onAmountChange(internalAmount.toLongOrNull() ?: 0L)
    }

    fun onBackspaceClick() {
        val currentAmount = internalAmount
        internalAmount = if (currentAmount.length > 1) {
            currentAmount.dropLast(1)
        } else {
            "0"
        }
        onAmountChange(internalAmount.toLongOrNull() ?: 0L)
    }

    fun onBackspaceLongClick() {
        internalAmount = "0"
        onAmountChange(0L)
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¥" + NumberFormat.getNumberInstance(Locale.JAPAN)
                .format(internalAmount.toLongOrNull() ?: 0L),
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onLongPress()
                        }
                    )
                }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { col ->
                        val number = (row - 1) * 3 + col
                        Button(
                            onClick = { onNumberClick(number) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(number.toString(), fontSize = 20.sp)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onBackspaceClick() },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onBackspaceLongClick() })
                        },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace")
                }
                Button(
                    onClick = { onNumberClick(0) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("0", fontSize = 20.sp)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm")
                }
            }
        }
    }
}
