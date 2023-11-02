package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable

class SafeWalletConnectViewModel(
    private val clearables: List<Clearable>
) : ViewModel() {

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

}