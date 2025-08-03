package io.github.arashiyama11.tinybudget.ui.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
data object TriggerAppsScreen : Screen {
    data class AppInfo(
        val label: String,
        val packageName: String,
        val isSelected: Boolean,
        val icon: ImageBitmap?
    )

    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val apps: ImmutableList<AppInfo>,
            val eventSink: (Event) -> Unit
        ) : State
    }

    sealed interface Event : CircuitUiEvent {
        data object GoBack : Event
        data class OnToggleAppSelection(val packageName: String, val isSelected: Boolean) : Event
    }
}

class TriggerAppsPresenter(
    private val navigator: Navigator,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : Presenter<TriggerAppsScreen.State> {

    @Composable
    override fun present(): TriggerAppsScreen.State {
        var appInfos by remember { mutableStateOf<List<TriggerAppsScreen.AppInfo>?>(null) }
        val selectedApps by settingsRepository.triggerApps.collectAsState(initial = emptySet())
        val initialSelectedApps = remember(selectedApps.isEmpty()) { selectedApps }
        val scope = rememberStableCoroutineScope()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .asSequence()
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                    .map {
                        val icon = try {
                            pm.getApplicationIcon(it.packageName).toBitmap().asImageBitmap()
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                        TriggerAppsScreen.AppInfo(
                            label = pm.getApplicationLabel(it).toString(),
                            packageName = it.packageName,
                            isSelected = selectedApps.contains(it.packageName),
                            icon = icon
                        )
                    }
                    .toList()
                appInfos = allApps
            }
        }

        val eventSink: (TriggerAppsScreen.Event) -> Unit = remember {
            { event ->
                when (event) {
                    is TriggerAppsScreen.Event.GoBack -> navigator.pop()
                    is TriggerAppsScreen.Event.OnToggleAppSelection -> {
                        scope.launch {
                            val currentSelected = settingsRepository.triggerApps.first()
                            val newSet = currentSelected.toMutableSet().apply {
                                if (event.isSelected) add(event.packageName)
                                else remove(event.packageName)
                            }
                            settingsRepository.setTriggerApps(newSet)
                        }
                    }
                }
            }
        }

        return if (appInfos == null) {
            TriggerAppsScreen.State.Loading
        } else {
            val sortedApps = appInfos.orEmpty().map { appInfo ->
                appInfo.copy(isSelected = selectedApps.contains(appInfo.packageName))
            }.sortedWith(
                compareByDescending<TriggerAppsScreen.AppInfo> { initialSelectedApps.contains(it.packageName) }
                    .thenBy { it.label }
            )
            TriggerAppsScreen.State.Success(
                apps = sortedApps.toImmutableList(),
                eventSink = eventSink
            )
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val context: Context
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is TriggerAppsScreen) {
                TriggerAppsPresenter(navigator, settingsRepository, this.context)
            } else {
                null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerAppsUi(state: TriggerAppsScreen.State, modifier: Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("トリガーアプリ設定") },
                navigationIcon = {
                    val eventSink = (state as? TriggerAppsScreen.State.Success)?.eventSink
                    IconButton(
                        onClick = { eventSink?.invoke(TriggerAppsScreen.Event.GoBack) },
                        enabled = eventSink != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (state) {
            is TriggerAppsScreen.State.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is TriggerAppsScreen.State.Success -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    state = listState,
                ) {
                    items(state.apps, key = { it.packageName }) { app ->
                        ListItem(
                            leadingContent = {
                                if (app.icon != null) {
                                    Image(
                                        app.icon,
                                        contentDescription = app.label,
                                        Modifier.size(40.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Apps,
                                        contentDescription = app.label,
                                        Modifier.size(40.dp)
                                    )
                                }
                            },
                            headlineContent = { Text(app.label) },
                            trailingContent = {
                                Checkbox(
                                    checked = app.isSelected,
                                    onCheckedChange = { isSelected ->
                                        state.eventSink(
                                            TriggerAppsScreen.Event.OnToggleAppSelection(
                                                app.packageName,
                                                isSelected
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
    }
}
