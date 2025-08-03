package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*

@Composable
fun NumericInputPad(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var internalAmount by remember { mutableStateOf(amount.toString()) }

    fun onNumberClick(number: Int) {
        internalAmount = if (internalAmount == "0") {
            number.toString()
        } else {
            internalAmount + number.toString()
        }
        onAmountChange(internalAmount.toLong())
    }

    fun onClearClick() {
        internalAmount = "0"
        onAmountChange(0L)
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¥$internalAmount",
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 上３行：数字ボタン 1–9
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
                            Text(number.toString())
                        }
                    }
                }
            }
            // 下段：クリア・0・確定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onClearClick() },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentPadding = PaddingValues(0.dp)

                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
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