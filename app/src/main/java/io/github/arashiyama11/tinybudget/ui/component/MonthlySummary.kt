package io.github.arashiyama11.tinybudget.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import io.github.arashiyama11.tinybudget.MonthlySummary
import io.github.arashiyama11.tinybudget.util.toJPYString
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlySummaryPager(
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
fun MonthlySummaryCard(
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
                    text = "Total: ${summary.totalAmount.value.toJPYString()}",
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
                        Text(text = amount.value.toJPYString())
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "No data for this month.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
