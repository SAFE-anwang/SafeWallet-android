package io.horizontalsystems.bankwallet.modules.swap.liquidity.confirmation.uniswap

import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendLiquidityEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.confirmation.BaseLiquidityConfirmationFragment
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token

class UniswapLiquidityConfirmationFragment(
    override val navGraphId: Int = R.id.liquidityConfirmationFragment
) : BaseLiquidityConfirmationFragment() {

    override val logger = AppLogger("swap_uniswap")

    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable =
                arguments?.getParcelable<SendLiquidityEvmModule.TransactionDataParcelable>(SendEvmModule.transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
        }

    private val additionalInfo: SendEvmData.AdditionalInfo?
        get() = arguments?.getParcelable(SendEvmModule.additionalInfoKey)

    private val token : Token?
        get() = arguments?.getParcelable(SendEvmModule.transactionToken)

    private val vmFactory by lazy {
        UniswapLiquidityConfirmationModule.Factory(
            dex.blockchainType,
            SendEvmData(transactionData, additionalInfo),
            token
        )
    }
    override val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }

}
