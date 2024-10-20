package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.UnsupportedException
import io.horizontalsystems.marketkit.models.BlockchainType

object SendTransactionServiceFactory {
    fun create(blockchainType: BlockchainType): ISendTransactionService = when (blockchainType) {
        BlockchainType.SafeFour,
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Gnosis,
        BlockchainType.Fantom -> SendTransactionServiceEvm(blockchainType)

        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.Zcash,
        BlockchainType.BinanceChain,
        BlockchainType.Solana,
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Safe,
        BlockchainType.Dogecoin,
        is BlockchainType.Unsupported -> throw UnsupportedException("")
    }
}
