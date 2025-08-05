package io.github.arashiyama11.tinybudget.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.ui.component.AddCategoryDialog
import io.github.arashiyama11.tinybudget.ui.component.EditCategoryDialog
import kotlinx.parcelize.Parcelize
import io.github.arashiyama11.tinybudget.ui.component.PermissionItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale


@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val categories: ImmutableList<Category>,
        val showAddCategoryDialog: Boolean,
        val editingCategory: Category?,
        val permissionStatus: ImmutableMap<String, Boolean>,
        val amountStep: Long,
        val sensitivity: Float,
        val frictionMultiplier: Float,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class NavigateTo(val screen: Screen) : Event
        data class OnCategoryEnabledChanged(val category: Category, val isEnabled: Boolean) : Event
        data object ShowAddCategoryDialog : Event
        data object HideAddCategoryDialog : Event
        data class AddCategory(val name: String) : Event
        data class ShowEditCategoryDialog(val category: Category) : Event
        data object HideEditCategoryDialog : Event
        data class UpdateCategory(val category: Category) : Event
        data class DeleteCategory(val category: Category) : Event
        data object OnResetOverlaySettingsClicked : Event
        data class OnRequestPermission(val permission: String) : Event
        data class UpdateAmountStep(val step: Long) : Event
        data class UpdateSensitivity(val sensitivity: Float) : Event
        data class UpdateFrictionMultiplier(val frictionMultiplier: Float) : Event
    }
}

class SettingsPresenter(
    private val navigator: Navigator,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionManager: PermissionManager
) : Presenter<SettingsScreen.State> {

    @Composable
    override fun present(): SettingsScreen.State {
        val scope = rememberStableCoroutineScope()
        val categories by categoryRepository.getAllCategories().map { it.toImmutableList() }
            .collectAsRetainedState(initial = persistentListOf())
        var showAddCategoryDialog by rememberRetained { mutableStateOf(false) }
        var editingCategory by rememberRetained { mutableStateOf<Category?>(null) }
        val permissionStatus by rememberRetained { mutableStateOf(getPermissionStatus()) }
        val amountStep by settingsRepository.amountStep.collectAsRetainedState(initial = 10L)
        val sensitivity by settingsRepository.sensitivity
            .collectAsRetainedState(
                initial = 1f
            )
        val frictionMultiplier by settingsRepository.frictionMultiplier.collectAsRetainedState(
            initial = 1f
        )

        return SettingsScreen.State(
            categories = categories,
            showAddCategoryDialog = showAddCategoryDialog,
            editingCategory = editingCategory,
            permissionStatus = permissionStatus,
            amountStep = amountStep,
            sensitivity = sensitivity,
            frictionMultiplier = frictionMultiplier
        ) { event ->
            when (event) {
                is SettingsScreen.Event.NavigateTo -> navigator.goTo(event.screen)
                is SettingsScreen.Event.OnCategoryEnabledChanged -> {
                    scope.launch {
                        categoryRepository.updateCategory(event.category.copy(isEnabled = event.isEnabled))
                    }
                }

                is SettingsScreen.Event.ShowAddCategoryDialog -> showAddCategoryDialog = true
                is SettingsScreen.Event.HideAddCategoryDialog -> showAddCategoryDialog = false
                is SettingsScreen.Event.AddCategory -> {
                    scope.launch {
                        categoryRepository.addCategory(Category(name = event.name))
                        showAddCategoryDialog = false
                    }
                }

                is SettingsScreen.Event.ShowEditCategoryDialog -> editingCategory = event.category
                is SettingsScreen.Event.HideEditCategoryDialog -> editingCategory = null
                is SettingsScreen.Event.UpdateCategory -> {
                    scope.launch {
                        categoryRepository.updateCategory(event.category)
                        editingCategory = null
                    }
                }

                is SettingsScreen.Event.DeleteCategory -> {
                    scope.launch {
                        categoryRepository.deleteCategory(event.category)
                        editingCategory = null
                    }
                }

                is SettingsScreen.Event.OnResetOverlaySettingsClicked -> {
                    scope.launch {
                        settingsRepository.resetOverlayPositionAndSize()
                    }
                }

                is SettingsScreen.Event.OnRequestPermission -> {
                    scope.launch {
                        when (event.permission) {
                            "overlay" -> permissionManager.requestOverlayPermission()
                            "accessibility" -> permissionManager.requestAccessibilityPermission()
                            "notification" -> permissionManager.requestNotificationPermission()
                        }
                    }
                }

                is SettingsScreen.Event.UpdateAmountStep -> {
                    scope.launch {
                        settingsRepository.setAmountStep(event.step)
                    }
                }

                is SettingsScreen.Event.UpdateSensitivity -> {
                    scope.launch {
                        settingsRepository.setSensitivity(event.sensitivity)
                    }
                }

                is SettingsScreen.Event.UpdateFrictionMultiplier -> {
                    scope.launch {
                        settingsRepository.setFrictionMultiplier(event.frictionMultiplier)
                    }
                }
            }
        }
    }

    private fun getPermissionStatus(): ImmutableMap<String, Boolean> {
        return persistentMapOf(
            "overlay" to permissionManager.checkOverlayPermission(),
            "accessibility" to permissionManager.checkAccessibilityPermission(),
            "notification" to permissionManager.checkNotificationPermission()
        )
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository,
        private val permissionManager: PermissionManager
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is SettingsScreen) {
                SettingsPresenter(
                    navigator,
                    categoryRepository,
                    settingsRepository,
                    permissionManager
                )
            } else {
                null
            }
        }
    }
}


@Composable
fun SettingsUi(state: SettingsScreen.State, modifier: Modifier) {
    if (state.showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { state.eventSink(SettingsScreen.Event.HideAddCategoryDialog) },
            onConfirm = { name -> state.eventSink(SettingsScreen.Event.AddCategory(name)) }
        )
    }

    state.editingCategory?.let {
        EditCategoryDialog(
            category = it,
            onDismiss = { state.eventSink(SettingsScreen.Event.HideEditCategoryDialog) },
            onConfirm = { category -> state.eventSink(SettingsScreen.Event.UpdateCategory(category)) },
            onDelete = { category -> state.eventSink(SettingsScreen.Event.DeleteCategory(category)) }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "カテゴリ管理", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { state.eventSink(SettingsScreen.Event.ShowAddCategoryDialog) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                    state.categories.forEach { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.clickable {
                                state.eventSink(
                                    SettingsScreen.Event.ShowEditCategoryDialog(
                                        category
                                    )
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = category.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        state.eventSink(
                                            SettingsScreen.Event.OnCategoryEnabledChanged(
                                                category,
                                                isEnabled
                                            )
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
        }

        item {
            Card(modifier = Modifier.padding(16.dp)) {

                ListItem(
                    headlineContent = { Text("トリガーアプリ設定") },
                    modifier = Modifier
                        .clickable {
                            state.eventSink(
                                SettingsScreen.Event.NavigateTo(
                                    TriggerAppsScreen
                                )
                            )
                        }
                        .padding(16.dp)
                )
            }

            HorizontalDivider()
        }

        item {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "権限設定", style = MaterialTheme.typography.titleLarge)
                    PermissionItem(
                        name = "オーバーレイ表示",
                        isGranted = state.permissionStatus["overlay"] ?: false,
                        onClick = { state.eventSink(SettingsScreen.Event.OnRequestPermission("overlay")) }
                    )
                    PermissionItem(
                        name = "ユーザー補助機能",
                        isGranted = state.permissionStatus["accessibility"] ?: false,
                        onClick = { state.eventSink(SettingsScreen.Event.OnRequestPermission("accessibility")) }
                    )
                    PermissionItem(
                        name = "通知",
                        isGranted = state.permissionStatus["notification"] ?: false,
                        onClick = { state.eventSink(SettingsScreen.Event.OnRequestPermission("notification")) }
                    )
                }
            }

            HorizontalDivider()
        }

        item {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "金額設定", style = MaterialTheme.typography.titleLarge)

                    // 金額ステップ設定
                    var amountStepText by remember { mutableStateOf(state.amountStep.toString()) }
                    OutlinedTextField(
                        value = amountStepText,
                        onValueChange = { newValue ->
                            amountStepText = newValue
                            newValue.toLongOrNull()?.let { step ->
                                if (step > 0) {
                                    state.eventSink(SettingsScreen.Event.UpdateAmountStep(step))
                                }
                            }
                        },
                        label = { Text("金額ステップ") },
                        supportingText = { Text("金額入力時のステップ値（円）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )

                    // 摩擦係数設定
                    Text(
                        text = "感度: ${
                            String.format(
                                Locale.JAPAN,
                                "%.1f",
                                state.sensitivity
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Text(
                        text = "支出記録の感度を調整します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = state.sensitivity,
                        onValueChange = { value ->
                            state.eventSink(SettingsScreen.Event.UpdateSensitivity(value))
                        },
                        valueRange = 0.1f..3.0f,
                        steps = 28,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 摩擦係数設定
                    Text(
                        text = "摩擦係数: ${
                            String.format(
                                Locale.JAPAN,
                                "%.2f",
                                state.frictionMultiplier
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Text(
                        text = "支出記録のダイヤルの摩擦係数を調整します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = state.frictionMultiplier,
                        onValueChange = { value ->
                            state.eventSink(SettingsScreen.Event.UpdateFrictionMultiplier(value))
                        },
                        valueRange = 0.1f..3.0f,
                        steps = 28,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            HorizontalDivider()
        }

        item {
            Card(modifier = Modifier.padding(16.dp)) {
                ListItem(
                    headlineContent = { Text("オーバーレイ設定をリセット") },
                    modifier = Modifier.clickable { state.eventSink(SettingsScreen.Event.OnResetOverlaySettingsClicked) }
                )
            }
        }
    }
}
