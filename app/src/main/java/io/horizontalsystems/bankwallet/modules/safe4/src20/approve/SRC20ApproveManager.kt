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

    private val TAG = "SRC20ApproveManager"
    var dispose: Disposable? = null

    val state = ApproveState(true, false, hint = R.string.Button_Approve)
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
            Log.d(TAG, "currentAllowance=$currentAllowance")
            if (currentAllowance.compareTo(requiredAmount) >= 0) {
                _stateFlow.update { it.copy(false) }
            } else {
                _stateFlow.update { it.copy(true, false, true, R.string.Approv_Button) }
            }
        } catch (e: Exception) {
            Log.d(TAG, "checkNeedApprove error=$e")
            _stateFlow.update { it.copy(error = e.message) }
        }
    }

    fun approve(requiredAmount: BigInteger) {
        try {
            Log.d(TAG, "requiredAmount=$requiredAmount")
            _stateFlow.update { it.copy(true, false, false, R.string.Approving) }
            val approveResult = approveService.approve(privateKey, spenderAddress, requiredAmount, address)
            Log.d(TAG, "approve result=$approveResult")
            if (approveResult.isNotBlank()) {
                checkTransactionStatus()
            } else {
                _stateFlow.update { it.copy(error = "批准失败") }
            }
        } catch (e: Exception) {
            Log.d(TAG, "approve error=$e")
            _stateFlow.update { it.copy(error = e.message) }
        }
    }

    private fun checkTransactionStatus() {
        val currentHeight = evmKit.lastBlockHeight ?: 0
        Log.d("SRC20ApproveManager", "currentHeight=$currentHeight")
        dispose?.dispose()
        dispose = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                Log.d("SRC20ApproveManager", "updateHeight=$it")
                if (it >= currentHeight + 1) {
                    _stateFlow.update { it.copy(false, true, false, R.string.Approving) }
                }
            }
    }

}

data class ApproveState(
    val needApprove: Boolean,
    val approveSuccess: Boolean,
    val canApprove: Boolean = false,
    val hint: Int = 0,
    val error: String? = null
)