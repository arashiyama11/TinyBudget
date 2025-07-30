package io.github.arashiyama11.tinybudget.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.theme.LocalSnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object OnBoardingScreen : Screen {
    data class State(
        val permissionPhase: PermissionPhase = PermissionPhase.GrantNotification,
        val eventSink: (Event) -> Unit
    ) : CircuitUiState

    sealed interface Event {
        data object OnGrantNotificationClicked : Event
        data object OnGrantOverlayClicked : Event
        data object OnGrantAccessibilityClicked : Event
        data object OnCompleteClicked : Event
    }
}

enum class PermissionPhase {
    GrantNotification,
    GrantOverlay,
    GrantAccessibility,
    Complete
}

class OnBoardingPresenter(
    private val navigator: Navigator,
    private val permissionManager: PermissionManager,
) : Presenter<OnBoardingScreen.State> {

    private val scope = CoroutineScope(Dispatchers.Main)

    class Factory(
        private val permissionManager: PermissionManager,
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext
        ): Presenter<*>? {
            return if (screen is OnBoardingScreen) {
                OnBoardingPresenter(
                    navigator = navigator,
                    permissionManager = permissionManager,
                )
            } else {
                null
            }
        }
    }


    @Composable
    override fun present(): OnBoardingScreen.State {
        var permissionPhase by remember { mutableStateOf(initialPhase()) }
        val snackbar = LocalSnackbarHostState.current


        return OnBoardingScreen.State(permissionPhase) { event ->
            permissionPhase =
                handleEvent(event, OnBoardingScreen.State(permissionPhase, {})).permissionPhase

            scope.launch {
                when (event) {
                    is OnBoardingScreen.Event.OnGrantNotificationClicked -> {
                        if (permissionManager.requestNotificationPermission()) {
                            permissionPhase = PermissionPhase.GrantOverlay
                        } else {
                            snackbar.showSnackbar("通知権限が許可されませんでした")
                        }
                    }

                    is OnBoardingScreen.Event.OnGrantOverlayClicked -> {
                        if (permissionManager.requestOverlayPermission()) {
                            permissionPhase = PermissionPhase.GrantAccessibility
                        } else {
                            scope.launch {
                                snackbar.showSnackbar("オーバーレイ権限が許可されませんでした")
                            }
                        }
                    }

                    is OnBoardingScreen.Event.OnGrantAccessibilityClicked -> {
                        if (permissionManager.requestAccessibilityPermission()) {
                            permissionPhase = PermissionPhase.Complete
                        } else {
                            scope.launch {
                                snackbar.showSnackbar("ユーザー補助機能が許可されませんでした")
                            }
                        }
                    }

                    is OnBoardingScreen.Event.OnCompleteClicked -> {
                        navigator.goTo(MainScreen)
                    }
                }
            }
        }
    }

    fun handleEvent(
        event: OnBoardingScreen.Event,
        currentState: OnBoardingScreen.State
    ): OnBoardingScreen.State {
        return when (event) {
            is OnBoardingScreen.Event.OnGrantNotificationClicked -> currentState.copy(
                permissionPhase = PermissionPhase.GrantNotification
            )

            is OnBoardingScreen.Event.OnGrantOverlayClicked -> currentState.copy(permissionPhase = PermissionPhase.GrantOverlay)
            is OnBoardingScreen.Event.OnGrantAccessibilityClicked -> currentState.copy(
                permissionPhase = PermissionPhase.GrantAccessibility
            )

            is OnBoardingScreen.Event.OnCompleteClicked -> currentState.copy(permissionPhase = PermissionPhase.Complete)
        }
    }

    private fun initialPhase(): PermissionPhase {
        val phase = when {
            !permissionManager.checkNotificationPermission() -> PermissionPhase.GrantNotification
            !permissionManager.checkOverlayPermission() -> PermissionPhase.GrantOverlay
            !permissionManager.checkAccessibilityPermission() -> PermissionPhase.GrantAccessibility
            else -> PermissionPhase.Complete
        }

        return phase
    }

}

@Composable
fun OnBoardingUi(state: OnBoardingScreen.State, modifier: Modifier = Modifier) {
    val snackbarHostState = LocalSnackbarHostState.current

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.permissionPhase) {
                PermissionPhase.GrantNotification -> {
                    PermissionStep(
                        title = "通知へのアクセス",
                        description = "支出を自動で記録するために、通知へのアクセスを許可してください。",
                        buttonText = "通知へのアクセスを許可",
                        onButtonClick = { state.eventSink(OnBoardingScreen.Event.OnGrantNotificationClicked) }
                    )
                }

                PermissionPhase.GrantOverlay -> {
                    PermissionStep(
                        title = "他のアプリの上に表示",
                        description = "入力フォームを素早く表示するために、オーバーレイ表示を許可してください。",
                        buttonText = "オーバーレイ表示を許可",
                        onButtonClick = { state.eventSink(OnBoardingScreen.Event.OnGrantOverlayClicked) }
                    )
                }

                PermissionPhase.GrantAccessibility -> {
                    PermissionStep(
                        title = "ユーザー補助機能",
                        description = "入力内容を自動で確定するために、ユーザー補助機能を有効にしてください。",
                        buttonText = "ユーザー補助機能を有効にする",
                        onButtonClick = { state.eventSink(OnBoardingScreen.Event.OnGrantAccessibilityClicked) }
                    )

                    Button({
                        state.eventSink(OnBoardingScreen.Event.OnCompleteClicked)
                    }) {
                        Text(text = "スキップ for debugging")
                    }
                }

                PermissionPhase.Complete -> {
                    PermissionStep(
                        title = "設定が完了しました",
                        description = "TinyBudgetへようこそ！さあ、家計簿をつけ始めましょう。",
                        buttonText = "始める",
                        onButtonClick = { state.eventSink(OnBoardingScreen.Event.OnCompleteClicked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStep(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onButtonClick) {
        Text(text = buttonText)
    }
}