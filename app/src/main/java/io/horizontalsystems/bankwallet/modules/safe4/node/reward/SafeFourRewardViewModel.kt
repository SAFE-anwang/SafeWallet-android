package io.horizontalsystems.bankwallet.modules.safe4.node.reward

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.SafeFourProvider
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.ProposalStatus
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourLockedVoteService
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawService
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class SafeFourRewardViewModel(
    val address: String,
    private val provider: SafeFourProvider,
    val service: WithdrawService,
    private val connectivityManager: ConnectivityManager
) : ViewModelUiState<SafeFourRewardModule.SafeFourRewardUiState>()  {

    private val disposables = CompositeDisposable()

    private var rewards: List<RewardInfo>? = null

    private var showConfirmationDialog = false

    private var isWithdrawing = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)


    init {
        getRewards()
    }

    private fun getRewards() {
        viewModelScope.launch(Dispatchers.IO) {
            provider.getRewards(address).map { response ->
                response.result.distinctBy { it["date"] }.mapNotNull { info ->
                    val date = info.getValue("date").toString()
//                    val amount = info.getValue("amount").toBigInteger()
                    val amount = BigInteger(info.getValue("amount"))
                    RewardInfo(amount, date)
                }.reversed()
            }.subscribeOn(Schedulers.io())
                    .subscribe({
                        rewards = it
                        emitState()
                    }, {
                    }).let {
                        disposables.add(it)
                    }
        }
    }

    override fun createState() = SafeFourRewardModule.SafeFourRewardUiState(
        rewards?.map {
            val amount = NodeCovertFactory.valueConvert(it.amount)
            RewardViewItem(
                    App.numberFormatter.formatCoinFull(amount, "SAFE", 18),
                    it.date
            )
        },
        rewards?.isNotEmpty() ?: false,
        showConfirmationDialog
    )


    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun closeDialog() {
        showConfirmationDialog = false
        emitState()
    }

    fun showConfirmation() {
        if (isWithdrawing.get())	return
        showConfirmationDialog = true
        emitState()
    }

    fun withdraw() {
        closeDialog()
        viewModelScope.launch(Dispatchers.IO) {
            service.withdraw(listOf(0))
        }
    }


    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}



data class RewardViewItem(
        val amount: String,
        val date: String
)

data class RewardInfo(
        val amount: BigInteger,
        val date: String
)
