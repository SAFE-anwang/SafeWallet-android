package io.horizontalsystems.bankwallet.modules.safe4.node.masternode

import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeItem
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class SafeFourCreateNodeViewModel(
        private val title: String,
        val wallet: Wallet,
        val predefinedAddress: String?,
        private val amountService: SendAmountService,
        private val addressService: SendEvmAddressService,
        private val nodeService: SafeFourNodeService
) : ViewModelUiState<SafeFourModule.SafeFourCreateNodeUiState>()  {

    var tmpItemToShow: NodeItem? = null
    private var enterAddress: String? = null
    private var superNodeName = ""
    private var eNode = ""
    private var introduction = ""
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    private val disposables = CompositeDisposable()

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    override fun createState() = SafeFourModule.SafeFourCreateNodeUiState(
            title = title,
            addressError = addressState.addressError,
            canSend = amountState.canBeSend && addressState.canBeSend
    )


    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun onEnterNodeName(name: String?) {
        superNodeName = name ?: ""
    }

    fun onEnterENode(eNode: String?) {
        this.eNode = eNode ?: ""
    }

    fun onEnterIntroduction(introduction: String?) {
        this.introduction = introduction ?: ""
    }


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
