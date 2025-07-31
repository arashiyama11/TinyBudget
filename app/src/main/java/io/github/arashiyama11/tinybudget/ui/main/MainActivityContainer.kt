package io.github.arashiyama11.tinybudget.ui.main

import android.content.Context
import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.data.AppContainer

class MainActivityContainer(
    appContainer: AppContainer,
    permissionManager: PermissionManager,
    context: Context
) {
    val onBoardingPresenterFactory = OnBoardingPresenter.Factory(
        permissionManager = permissionManager
    )
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
}
