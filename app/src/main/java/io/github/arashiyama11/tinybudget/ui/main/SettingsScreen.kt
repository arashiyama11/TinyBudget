package io.github.arashiyama11.tinybudget.ui.main

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.ui.component.Footer
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize


@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val categories: List<Category>,
        val showAddCategoryDialog: Boolean,
        val editingCategory: Category?,
        val permissionStatus: Map<String, Boolean>,
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
        val scope = rememberCoroutineScope()
        val categories by categoryRepository.getAllCategories()
            .collectAsState(initial = emptyList())
        var showAddCategoryDialog by rememberRetained { mutableStateOf(false) }
        var editingCategory by rememberRetained { mutableStateOf<Category?>(null) }
        val permissionStatus by remember { mutableStateOf(getPermissionStatus()) }

        return SettingsScreen.State(
            categories = categories,
            showAddCategoryDialog = showAddCategoryDialog,
            editingCategory = editingCategory,
            permissionStatus = permissionStatus
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
            }
        }
    }

    private fun getPermissionStatus(): Map<String, Boolean> {
        return mapOf(
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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Footer(currentScreen = SettingsScreen, navigate = {
                state.eventSink(SettingsScreen.Event.NavigateTo(it))
            })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
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
}

@Composable
private fun PermissionItem(name: String, isGranted: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            Text(
                if (isGranted) "許可済み" else "未許可",
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clickable(enabled = !isGranted, onClick = onClick)
    )
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("カテゴリを追加") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("カテゴリ名") }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    var text by remember { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("カテゴリを編集") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("カテゴリ名") }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(category.copy(name = text)) },
                enabled = text.isNotBlank()
            ) {
                Text("更新")
            }
        },
        dismissButton = {
            Column {
                Button(onClick = onDismiss) {
                    Text("キャンセル")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onDelete(category) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            }
        }
    )
}

