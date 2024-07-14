package io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SafeFourCreateNodeConfirmationViewModel(
        private val title: String,
        private val isSuper: Boolean,
        private val wallet: Wallet,
        val createNodeData: SafeFourConfirmationModule.CreateNodeData,
        private val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<SafeFourConfirmationModule.SafeFourCreateNodeConfirmationUiState>()  {


    private val disposables = CompositeDisposable()

    init {

    }

    fun send() {
        val single = if (isSuper) {
            evmKitWrapper.createSuperNode(
                    createNodeData.value,
                    createNodeData.isUnion,
                    createNodeData.address,
                    createNodeData.lockDay,
                    createNodeData.name,
                    createNodeData.enode,
                    createNodeData.description,
                    createNodeData.creatorIncentive,
                    createNodeData.partnerIncentive,
                    createNodeData.voterIncentive
            )
        } else {
            evmKitWrapper.createMasterNode(
                    createNodeData.value,
                    createNodeData.isUnion,
                    createNodeData.address,
                    createNodeData.lockDay,
                    createNodeData.enode,
                    createNodeData.description,
                    createNodeData.creatorIncentive,
                    createNodeData.partnerIncentive
            )
        }
        single.subscribeOn(Schedulers.io())
                .subscribe({

                }, {

                }).let { disposables.add(it) }
    }

    override fun createState() = SafeFourConfirmationModule.SafeFourCreateNodeConfirmationUiState(
        title = title
    )

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
