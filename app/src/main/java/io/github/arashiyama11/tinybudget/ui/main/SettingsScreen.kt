package io.github.arashiyama11.tinybudget.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize
import io.github.arashiyama11.tinybudget.ui.component.Footer


@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class NavigateTo(val screen: Screen) : Event
    }
}

class SettingsPresenter(
    private val navigator: Navigator
) : Presenter<SettingsScreen.State> {
    @Composable
    override fun present(): SettingsScreen.State {
        return SettingsScreen.State { event ->
            when (event) {
                is SettingsScreen.Event.NavigateTo -> {
                    navigator.goTo(event.screen)
                }
            }
        }
    }

    class Factory : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is SettingsScreen) {
                SettingsPresenter(navigator)
            } else {
                null
            }
        }
    }
}


@Composable
fun SettingsUi(state: SettingsScreen.State, modifier: Modifier) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Footer(currentScreen = SettingsScreen, navigate = {
                state.eventSink(SettingsScreen.Event.NavigateTo(it))
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings Screen")
        }
    }
}
