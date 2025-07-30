package io.github.arashiyama11.tinybudget.ui.overlay

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun OverlayUi(overlayViewModel: OverlayViewModel) {
    val uiState by overlayViewModel.uiState.collectAsState()

    Card(modifier = Modifier.padding(8.dp)) {
        // Card 内を高さいっぱいに使う
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // 上側のコンテンツをスクロール可能＆余った高さを占有
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DialAmountInput(
                    amount = uiState.amount,
                    step = overlayViewModel.amountStep,
                    onAmountChange = overlayViewModel::onAmountChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    fontSize = 20.sp
                )

                CategorySelector(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = overlayViewModel::onCategorySelected,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = uiState.note,
                    onValueChange = overlayViewModel::onNoteChange,
                    label = { Text("メモ (任意)", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ここは weight を使わないので、必ず見える
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = overlayViewModel::saveTransaction,
                    enabled = uiState.amount > 0 && uiState.selectedCategoryId != null,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("保存", fontSize = 14.sp)
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")

@Composable
private fun DialAmountInput(
    amount: Long,
    step: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp
) {
    var isDragging by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp)

    val animatable = remember { Animatable(amount.toFloat()) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()
    val decay = exponentialDecay<Float>(frictionMultiplier = 1f)

    // 外部 amount が変わったら即同期


    Surface(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    scope.launch { animatable.stop() }
                    velocityTracker.resetTracking()
                },
                onDrag = { change, dragAmt ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    val delta = (dragAmt.y / 40f) * step
                    scope.launch {
                        val next = (animatable.value - delta).coerceAtLeast(0f)
                        animatable.snapTo(next)
                        // ← ここでリアルタイムに ViewModel 側の amount を更新
                        val interimStep = (next / step)
                            .roundToLong()
                            .coerceAtLeast(0L) * step
                        onAmountChange(interimStep)
                    }
                },
                onDragEnd = {
                    isDragging = false
                    val velocityY = velocityTracker.calculateVelocity().y
                    val initialVelocity = -velocityY / 40f * step
                    scope.launch {
                        animatable.animateDecay(initialVelocity, decay)
                        val finalStep = (animatable.value / step)
                            .roundToLong()
                            .coerceAtLeast(0L)
                        val finalAmt = finalStep * step
                        animatable.snapTo(finalAmt.toFloat())
                        onAmountChange(finalAmt)
                    }
                },
                onDragCancel = {
                    isDragging = false
                }
            )
        },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = elevation,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            val dynamicFont = if (maxHeight < 60.dp) fontSize * 0.8f else fontSize
            val displayValue = (animatable.value / step)
                .roundToLong()
                .coerceAtLeast(0L) * step

            Text(
                text = NumberFormat.getCurrencyInstance(Locale.JAPAN)
                    .format(displayValue),
                style = TextStyle(
                    fontSize = dynamicFont,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("カテゴリ", fontSize = 14.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .heightIn(min = 48.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name, fontSize = 14.sp) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
