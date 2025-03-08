package io.horizontalsystems.bankwallet.modules.swap.liquidity.confirmation

import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap.UniswapConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.send.AddLiquidityTransactionViewModel
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class LiquidityConfirmationFragment(
    override val navGraphId: Int = R.id.liquidityConfirmationFragment
) : BaseAddLiquidityConfirmationFragment() {

    companion object {
        private const val transactionDataKey = "transactionDataKey"
        private const val dexKey = "dexKey"
        private const val additionalInfoKey = "additionalInfoKey"
        private const val tokenKey = "token"

        fun prepareParams(
            dex: SwapMainModule.Dex,
            transactionData: SendEvmModule.TransactionDataParcelable,
            additionalInfo: SendEvmData.AdditionalInfo?,
            token: Token?
        ) = bundleOf(
            dexKey to dex,
            transactionDataKey to transactionData,
            additionalInfoKey to additionalInfo,
            tokenKey to token
        )
    }

    private val input by lazy {
        arguments?.getInputX<Input>()!!
    }

    private val dex by lazy {
        input.dex
    }

    private val transactionData: TransactionData
        get() = input.transactionData


    private val additionalInfo by lazy {
        input.additionalInfo
    }

    private val token by lazy {
        input.token
    }

    override val logger = AppLogger("add-liquidity")

    private val vmFactory by lazy {
        UniswapConfirmationModule.Factory(
            dex,
            transactionData,
            additionalInfo,
            token
        )
    }
    override val sendEvmTransactionViewModel by navGraphViewModels<AddLiquidityTransactionViewModel>(navGraphId) { vmFactory }
//    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
//    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }

    @Parcelize
    data class Input(
            val dex: SwapMainModule.Dex,
            val transactionDataParcelable: SendEvmModule.TransactionDataParcelable,
            val additionalInfo: SendEvmData.AdditionalInfo?,
            val token: Token? = null
    ) : Parcelable {
        val transactionData: TransactionData
            get() = TransactionData(
                    Address(transactionDataParcelable.toAddress),
                    transactionDataParcelable.value,
                    transactionDataParcelable.input,
                    transactionDataParcelable.lockTime,
                    transactionDataParcelable.isBothErc
            )

        constructor(sendEvmData: SendEvmData, dex: SwapMainModule.Dex, token: Token) :
                this(
                        dex,
                        SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
                        sendEvmData.additionalInfo,
                        token
                )
    }
}
