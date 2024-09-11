package io.horizontalsystems.bankwallet.modules.safe4.node.supernode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Master_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Master_Node_Create_Join_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Node_Lock_Day
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Super_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Super_Node_Create_Join_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourCreateNodeModule
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourNodeService
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.BigInteger

class SafeFourCreateNodeViewModel(
        val wallet: Wallet,
        val isSuperNode: Boolean,
        private val amountService: SendAmountService,
        private val addressService: SendEvmAddressService,
        private val ethereumKit: EthereumKit,
        val coinMaxAllowedDecimals: Int,
        private val xRateService: XRateService,
        private val rpcBlockchainSafe4: RpcBlockchainSafe4,
) : ViewModelUiState<SafeFourCreateNodeModule.SafeFourCreateNodeUiState>()  {

    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    var coinRate by mutableStateOf(xRateService.getRate(wallet.token.coin.uid))
        private set

    private var isUnion = false
    var superNodeName = ""
    private var eNode = ""
    var introduction = ""
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var creatorIncentive = 10
    private var partnerIncentive = 45
    private var voterIncentive = 45

    private var existENode = false
    private var existNode = false
    private var existNodeFounder = false
    private var isInputCurrentWalletAddress = false

    private val disposables = CompositeDisposable()

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        onEnterAmount(BigDecimal(getLockAmount()))

        resetIncentive()
    }

    private fun handleUpdatedAmountState(amountState: SendAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState
        if (addressState.canBeSend) {
            checkNodeExist(addressState.address?.hex)
            existNodeFounder(addressState.address?.hex)
        }
        emitState()
    }

    fun getReceiveAddress(): String {
        return ethereumKit.receiveAddress.hex
    }

    override fun createState() = SafeFourCreateNodeModule.SafeFourCreateNodeUiState(
            title = R.string.Safe_Four_Register_Super_Node,
            addressError = addressState.addressError,
            availableBalance = amountState.availableBalance,
            amountCaution = amountState.amountCaution,
            canBeSend =
                if (isSuperNode)
                    amountState.availableBalance.compareTo(BigDecimal(getLockAmount())) > 0
                    && !existNodeFounder
                    && amountState.canBeSend && addressState.canBeSend
                    && !isInputCurrentWalletAddress
                    && superNodeName.length >= 8 && eNode.isNotBlank() && !existENode && !existNode
                    && introduction.length >= 8
                else
                    amountState.availableBalance.compareTo(BigDecimal(getLockAmount())) > 0
                    && !existNodeFounder
                    && amountState.canBeSend && addressState.canBeSend
                    && !isInputCurrentWalletAddress
                    && eNode.isNotBlank() && !existENode && !existNode
                    && introduction.isNotBlank() && introduction.length >= 8
            ,
            lockAmount = "${getLockAmount()} SAFE",
            existENode,
            existNode,
            existNodeFounder,
            isInputCurrentWalletAddress
    )

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        if (getReceiveAddress().equals(address?.hex, true)) {
            isInputCurrentWalletAddress = true
            emitState()
            return
        }
        isInputCurrentWalletAddress = false
        addressService.setAddress(address)
    }

    fun onEnterNodeName(name: String?) {
        superNodeName = name ?: ""
        emitState()
    }

    fun onEnterENode(eNode: String?) {
        this.eNode = eNode ?: ""
        existENode = false
        existENode(eNode)
    }

    private fun existENode(eNode: String?) {
        eNode?.let { eNode ->
            try {
                rpcBlockchainSafe4.existNodeEnode(eNode)
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            existENode = it
                        }, {

                        }).let {
                            disposables.add(it)
                        }
            } catch (e: Exception) {

            } finally {
                emitState()
            }
        }
    }

    private fun checkNodeExist(address: String?) {
        if (address == null)    return
        rpcBlockchainSafe4.existNodeAddress(address)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    existNode = it
                    emitState()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    private fun existNodeFounder(address: String?) {
        if (address == null)    return
        rpcBlockchainSafe4.existNodeFounder(address)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    existNodeFounder = it
                    emitState()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    fun onSelectType(index: Int) {
        this.isUnion = index == 1
        onEnterAmount(BigDecimal(getLockAmount()))
        emitState()
    }

    fun onEnterIntroduction(introduction: String?) {
        this.introduction = introduction ?: ""
        emitState()
    }

    private fun resetIncentive() {
        if (isSuperNode) {
            creatorIncentive = 10
            partnerIncentive = 45
            voterIncentive = 45
            /*if (isUnion) {
                creatorIncentive = 10
                partnerIncentive = 45
                voterIncentive = 45
            } else {
                creatorIncentive = 10
                partnerIncentive = 45
                voterIncentive = 45
            }*/
        } else {
            if (isUnion) {
                creatorIncentive = 50
                partnerIncentive = 50
            } else {
                creatorIncentive = 100
                partnerIncentive = 0
            }
        }
    }

    fun onEnterIncentive(partnerIncentive: Int, creatorIncentive: Int, voterIncentive: Int = 45) {
        this.partnerIncentive = partnerIncentive
        this.creatorIncentive = creatorIncentive
        this.voterIncentive = voterIncentive
    }

    private fun getLockAmount(): Int {
        return if (!isUnion) {
            if (isSuperNode) Super_Node_Create_Amount else Master_Node_Create_Amount
        } else {
            if (isSuperNode) Super_Node_Create_Join_Amount else Master_Node_Create_Join_Amount
        }
    }

    fun getCreateNodeData(): SafeFourConfirmationModule.CreateNodeData {
        if (isUnion) {
            resetIncentive()
        }
        return SafeFourConfirmationModule.CreateNodeData(
                BigDecimal.valueOf(getLockAmount().toLong()).movePointRight(18).toBigInteger(),
                isUnion,
                addressState.address!!.hex,
                BigInteger.valueOf(Node_Lock_Day.toLong()),
                superNodeName,
                eNode,
                introduction,
                creatorIncentive.toBigInteger(),
                partnerIncentive.toBigInteger(),
                voterIncentive.toBigInteger(),
                isSuperNode
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
