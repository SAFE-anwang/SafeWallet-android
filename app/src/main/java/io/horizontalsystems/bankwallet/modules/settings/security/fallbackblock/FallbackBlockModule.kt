package io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object FallbackBlockModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FallbackBlockViewModel(
                App.walletManager, App.accountManager, App.adapterManager
            ) as T
        }
    }
}
