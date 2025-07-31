package io.github.arashiyama11.tinybudget.ui.main

import io.github.arashiyama11.tinybudget.PermissionManager
import io.github.arashiyama11.tinybudget.data.AppContainer

class MainActivityContainer(appContainer: AppContainer, permissionManager: PermissionManager) {
    val onBoardingPresenterFactory = OnBoardingPresenter.Factory(
        permissionManager = permissionManager
    )
    val mainPresenterFactory = MainPresenter.Factory(
        transactionRepository = appContainer.transactionRepository,
        categoryRepository = appContainer.categoryRepository
    )
    val settingsPresenterFactory = SettingsPresenter.Factory()

    val editTransactionPresenterFactory = EditTransactionPresenter.Factory(
        transactionRepository = appContainer.transactionRepository,
        categoryRepository = appContainer.categoryRepository
    )
}
