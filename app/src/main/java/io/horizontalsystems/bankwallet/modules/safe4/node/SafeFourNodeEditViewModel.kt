package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.createCaution
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmAddressService
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SafeFourNodeEditViewModel(
        val wallet: Wallet,
        val isSuper: Boolean,
        val nodeName: String,
        val nodeAddress: String,
        val nodeEnode: String,
        val nodeDesc: String,
        val nodeId: Int,
        val incentivePlan: NodeIncentivePlan,
        private val safe4: RpcBlockchainSafe4,
        private val addressService: SendEvmAddressService,
        private val privateKey: String,
        private val walletAddress: String
) : ViewModelUiState<NodeEditUiState>() {

    private var inputNodeName: String = nodeName
    private var inputNodeAddress: String = nodeAddress
    private var inputNodeEnode: String = nodeEnode
    private var inputNodeDesc: String = nodeDesc

    private var inputPartnerIncentive = incentivePlan.partner
    private var inputCreatorIncentive = incentivePlan.creator
    private var inputVoterIncentive = incentivePlan.voter

    private var nameUpdateSuccess = false
    private var addressUpdateSuccess = false
    private var enodeUpdateSuccess = false
    private var descUpdateSuccess = false
    private var incentiveUpdateSuccess = false
    private var incentiveCanUpdate = false

    private var addressState = addressService.stateFlow.value

    private var existENode = false
    private var existNode = false
    private var existNodeFounder = false
    private var isInputCurrentWalletAddress = false

    var sendResult by mutableStateOf<SendResult?>(null)

    private val disposables = CompositeDisposable()

    init {
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
    }


    private fun handleUpdatedAddressState(addressState: SendEvmAddressService.State) {
        this.addressState = addressState
        if (addressState.canBeSend && addressState.address != null) {
            checkNodeExist(addressState.address.hex)
            existNodeFounder(addressState.address.hex)
        }
        if (!addressState.canBeSend) {
            existNode = false
        }
        emitState()
    }

    override fun createState() =
        NodeEditUiState(
            wallet,
            nodeName,
            nodeAddress,
            nodeEnode,
            nodeDesc,
            addressState.addressError,
            !nameUpdateSuccess && !inputNodeName.equals(nodeName) && inputNodeName.isNotBlank() && inputNodeName.length >= 8,
            !addressUpdateSuccess && !nodeAddress.equals(addressState.address?.hex, true)
                    && addressState.canBeSend && !existNode && !existNodeFounder
                    && !isInputCurrentWalletAddress,
            !enodeUpdateSuccess && !inputNodeEnode.equals(nodeEnode) && inputNodeEnode.isNotBlank() && !existENode,
            !descUpdateSuccess && !inputNodeDesc.equals(nodeDesc) && inputNodeDesc.isNotBlank() && inputNodeDesc.length >= 12,
            !incentiveUpdateSuccess && incentiveCanUpdate,
            existENode,
            existNode,
            existNodeFounder

        )

    fun onEnterName(name: String) {
        this.inputNodeName = name
        emitState()
    }

    fun onEnterENODE(enode: String) {
        this.inputNodeEnode = enode
        existENode(enode)
    }

    fun onEnterDesc(desc: String) {
        this.inputNodeDesc = desc
        emitState()
    }

    fun onEnterAddress(address: Address?) {
        if (nodeAddress.equals(address?.hex, true) && walletAddress.equals(address?.hex, true)) {
            isInputCurrentWalletAddress = true
            emitState()
            return
        }
        isInputCurrentWalletAddress = false
        addressService.setAddress(address)
    }

    private fun existENode(eNode: String?) {
        eNode?.let { eNode ->
            try {
                safe4.existNodeEnode(isSuper, eNode)
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
        if (address == null || nodeAddress == address)    return
        safe4.existNodeAddress(address)
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
        if (address == null || nodeAddress == address)    return
        safe4.existNodeFounder(address)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    existNodeFounder = it
                    emitState()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    fun updateName() {
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                safe4.changeName(privateKey, nodeAddress, inputNodeName)
                sendResult = SendResult.Sent(null)
                nameUpdateSuccess = true
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    fun updateAddress() {
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                safe4.changeAddress(isSuper, privateKey, nodeAddress, addressState.address!!.hex)
                sendResult = SendResult.Sent(null)
                addressUpdateSuccess = true
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    fun updateEnode() {
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                safe4.changeEnode(isSuper, privateKey, nodeAddress, inputNodeEnode)
                sendResult = SendResult.Sent(null)
                enodeUpdateSuccess = true
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    fun updateDesc() {
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                safe4.changeDescription(isSuper, privateKey, nodeAddress, inputNodeDesc)
                sendResult = SendResult.Sent(null)
                descUpdateSuccess = true
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    fun updateDescIncentive() {
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                safe4.changeIncentive(privateKey, nodeId.toBigInteger(),
                    inputCreatorIncentive.toBigInteger(), inputPartnerIncentive.toBigInteger(), inputVoterIncentive.toBigInteger())
                sendResult = SendResult.Sent(null)
                incentiveUpdateSuccess = true
                emitState()
            } catch (e: Exception) {
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    fun onEnterIncentive(partnerIncentive: Int, creatorIncentive: Int, voterIncentive: Int = 45) {
        if (partnerIncentive != incentivePlan.partner
            || creatorIncentive != incentivePlan.creator
            || voterIncentive != incentivePlan.voter) {
            incentiveCanUpdate = true

            this.inputPartnerIncentive = partnerIncentive
            this.inputCreatorIncentive = creatorIncentive
            this.inputVoterIncentive = voterIncentive
            emitState()
        }
    }
}

data class NodeEditUiState (
        val wallet: Wallet,
        val name: String,
        val address: String,
        val enode: String,
        val desc: String,
        val addressError: Throwable?,
        val nameCanUpdate: Boolean = false,
        val addressCanUpdate: Boolean = false,
        val enodeCanUpdate: Boolean = false,
        val descCanUpdate: Boolean = false,
        val incentiveCanUpdate: Boolean = false,
        val existsEnode: Boolean = false,
        val existsNode: Boolean = false,
        val isFounder: Boolean = false,
        val isInputCurrentWalletAddress: Boolean = false,
)
