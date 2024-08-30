package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class RedeemSafe3SelectViewModel(): ViewModelUiState<RedeemSafe3Module.RedeemSafe3SelectUiState>() {

    val tabs = listOf(
            Pair(0, R.string.Redeem_Safe3_Other_Wallet),
            Pair(1, R.string.Redeem_Safe3_Local_Wallet)
    )
    private var syncing = true

    override fun createState(): RedeemSafe3Module.RedeemSafe3SelectUiState {
        return RedeemSafe3Module.RedeemSafe3SelectUiState(syncing)
    }

    fun updateSyncStatus(syncing: Boolean) {
        this.syncing = syncing
        emitState()
    }
}