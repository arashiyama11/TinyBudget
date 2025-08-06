package io.github.arashiyama11.tinybudget.di

import androidx.activity.ComponentActivity
import com.slack.circuit.runtime.presenter.Presenter
import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.ui.main.EditTransactionPresenter
import io.github.arashiyama11.tinybudget.ui.main.HomePresenter
import io.github.arashiyama11.tinybudget.ui.main.MainPresenter
import io.github.arashiyama11.tinybudget.ui.main.OnBoardingPresenter
import io.github.arashiyama11.tinybudget.ui.main.SettingsPresenter
import io.github.arashiyama11.tinybudget.ui.main.TriggerAppsPresenter
import kotlinx.collections.immutable.persistentSetOf

class MainActivityContainer(
    appContainer: AppContainer,
    context: ComponentActivity
) {

    val permissionManager = PermissionManager(context)
    val onBoardingPresenterFactory = OnBoardingPresenter.Factory(
        permissionManager = permissionManager
    )
    val homePresenterFactory = HomePresenter.Factory()
    val mainPresenterFactory = MainPresenter.Factory(
        transactionRepository = appContainer.transactionRepository,
        categoryRepository = appContainer.categoryRepository
    )
    val settingsPresenterFactory = SettingsPresenter.Factory(
        categoryRepository = appContainer.categoryRepository,
        settingsRepository = appContainer.settingsRepository,
        permissionManager = permissionManager
    )

    val editTransactionPresenterFactory = EditTransactionPresenter.Factory(
        transactionRepository = appContainer.transactionRepository,
        categoryRepository = appContainer.categoryRepository
    )

    val triggerAppsPresenterFactory = TriggerAppsPresenter.Factory(
        settingsRepository = appContainer.settingsRepository,
        context = context
    )


    val presenterFactories: Iterable<Presenter.Factory> = persistentSetOf(
        onBoardingPresenterFactory,
        homePresenterFactory,
        mainPresenterFactory,
        settingsPresenterFactory,
        editTransactionPresenterFactory,
        triggerAppsPresenterFactory
    )
}