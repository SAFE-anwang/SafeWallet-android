package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.feeratekit.FeeRateKit
import io.horizontalsystems.feeratekit.model.FeeProviderConfig
import io.horizontalsystems.feeratekit.providers.BitcoinFeeProvider
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

class FeeRateProvider(appConfig: AppConfigProvider) {

    private val feeRateKit: FeeRateKit by lazy {
        FeeRateKit(
            FeeProviderConfig(
                ethEvmUrl = appConfig.blocksDecodedEthereumRpc,
                ethEvmAuth = null,
                bscEvmUrl = FeeProviderConfig.defaultBscEvmUrl(),
                mempoolSpaceUrl = appConfig.mempoolSpaceUrl,
                blockCypherUrl = appConfig.blockCypherUrl,
                torEnabled = App.torKitManager.isTorEnabled
            )
        )
    }

    suspend fun bitcoinFeeRate(): BitcoinFeeProvider.RecommendedFees {
        return feeRateKit.bitcoin()
    }

    suspend fun litecoinFeeRate(): BigInteger {
        return feeRateKit.litecoin()
    }

    suspend fun dogecoinFeeRate(): BigInteger {
        return BigInteger("4000")
    }

    suspend fun bitcoinCashFeeRate(): BigInteger {
        return feeRateKit.bitcoinCash()
    }

    suspend fun dashFeeRate(): BigInteger {
        return feeRateKit.dash()
    }

    fun safeFeeRate(): BigInteger {
        return BigInteger("10")
    }

}

class BitcoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override val feeRateChangeable = true

    override suspend fun getFeeRates(): FeeRates {
        val bitcoinFeeRate = feeRateProvider.bitcoinFeeRate()
        return FeeRates(bitcoinFeeRate.halfHourFee, bitcoinFeeRate.minimumFee)
    }
}

class LitecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.litecoinFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class DogecoinFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dogecoinFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class BitcoinCashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.bitcoinCashFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class DashFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        val feeRate = feeRateProvider.dashFeeRate()
        return FeeRates(feeRate.toInt())
    }
}

class ECashFeeRateProvider : IFeeRateProvider {
    override suspend fun getFeeRates(): FeeRates {
        return FeeRates(2)
    }
}

class SafeFeeRateProvider(private val feeRateProvider: FeeRateProvider) : IFeeRateProvider {
    override suspend fun getFeeRates() : FeeRates {
        val feeRate = feeRateProvider.safeFeeRate()
        return FeeRates(feeRate.toInt())
    }
}


data class FeeRates(
    val recommended: Int,
    val minimum: Int = 0,
)