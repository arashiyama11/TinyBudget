package io.github.arashiyama11.tinybudget

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import io.github.arashiyama11.tinybudget.ui.main.EditTransactionScreen
import io.github.arashiyama11.tinybudget.ui.main.EditTransactionUi
import io.github.arashiyama11.tinybudget.ui.main.MainActivityContainer
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

    private val permissionManager by lazy { PermissionManager(this) }
    private lateinit var container: MainActivityContainer

    override fun onResume() {
        super.onResume()
        // 設定画面から戻ってきたときに呼び出し
        permissionManager.onActivityResumed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as TinyBudgetApp).appContainer
        container = MainActivityContainer(appContainer, permissionManager, this)

        val circuit = Circuit.Builder()
            .addPresenterFactory(container.onBoardingPresenterFactory)
            .addUi<OnBoardingScreen, OnBoardingScreen.State> { uiState, modifier ->
                OnBoardingUi(uiState, modifier)
            }
            .addPresenterFactory(container.mainPresenterFactory)
            .addUi<MainScreen, MainScreen.State> { uiState, modifier ->
                MainUi(uiState, modifier)
            }
            .addPresenterFactory(container.settingsPresenterFactory)
            .addUi<SettingsScreen, SettingsScreen.State> { uiState, modifier ->
                SettingsUi(uiState, modifier)
            }
            .addPresenterFactory(container.editTransactionPresenterFactory)
            .addUi<EditTransactionScreen, EditTransactionScreen.State> { uiState, modifier ->
                EditTransactionUi(uiState, modifier)
            }
            .addPresenterFactory(container.triggerAppsPresenterFactory)
            .addUi<TriggerAppsScreen, TriggerAppsScreen.State> { uiState, modifier ->
                TriggerAppsUi(uiState, modifier)
            }
            .build()

        setContent {
            val backStack = rememberSaveableBackStack(root = OnBoardingScreen)
            val navigator = rememberCircuitNavigator(backStack)
            val snackbarHostState = remember { SnackbarHostState() }
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
