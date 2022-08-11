package io.horizontalsystems.bankwallet.core.providers

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.entities.Currency
import java.util.*

class AppConfigProvider(val index: Int) {

//     val companyWebPageLink: String = "https://horizontalsystems.io"
     val companyWebPageLink: String = "https://www.anwang.com"
//     val appWebPageLink: String = "https://unstoppable.money"
     val appWebPageLink: String = "https://www.anwang.com"
//     val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
     val appGithubLink: String = "https://github.com/SAFE-anwang/SafeWallet-android"
//     val appTwitterLink: String = "https://twitter.com/UnstoppableByHS"
     val appTwitterLink: String = "https://twitter.com/safeanwang"
//     val appTelegramLink: String = "https://t.me/unstoppable_announcements"
    val appTelegramLink: String = "https://t.me/safeanwang"
//     val appRedditLink: String = "https://www.reddit.com/r/UNSTOPPABLEWallet/"
    val appRedditLink: String = "https://www.reddit.com/user/safe_2018"
//     val reportEmail = "support.unstoppable@protonmail.com"
    val reportEmail = "foundation@anwang.com"
     val btcCoreRpcUrl: String = "https://btc.horizontalsystems.xyz/rpc"
     val releaseNotesUrl: String = "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/"
     val walletConnectUrl = "relay.walletconnect.com"


    val safeBlockExplorer = "https://chain.anwang.com"
    val safeAcrossChainExplorer = "https://anwang.com/assetgate.html"
    val safeCoinGecko = "https://www.coingecko.com/en/coins/safe"
    val safeSafeBEP20 = "https://www.coingecko.com/en/coins/safe-anwang"
    val safeCoinMarketCap = "https://coinmarketcap.com/currencies/safe"
    val supportEmail = "mailto:support@anwang.com"

     val walletConnectProjectId by lazy {
         Translator.getString(R.string.walletConnectV2Key)
     }

     val twitterBearerToken by lazy {
        Translator.getString(R.string.twitterBearerToken)
    }
     val cryptoCompareApiKey by lazy {
        Translator.getString(R.string.cryptoCompareApiKey)
    }
     val defiyieldProviderApiKey by lazy {
        Translator.getString(R.string.defiyieldProviderApiKey)
    }
     val infuraProjectId by lazy {
         val projectId = when(index) {
             0 -> Translator.getString(R.string.infuraProjectId)
             1 -> infuraProjectId2
             else -> infuraProjectId3
         }
        projectId
    }
     val infuraProjectSecret by lazy {
         val projectKey = when(index) {
             0 -> Translator.getString(R.string.infuraSecretKey)
             1 -> infuraProjectSecret2
             else -> infuraProjectSecret3
         }
         projectKey
    }

    val infuraProjectId2 by lazy {
        Translator.getString(R.string.infuraProjectId2)
    }
    val infuraProjectSecret2 by lazy {
        Translator.getString(R.string.infuraSecretKey2)
    }

    val infuraProjectId3 by lazy {
        Translator.getString(R.string.infuraProjectId3)
    }
    val infuraProjectSecret3 by lazy {
        Translator.getString(R.string.infuraSecretKey3)
    }

     val etherscanApiKey by lazy {
        Translator.getString(R.string.etherscanKey)
    }
     val bscscanApiKey by lazy {
        Translator.getString(R.string.bscscanKey)
    }
     val guidesUrl by lazy {
        Translator.getString(R.string.guidesUrl)
    }
     val faqUrl by lazy {
        Translator.getString(R.string.faqUrl)
    }
     val coinsJsonUrl by lazy {
        Translator.getString(R.string.coinsJsonUrl)
    }
     val providerCoinsJsonUrl by lazy {
        Translator.getString(R.string.providerCoinsJsonUrl)
    }

     val marketApiBaseUrl by lazy {
        Translator.getString(R.string.marketApiBaseUrl)
    }

    val marketApiKey by lazy {
        Translator.getString(R.string.marketApiKey)
    }

     val fiatDecimal: Int = 2
     val maxDecimal: Int = 8
     val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

    val currencies: List<Currency> = listOf(
        Currency("AUD", "A$", 2),
        Currency("BRL", "R$", 2),
        Currency("CAD", "C$", 2),
        Currency("CHF", "₣", 2),
        Currency("CNY", "¥", 2),
        Currency("EUR", "€", 2),
        Currency("GBP", "£", 2),
        Currency("HKD", "HK$", 2),
        Currency("ILS", "₪", 2),
        Currency("JPY", "¥", 2),
        Currency("RUB", "₽", 2),
        Currency("SGD", "S$", 2),
        Currency("USD", "$", 2),
    )

    val safeTwitterUser = "safeanwang"
}
