package io.github.arashiyama11.tinybudget.ui.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.graphics.createBitmap

@Parcelize
data object TriggerAppsScreen : Screen {
    data class AppInfo(
        val label: String,
        val packageName: String,
        val isSelected: Boolean
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
    @Composable
    override fun present(): TriggerAppsScreen.State {
        val scope = rememberCoroutineScope()
        // ユーザーの現在の選択セット
        val selectedApps by settingsRepository.triggerApps
            .collectAsState(initial = emptySet())

        // 初回のみ「最初の selectedApps の値」をキャプチャ
        val initialSelected = remember(selectedApps.isEmpty()) { selectedApps }

        // allApps：初回だけ計算。選択済み(true)を先に、ラベル順でソート
        val allApps = remember(initialSelected) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map {
                    TriggerAppsScreen.AppInfo(
                        label = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName,
                        isSelected = initialSelected.contains(it.packageName)
                    )
                }
                .sortedWith(
                    compareByDescending<TriggerAppsScreen.AppInfo> { it.isSelected }
                        .thenBy { it.label }
                )
                .toList()
        }

        // apps：allApps の順序はそのまま、isSelected フラグだけ最新の selectedApps で更新
        val apps = allApps.map {
            it.copy(isSelected = selectedApps.contains(it.packageName))
        }

        return TriggerAppsScreen.State(apps = apps) { event ->
            when (event) {
                is TriggerAppsScreen.Event.GoBack ->
                    navigator.pop()

                is TriggerAppsScreen.Event.OnToggleAppSelection -> {
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
    val pm = LocalContext.current.packageManager

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
                .fillMaxSize()
        ) {
            items(state.apps, key = { it.packageName }) { app ->
                // アプリごとに一度だけ変換するように remember でキャッシュ
                val iconBitmap = remember(app.packageName) {
                    // Drawable を取得
                    val drawable = pm.getApplicationIcon(app.packageName)
                    // Bitmap にキャスト or 描画
                    val bmp = when (drawable) {
                        is BitmapDrawable -> drawable.bitmap
                        else -> {
                            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
                            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
                            createBitmap(w, h).also { bitmap ->
                                Canvas(bitmap).apply {
                                    drawable.setBounds(0, 0, w, h)
                                    drawable.draw(this)
                                }
                            }
                        }
                    }
                    bmp.asImageBitmap()
                }

                ListItem(
                    // ここでアイコンを左に表示
                    leadingContent = {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(40.dp)
                        )
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