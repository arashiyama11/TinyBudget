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
import io.github.arashiyama11.tinybudget.data.local.database.AppDatabase
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.main.MainPresenter
import io.github.arashiyama11.tinybudget.ui.main.MainScreen
import io.github.arashiyama11.tinybudget.ui.main.MainUi
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingPresenter
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingScreen
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingUi
import io.github.arashiyama11.tinybudget.ui.theme.LocalSnackbarHostState
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme

class MainActivity : AppCompatActivity() {

    private val permissionManager by lazy { PermissionManager(this) }
    private val appDatabase by lazy { AppDatabase.getDatabase(this) }
    private val categoryRepository by lazy { CategoryRepository(appDatabase.categoryDao()) }
    private val transactionRepository by lazy { TransactionRepository(appDatabase.transactionDao()) }

    override fun onResume() {
        super.onResume()
        // 設定画面から戻ってきたときに呼び出し
        permissionManager.onActivityResumed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val circuit = Circuit.Builder()
            .addPresenterFactory(
                OnBoardingPresenter.Factory(
                    permissionManager,
                )
            )
            .addUi<OnBoardingScreen, OnBoardingScreen.State> { uiState, modifier ->
                OnBoardingUi(uiState, modifier)
            }
            .addPresenterFactory(
                MainPresenter.Factory(
                    transactionRepository,
                    categoryRepository
                )
            )
            .addUi<MainScreen, MainScreen.State> { uiState, modifier ->
                MainUi(uiState, modifier)
            }.build()

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
