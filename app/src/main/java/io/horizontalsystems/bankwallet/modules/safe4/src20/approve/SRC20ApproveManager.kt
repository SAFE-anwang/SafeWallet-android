package io.horizontalsystems.bankwallet.modules.safe4.src20.approve

import android.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigInteger
import java.util.concurrent.Executors

class SRC20ApproveManager(
    val evmKit: EthereumKit,
    val privateKey: String?,
    val address: String,
    val contract: String
) {

    var dispose: Disposable? = null

    val state = ApproveState(true, false, R.string.Button_Approve)
    private val _stateFlow = MutableStateFlow(state)
    val stateFlow: StateFlow<ApproveState>
        get() = _stateFlow

    val approveService by lazy {
        SRC20ApproveService(RpcSource.safeFourHttp().uris[0].toString(), contract)
    }

    val spenderAddress by lazy {
        if (Chain.SafeFour.isSafe4TestNetId) {
            "0x4f203092FB68732D8484c099a72dDc5a195f26f9"
        } else {
            "0x6A6dFAF83cc1741FE08A9EFDea596dEad68f7420"
        }
    }

    fun checkNeedApprove(requiredAmount: BigInteger) {
        try {
            val currentAllowance = approveService.allowance(address, spenderAddress)
            Log.d("src20approve", "currentAllowance=$currentAllowance")
            if (currentAllowance.compareTo(requiredAmount) >= 0) {
                _stateFlow.update { it.copy(false) }
            } else {
//                val additionalAmount = requiredAmount.subtract(currentAllowance)
                Log.d("src20approve", "requiredAmount=$requiredAmount")
                val approveResult = approveService.approve(privateKey, spenderAddress, requiredAmount, address)
                if (approveResult.isNotBlank()) {
                    _stateFlow.update { it.copy(true, false, R.string.Approving) }
                    checkTransactionStatus()
                } else {
                    _stateFlow.update { it.copy(true, false, R.string.Approving) }
                }
            }
        } catch (e: Exception) {
            Log.d("checkNeedApprove", "error=$e")
            _stateFlow.update { it.copy(error = e.message) }
        }
    }

    private fun checkTransactionStatus() {
        val currentHeight = evmKit.lastBlockHeight ?: 0
        dispose?.dispose()
        dispose = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (it > currentHeight + 6) {
                    _stateFlow.update { it.copy(false, true, R.string.Approving) }
                }
            }
    }

}

data class ApproveState(
    val needApprove: Boolean,
    val approveSuccess: Boolean,
    val hint: Int = 0,
    val error: String? = null
)