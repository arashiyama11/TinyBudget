package io.github.arashiyama11.tinybudget.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.OverlayService
import io.github.arashiyama11.tinybudget.ui.component.Footer
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeScreen : Screen {
    data class State(
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
    }
}

class HomePresenter : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        return HomeScreen.State { event ->
        }
    }

    class Factory : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is HomeScreen) {
                HomePresenter()
            } else {
                null
            }
        }
    }
}

@Composable
fun HomeUi(state: HomeScreen.State, modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            // MainScreen (page 0) の時だけ FAB を表示
            if (pagerState.currentPage == 0) {
                FloatingActionButton(onClick = {
                    val intent = Intent(context, OverlayService::class.java)
                    context.startForegroundService(intent)
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "支出を記録する")
                }
            }
        },
        bottomBar = {
            Footer(
                currentScreen = if (pagerState.currentPage == 0) MainScreen else SettingsScreen,
                onTabSelected = { screen ->
                    scope.launch {
                        val page = if (screen is MainScreen) 0 else 1
                        pagerState.scrollToPage(page)
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
        ) { page ->
            val pageModifier = modifier.padding(paddingValues)
            when (page) {
                0 -> CircuitContent(screen = MainScreen, modifier = pageModifier)
                1 -> CircuitContent(screen = SettingsScreen, modifier = pageModifier)
            }
        }
    }
}
