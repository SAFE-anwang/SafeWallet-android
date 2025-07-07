package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe.lockinfo

import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.modules.balance.token.TokenTransactionsService
import io.horizontalsystems.bankwallet.modules.safe4.linelocksafe.LineLockSafeModule
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.transactions.TransactionItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItemFactory
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.web3j.abi.FunctionReturnDecoder
import java.math.BigInteger

class LineLockInfoViewModel(
    val wallet: Wallet,
    private val transactionsService: TokenTransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory,
): ViewModelUiState<LineLockSafeModule.LineLockInfoUiState>() {

    private var lockInfos: List<LineLockSafeModule.LineLockInfo>? = null
    private var lockedAmount = BigInteger.ZERO
    private val disposables = CompositeDisposable()

    init {
        transactionsService.itemsObservable
            .subscribeIO {
                val filter = it.filter { it.record is ContractCallTransactionRecord }
                updateTransactions(filter)
                if (it.size == 20) {
                    transactionsService.loadNext()
                }
            }
            .let {
                disposables.add(it)
            }
        viewModelScope.launch(Dispatchers.IO) {
            transactionsService.start()
        }
    }

    override fun createState(): LineLockSafeModule.LineLockInfoUiState {
        return LineLockSafeModule.LineLockInfoUiState(
            NodeCovertFactory.formatSafe(lockedAmount),
            lockInfos
        )
    }

    private fun updateTransactions(items: List<TransactionItem>) {
        val transactions = items
            .map {
                transactionViewItem2Factory.convertToViewItemCached(it) }
        lockedAmount = BigInteger.ZERO
        val temp = mutableListOf<LineLockSafeModule.LineLockInfo>()
        transactions.forEach { transaction ->
            transaction.input?.let {
                val inputString = it.toHexString()
                val method = inputString.substring(0, 10)
                // lock
                if (method == "0x9c4ee6bf") {
                    val address = inputString.substring(34, 74)
                    val times = inputString.substring(75, 138).toInt()
                    val spaceDay = inputString.substring(139, 202).toInt(16)
                    val startDay = inputString.substring(203, inputString.length).toInt(16)
                    val value = transaction.vaule.divide(times.toBigInteger())
                    val formatValue = NodeCovertFactory.formatSafe(value)
                    lockedAmount += transaction.vaule
                    for(i in 1..times) {
                        temp.add(LineLockSafeModule.LineLockInfo(
                            formatValue,
                            (startDay + i * spaceDay) / 30,
                            "0x$address"
                        ))
                    }
                }
            }
        }

        lockInfos = temp
        emitState()
    }
}