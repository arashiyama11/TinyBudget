package io.github.arashiyama11.tinybudget

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import io.github.arashiyama11.tinybudget.ui.main.EditTransactionScreen
import io.github.arashiyama11.tinybudget.ui.main.EditTransactionUi
import io.github.arashiyama11.tinybudget.ui.main.HomeScreen
import io.github.arashiyama11.tinybudget.ui.main.HomeUi
import io.github.arashiyama11.tinybudget.di.MainActivityContainer
import io.github.arashiyama11.tinybudget.ui.main.MainScreen
import io.github.arashiyama11.tinybudget.ui.main.MainUi
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingScreen
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingUi
import io.github.arashiyama11.tinybudget.ui.main.SettingsScreen
import io.github.arashiyama11.tinybudget.ui.main.SettingsUi
import io.github.arashiyama11.tinybudget.ui.main.TriggerAppsScreen
import io.github.arashiyama11.tinybudget.ui.main.TriggerAppsUi
import io.github.arashiyama11.tinybudget.ui.theme.LocalSnackbarHostState
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme

class MainActivity : AppCompatActivity() {

    private lateinit var container: MainActivityContainer

    override fun onResume() {
        super.onResume()
        container.permissionManager.onActivityResumed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = MainActivityContainer(appContainer, this)


        val circuit = Circuit.Builder()
            .addPresenterFactories(container.presenterFactories)
            .addUi<OnBoardingScreen, OnBoardingScreen.State> { uiState, modifier ->
                OnBoardingUi(uiState, modifier)
            }
            .addUi<HomeScreen, HomeScreen.State> { uiState, modifier ->
                HomeUi(uiState, modifier)
            }
            .addUi<MainScreen, MainScreen.State> { uiState, modifier ->
                MainUi(uiState, modifier)
            }
            .addUi<SettingsScreen, SettingsScreen.State> { uiState, modifier ->
                SettingsUi(uiState, modifier)
            }
            .addUi<EditTransactionScreen, EditTransactionScreen.State> { uiState, modifier ->
                EditTransactionUi(uiState, modifier)
            }
            .addUi<TriggerAppsScreen, TriggerAppsScreen.State> { uiState, modifier ->
                TriggerAppsUi(uiState, modifier)
            }
            .build()


        val snackbarHostState = SnackbarHostState()

        setContent {
            val backStack = rememberSaveableBackStack(root = OnBoardingScreen)
            val navigator = rememberCircuitNavigator(backStack)
            TinyBudgetTheme {
                CircuitCompositionLocals(circuit) {
                    CompositionLocalProvider(
                        LocalSnackbarHostState provides snackbarHostState
                    ) {
                        NavigableCircuitContent(navigator, backStack)
                    }
                }
            }
        }
    }
}
