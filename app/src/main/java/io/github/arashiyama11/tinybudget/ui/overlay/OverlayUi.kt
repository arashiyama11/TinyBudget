package io.github.arashiyama11.tinybudget.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun OverlayUi(overlayViewModel: OverlayViewModel) {
    val uiState by overlayViewModel.uiState.collectAsState()

    Card(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DialAmountInput(
                amount = uiState.amount,
                step = overlayViewModel.amountStep,
                onAmountChange = overlayViewModel::onAmountChange
            )

            CategorySelector(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = overlayViewModel::onCategorySelected
            )

            TextField(
                value = uiState.note,
                onValueChange = overlayViewModel::onNoteChange,
                label = { Text("メモ (任意)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = overlayViewModel::close) {
                    Text("閉じる")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = overlayViewModel::saveTransaction,
                    enabled = uiState.amount > 0 && uiState.selectedCategoryId != null
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun DialAmountInput(
    amount: Long,
    step: Long,
    onAmountChange: (Long) -> Unit
) {
    // Animatable と VelocityTracker の準備
    val animatable = remember { Animatable(amount.toFloat()) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()

    // 表示用フォーマット
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale.JAPAN) }
    val dragSensitivity = 40f

    // 慣性スクロールの減速を早める
    val decay = exponentialDecay<Float>(frictionMultiplier = 1f)

    // amount 外部変更を即同期
    LaunchedEffect(amount) {
        animatable.snapTo(amount.toFloat())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(amount) {
                detectDragGestures(
                    onDragStart = { _: Offset ->
                        // 前のアニメを止め、速度トラッカーをリセット
                        scope.launch { animatable.stop() }
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change: PointerInputChange, dragAmt: Offset ->
                        change.consume()
                        // ドラッグ速度を記録
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        // ドラッグ分を value に反映
                        val delta = (dragAmt.y / dragSensitivity) * step
                        scope.launch {
                            val next = (animatable.value - delta).coerceAtLeast(0f)
                            animatable.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        // ドラッグ終了時の速度取得
                        val velocityY = velocityTracker.calculateVelocity().y
                        val initialVelocity = -velocityY / dragSensitivity * step

                        // 慣性アニメ
                        scope.launch {
                            animatable.animateDecay(initialVelocity, decay)
                            // 最後にステップ単位で丸め
                            val stepCount = (animatable.value / step)
                                .roundToLong()
                                .coerceAtLeast(0L)
                            val finalAmt = stepCount * step
                            animatable.snapTo(finalAmt.toFloat())
                            onAmountChange(finalAmt)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 常に step に丸めて表示
        val displayStepCount = (animatable.value / step)
            .roundToLong()
            .coerceAtLeast(0L)
        val displayValue = displayStepCount * step
        Text(
            text = numberFormat.format(displayValue),
            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<io.github.arashiyama11.tinybudget.data.local.entity.Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("カテゴリ") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
