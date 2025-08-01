package io.github.arashiyama11.tinybudget.ui.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import androidx.core.graphics.drawable.toBitmap
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Parcelize
data object TriggerAppsScreen : Screen {
    data class AppInfo(
        val label: String,
        val packageName: String,
        val isSelected: Boolean,
        val icon: ImageBitmap?
    )

    data class State(
        val apps: List<AppInfo>,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

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

    private val iconCache = mutableStateMapOf<String, ImageBitmap>()

    @Composable
    override fun present(): TriggerAppsScreen.State {
        val scope = rememberStableCoroutineScope()
        val selectedApps by settingsRepository.triggerApps.collectAsRetainedState(initial = emptySet())

        // allBasic は今までどおり先に一度だけ作成
        val initialSelected = rememberRetained(selectedApps.isEmpty()) { selectedApps }
        val allBasic = rememberRetained(initialSelected) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map {
                    TriggerAppsScreen.AppInfo(
                        label = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName,
                        isSelected = initialSelected.contains(it.packageName),
                        icon = null
                    )
                }
                .sortedWith(
                    compareByDescending<TriggerAppsScreen.AppInfo> { it.isSelected }
                        .thenBy { it.label }
                )
                .toList()
        }

        LaunchedEffect(allBasic.map { it.packageName }) {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                allBasic.forEach { info ->
                    if (!iconCache.containsKey(info.packageName)) {
                        val drawable = pm.getApplicationIcon(info.packageName)
                        val bmp = drawable.toBitmap().asImageBitmap()
                        iconCache[info.packageName] = bmp
                    }
                }
            }
        }

        val apps = allBasic.map { basic ->
            basic.copy(
                isSelected = selectedApps.contains(basic.packageName),
                icon = iconCache[basic.packageName]
            )
        }

        return TriggerAppsScreen.State(apps) { event ->
            when (event) {
                is TriggerAppsScreen.Event.GoBack ->
                    navigator.pop()

                is TriggerAppsScreen.Event.OnToggleAppSelection ->
                    scope.launch {
                        val newSet = selectedApps.toMutableSet().apply {
                            if (event.isSelected) add(event.packageName)
                            else remove(event.packageName)
                        }
                        settingsRepository.setTriggerApps(newSet)
                    }
            }
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
    val listState = rememberLazyListState()

    LaunchedEffect(state.apps.size) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("トリガーアプリ設定") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(TriggerAppsScreen.Event.GoBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                            Image(app.icon, contentDescription = app.label, Modifier.size(40.dp))
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
