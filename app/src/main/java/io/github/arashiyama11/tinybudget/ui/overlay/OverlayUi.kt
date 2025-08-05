package io.github.arashiyama11.tinybudget.ui.overlay

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.component.CategorySelector
import io.github.arashiyama11.tinybudget.ui.component.DialAmountInput
import io.github.arashiyama11.tinybudget.ui.component.NumericInputPad
import io.github.arashiyama11.tinybudget.ui.theme.PreviewOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.annotations.TestOnly

private const val DIAL_MIN_SIZE_DP = 64

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayUi(overlayViewModel: OverlayViewModel) {
    val uiState by overlayViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var height by remember { mutableIntStateOf(200) }

    LaunchedEffect(uiState.showSaveConfirmation) {
        if (uiState.showSaveConfirmation) {
            Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .onGloballyPositioned {
                height = it.size.height
            }) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (uiState.isNumericInputMode) {
                    NumericInputPad(
                        amount = uiState.amount,
                        onAmountChange = overlayViewModel::onAmountChange,
                        onConfirm = overlayViewModel::onToggleNumericInputMode,
                        onLongPress = overlayViewModel::onToggleNumericInputMode,
                    )
                } else {
                    val density = LocalDensity.current

                    var dialHeight by remember { mutableIntStateOf(200) }
                    val dialHeightInDp =
                        remember(dialHeight) { with(density) { dialHeight.toDp() } }
                    // ダイアルを十分に表示できる最小のオーバーレイの高さ
                    var criticalHeight: Int? by remember { mutableStateOf(null) }


                    LaunchedEffect(dialHeight) {
                        if (0 < dialHeight && dialHeightInDp < DIAL_MIN_SIZE_DP.dp && (criticalHeight == null || height > criticalHeight!!)) {
                            criticalHeight = height
                        }
                    }

                    LaunchedEffect(criticalHeight, height) {
                        if (criticalHeight != null && height > criticalHeight!!) {
                            criticalHeight = null
                        }
                    }

                    DialAmountInput(
                        amount = uiState.amount,
                        step = uiState.amountStep,
                        sensitivity = uiState.sensitivity,
                        onAmountChange = overlayViewModel::onAmountChange,
                        onLongPress = overlayViewModel::onToggleNumericInputMode,
                        modifier = Modifier.onGloballyPositioned {
                            dialHeight = it.size.height
                        }.fillMaxWidth().run {
                            if (criticalHeight != null) {
                                height(DIAL_MIN_SIZE_DP.dp)
                            } else {
                                weight(1f, fill = true)
                            }
                        },
                        fontSize = 20.sp,
                        sync = uiState.sync
                    )
                }

                CategorySelector(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = overlayViewModel::onCategorySelected,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = uiState.note,
                    onValueChange = overlayViewModel::onNoteChange,

                    label = {
                        Text(
                            "メモ (任意)",
                            fontSize = 14.sp,
                            softWrap = true,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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


@TestOnly
@Preview
@Composable
private fun PreviewOverlayUi() {


    val overlayViewModel = remember {
        OverlayViewModel(
            categoryRepository = mockCategoryRepository,
            transactionRepository = mockTransactionRepository,
            settingsRepository = mockSettingsRepository,
            stopSelf = {}
        )
    }

    PreviewOf {
        Box(
            modifier = Modifier.size(200.dp, 200.dp)
        ) {
            OverlayUi(overlayViewModel = overlayViewModel)
        }
    }
}

@TestOnly
private val mockCategoryRepository = object : CategoryRepository {
    private val category = Category(id = 0, name = "食費", isEnabled = true)
    override fun getAllCategories(): Flow<List<Category>> {
        return flowOf(listOf(category))
    }

    override fun getEnabledCategories(): Flow<List<Category>> {
        return flowOf(listOf(category))

    }

    override suspend fun getCategory(id: Int): Category? {
        return if (id == category.id) category else null
    }

    override suspend fun addCategory(category: Category) {
    }

    override suspend fun updateCategory(category: Category) {
    }

    override suspend fun deleteCategory(category: Category) {
    }

}

@TestOnly
private val mockTransactionRepository = object : TransactionRepository {
    override fun getTransactionsByMonth(
        year: Int,
        month: Int
    ): Flow<List<Transaction>> {
        TODO("Not yet implemented")
    }

    override suspend fun addTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

}


@TestOnly
private val mockSettingsRepository = object : SettingsRepository {
    override val defaultCategoryId: Flow<Int?>
        get() = MutableStateFlow(0)
    override val lastCategoryId: Flow<Int?>
        get() = MutableStateFlow(0)
    override val isLastModeNumeric: Flow<Boolean>
        get() = MutableStateFlow(false)
    override val overlayPosition: Flow<Pair<Float, Float>>
        get() = MutableStateFlow(0f to 0f)
    override val overlaySize: Flow<Pair<Float, Float>>
        get() = MutableStateFlow(100f to 200f)
    override val overlayDestroyedAt: Flow<Long?>
        get() = MutableStateFlow(0L)
    override val triggerApps: Flow<Set<String>>
        get() = MutableStateFlow(emptySet())
    override val amountStep: Flow<Long>
        get() = MutableStateFlow(10)
    override val sensitivity: Flow<Float>
        get() = MutableStateFlow(1f)

    override suspend fun setDefaultCategoryId(id: Int) {
        (defaultCategoryId as MutableStateFlow<Int?>).value = id
    }

    override suspend fun setLastCategoryId(id: Int) {
        (lastCategoryId as MutableStateFlow<Int?>).value = id
    }

    override suspend fun setLastModeNumeric(isNumeric: Boolean) {
        (isLastModeNumeric as MutableStateFlow<Boolean>).value = isNumeric
    }

    override suspend fun setOverlayPosition(x: Float, y: Float) {
        (overlayPosition as MutableStateFlow<Pair<Float, Float>>).value = Pair(x, y)
    }

    override suspend fun setOverlaySize(width: Float, height: Float) {
        (overlaySize as MutableStateFlow<Pair<Float, Float>>).value = Pair(width, height)
    }

    override suspend fun setOverlayDestroyedAt(time: Long) {
        (overlayDestroyedAt as MutableStateFlow<Long?>).value = time
    }

    override suspend fun resetOverlayPositionAndSize() {
        (overlayPosition as MutableStateFlow<Pair<Float, Float>>).value = Pair(0f, 0f)
        (overlaySize as MutableStateFlow<Pair<Float, Float>>).value = Pair(300f, 400f)
    }

    override suspend fun setTriggerApps(packageNames: Set<String>) {
        (triggerApps as MutableStateFlow<Set<String>>).value = packageNames
    }

    override suspend fun setAmountStep(step: Long) {
        (amountStep as MutableStateFlow<Long>).value = step
    }

    override suspend fun setSensitivity(multiplier: Float) {
        (sensitivity as MutableStateFlow<Float>).value = multiplier
    }
}

