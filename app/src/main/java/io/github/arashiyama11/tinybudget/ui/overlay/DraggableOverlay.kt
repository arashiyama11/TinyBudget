package io.github.arashiyama11.tinybudget.ui.overlay

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme
import kotlinx.coroutines.launch


@Composable
fun DraggableOverlay(
    modifier: Modifier,
    shape: Shape,
    windowManager: WindowManager,
    host: ComposeView,
    lp: WindowManager.LayoutParams,
    settingsRepository: SettingsRepository,
    initialPosition: Pair<Float, Float>,
    initialSize: Pair<Float, Float>,
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val screenW = remember { context.resources.displayMetrics.widthPixels.toFloat() }
    val screenH = remember { context.resources.displayMetrics.heightPixels.toFloat() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val minWidth = 64.dp
    val minHeight = 40.dp
    // サイズ上限を画面サイズの80%に設定
    val maxWidth = remember { with(density) { (screenW * 0.8f).toDp() } }
    val maxHeight = remember { with(density) { (screenH * 0.8f).toDp() } }

    // 事前読み込みした設定値を使用してstate初期化
    val initialValues = remember {
        with(density) {
            val constrainedWidth = initialSize.first.dp.coerceIn(minWidth, maxWidth)
            val constrainedHeight = initialSize.second.dp.coerceIn(minHeight, maxHeight)

            val offsetX = initialPosition.first.coerceIn(
                0f,
                (screenW - constrainedWidth.toPx()).coerceAtLeast(0f)
            )
            val offsetY = initialPosition.second.coerceIn(
                0f,
                (screenH - constrainedHeight.toPx()).coerceAtLeast(0f)
            )

            Triple(Pair(offsetX, offsetY), constrainedWidth, constrainedHeight)
        }
    }

    var offsetX by remember { mutableFloatStateOf(initialValues.first.first) }
    var offsetY by remember { mutableFloatStateOf(initialValues.first.second) }
    var size by remember { mutableStateOf(DpSize(initialValues.second, initialValues.third)) }

    // 初期値でWindowManagerのレイアウトを即座に更新
    LaunchedEffect(Unit) {
        with(density) {
            lp.apply {
                x = offsetX.toInt()
                y = offsetY.toInt()
                width = size.width.roundToPx()
                height = size.height.roundToPx()
            }
            windowManager.updateViewLayout(host, lp)
        }
    }

    TinyBudgetTheme {
        Surface(
            modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // ドラッグ終了時に位置を保存
                            coroutineScope.launch {
                                settingsRepository.setOverlayPosition(offsetX, offsetY)
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        offsetX = (offsetX + drag.x)
                            .coerceIn(0f, (screenW - size.width.toPx()))
                        offsetY = (offsetY + drag.y)
                            .coerceIn(0f, (screenH - size.height.toPx()))
                        lp.x = offsetX.toInt()
                        lp.y = offsetY.toInt()
                        windowManager.updateViewLayout(host, lp)
                    }
                },
            shape = shape
        ) {
            Box(Modifier.fillMaxSize()) {

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    content()
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onExit() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .align(Alignment.BottomStart)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // リサイズ終了時にサイズと位置を保存
                                    coroutineScope.launch {
                                        with(density) {
                                            settingsRepository.setOverlaySize(
                                                size.width.value,
                                                size.height.value
                                            )
                                            settingsRepository.setOverlayPosition(offsetX, offsetY)
                                        }
                                    }
                                }
                            ) { change, drag ->
                                change.consume()
                                with(density) {
                                    val dx = drag.x.toDp()
                                    val dy = drag.y.toDp()

                                    // 新しいサイズを計算（制限を適用）
                                    val newW = (size.width - dx).coerceIn(minWidth, maxWidth)
                                    val newH = (size.height + dy).coerceIn(minHeight, maxHeight)

                                    // 位置調整（左上を固定点として、サイズ変更時の位置補正）
                                    val widthDiff = size.width - newW
                                    val newOffsetX = (offsetX + widthDiff.toPx()).coerceIn(
                                        0f,
                                        (screenW - newW.toPx()).coerceAtLeast(0f)
                                    )

                                    // 状態を更新
                                    offsetX = newOffsetX
                                    size = DpSize(newW, newH)

                                    // WindowManagerのレイアウトパラメータを更新
                                    lp.apply {
                                        width = newW.roundToPx()
                                        height = newH.roundToPx()
                                        x = newOffsetX.toInt()
                                    }
                                    windowManager.updateViewLayout(host, lp)
                                }
                            }
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(topEnd = 4.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Resize",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}