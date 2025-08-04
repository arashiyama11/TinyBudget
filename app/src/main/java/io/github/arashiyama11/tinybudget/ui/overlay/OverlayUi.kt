package io.github.arashiyama11.tinybudget.ui.overlay

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.tinybudget.ui.component.CategorySelector
import io.github.arashiyama11.tinybudget.ui.component.DialAmountInput
import io.github.arashiyama11.tinybudget.ui.component.NumericInputPad

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayUi(overlayViewModel: OverlayViewModel) {
    val uiState by overlayViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.showSaveConfirmation) {
        if (uiState.showSaveConfirmation) {
            Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
        }
    }

    Card(modifier = Modifier.padding(8.dp)) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = true),
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
                    DialAmountInput(
                        amount = uiState.amount,
                        step = uiState.amountStep,
                        sensitivity = uiState.sensitivity,
                        onAmountChange = overlayViewModel::onAmountChange,
                        onLongPress = overlayViewModel::onToggleNumericInputMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
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
