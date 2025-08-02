package io.github.arashiyama11.tinybudget.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import io.github.arashiyama11.tinybudget.ui.theme.AppTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DialAmountInput(
    amount: Long,
    step: Long,
    frictionMultiplier: Float,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    sync: Boolean = false
) {
    var isDragging by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp)

    val animatable = remember { Animatable(amount.toFloat()) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()
    val decay = exponentialDecay<Float>(frictionMultiplier = frictionMultiplier)

    // 外部 amount が変わったら即同期
    LaunchedEffect(amount) {
        animatable
        if (sync) {
            animatable.snapTo(amount.toFloat())
        }
    }

    LaunchedEffect(Unit) {
        animatable.asStateFlow(scope).collect { onAmountChange(it.roundToLong()) }
    }

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
fun CategorySelector(
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
        AppTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("カテゴリ", fontSize = 14.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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

fun <T, V : AnimationVector> Animatable<T, V>.asStateFlow(
    scope: CoroutineScope
) = snapshotFlow { this.value }
    .distinctUntilChanged()
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = this.value
    )
