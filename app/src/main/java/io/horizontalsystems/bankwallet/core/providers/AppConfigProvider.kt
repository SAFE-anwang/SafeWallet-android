package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.net.ApiKeyUtil
import io.horizontalsystems.ethereumkit.models.Chain
import java.util.*
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class AppConfigProvider(val index: Int, localStorage: ILocalStorage) {

    val appId by lazy { localStorage.appId }
    val appVersion by lazy { BuildConfig.VERSION_NAME }
    val appBuild by lazy { BuildConfig.VERSION_CODE }
    val companyWebPageLink by lazy { Translator.getString(R.string.companyWebPageLink) }
    val appWebPageLink by lazy { Translator.getString(R.string.appWebPageLink) }
    val analyticsLink by lazy { Translator.getString(R.string.analyticsLink) }
    val appGithubLink by lazy { Translator.getString(R.string.appGithubLink) }
    val appTwitterLink by lazy { Translator.getString(R.string.appTwitterLink) }
    val appTelegramLink by lazy { Translator.getString(R.string.appTelegramLink) }
    val reportEmail by lazy { Translator.getString(R.string.reportEmail) }
    val releaseNotesUrl by lazy { Translator.getString(R.string.releaseNotesUrl) }
    val mempoolSpaceUrl: String = "https://mempool.space"
    val blockCypherUrl: String = "https://api.blockcypher.com"
    val walletConnectUrl = "relay.walletconnect.com"
    val walletConnectProjectId by lazy {
        ApiKeyUtil.getApiKey("walletConnectV2")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        when(index) {
            0 -> Translator.getString(R.string.walletConnectV2Key)
            else -> Translator.getString(R.string.walletConnectV2Key1)
        }
    }
    val walletConnectAppMetaDataName by lazy { Translator.getString(R.string.walletConnectAppMetaDataName) }
    val walletConnectAppMetaDataUrl by lazy { Translator.getString(R.string.walletConnectAppMetaDataUrl) }
    val walletConnectAppMetaDataIcon by lazy { Translator.getString(R.string.walletConnectAppMetaDataIcon) }
    val accountsBackupFileSalt by lazy { Translator.getString(R.string.accountsBackupFileSalt) }
    val simplexSupportChat = "https://smp11.simplex.im/g#yTrDh716RZCNYsdPSDrqMMlHnqZlW4XJGnFTugBrsAI"
    val nymVpnLink = "https://nymtechnologies.pxf.io/N9vnr1"
    val telegramSupportChat = "https://t.me/m/1TNZ9JE4MTNi"

    val safeBlockExplorer = "https://chain.anwang.com"
    val safeAcrossChainExplorer = "https://anwang.com/assetgate.html"
    val safe4BlockExplorer = "https://safe4.anwang.com"
    val safe4AcrossChainExplorer = "https://safe4.anwang.com/crosschains"
    val safeCoinGecko = "https://www.coingecko.com/en/coins/safe-anwang"
    val safeSafeBEP20 = "https://coinmarketcap.com/currencies/safe-anwang"
    val safeCoinMarketCap = "https://coinmarketcap.com/currencies/safe"
    val supportEmail = "mailto:support@anwang.com"
    val safeEthContract = "https://etherscan.io/token/0xEE9c1Ea4DCF0AAf4Ff2D78B6fF83AA69797B65Eb"
    val safeEthUniswap = "https://v2.info.uniswap.org/pair/0x8b04fdc8e8d7ac6400b395eb3f8569af1496ee33"
    val safeBSCContract = "https://bscscan.com/token/0x4d7fa587ec8e50bd0e9cd837cb4da796f47218a1"
    val safeBSCPancakeswap = "https://pancakeswap.finance/info/pairs/0x400db103af7a0403c9ab014b2b73702b89f6b4b7"
    val safeMaticContract = "https://polygonscan.com/address/0xb7dd19490951339fe65e341df6ec5f7f93ff2779"

    val safe4EthContract = "https://etherscan.io/token/0x96f59C9D155d598d4f895F07dd6991cCB5FA7DC7"
    val safe4BSCContract = "https://bscscan.com/token/0x3a5557AD6FA16699dD56fd0E418C70c83e42240a"
    val safe4MaticContract = "https://polygonscan.com/address/0xe0D3ff9b473976855B2242a1A022aC66f980Ce50"
    val safe4UsdtContract = "https://safe4.anwang.com/address/0x9C1246a4BB3c57303587e594a82632c3171662C9"

    private val testSafe4Api = "https://safe4testnet.anwang.com/api/"
    private val mainSafe4Api = "https://safe4.anwang.com/api/"

    val safe4Api by lazy {
        if (Chain.SafeFour.isSafe4TestNetId) {
            testSafe4Api
        } else {
            mainSafe4Api
        }
    }

    val blocksDecodedEthereumRpc by lazy {
        ApiKeyUtil.getEthRpcEndpoint() ?: Translator.getString(R.string.blocksDecodedEthereumRpc)
    }
    val twitterBearerToken by lazy {
        ApiKeyUtil.getApiKey("twitterBearerToken")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.twitterBearerToken)
    }

    val infuraApiKey by  lazy {
        ApiKeyUtil.getApiKey("infura")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        return@lazy null
    }

    val infuraProjectId by lazy {
        infuraApiKey?.split("#")?.get(0) ?: when(index) {
            0 -> Translator.getString(R.string.infuraProjectId)
//            1 -> infuraProjectId2
            else -> infuraProjectId3
        }
    }
    val infuraProjectSecret by lazy {
        infuraApiKey?.split("#")?.get(1) ?:  when(index) {
            0 -> Translator.getString(R.string.infuraSecretKey)
//            1 -> infuraProjectSecret2
            else -> infuraProjectSecret3
        }
    }

    /*val infuraProjectId2 by lazy {
        Translator.getString(R.string.infuraProjectId2)
    }
    val infuraProjectSecret2 by lazy {
        Translator.getString(R.string.infuraSecretKey2)
    }*/

    val infuraProjectId3 by lazy {
        Translator.getString(R.string.infuraProjectId3)
    }
    val infuraProjectSecret3 by lazy {
        Translator.getString(R.string.infuraSecretKey3)
    }

    val cacheEtherscanApiKey by lazy {
        ApiKeyUtil.getApiKey("etherscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        return@lazy null
    }

    val etherscanApiKey by lazy {
        if (cacheEtherscanApiKey != null) {
            cacheEtherscanApiKey!!
        } else {
            val index = Random().nextInt(2)
            when (index) {
                0 -> Translator.getString(R.string.etherscanKey)
                else -> Translator.getString(R.string.etherscanKey1)
            }
        }
    }

    val cacheBscscanApiKey by lazy {
        ApiKeyUtil.getApiKey("bscscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        return@lazy null
    }

    val bscscanApiKey by lazy {
        if (cacheBscscanApiKey != null) {
            cacheBscscanApiKey!!
        } else {
            val index = Random().nextInt(6)
            when(index) {
                0 -> Translator.getString(R.string.bscscanKey)
                1 -> Translator.getString(R.string.bscscanKey2)
                2 -> Translator.getString(R.string.bscscanKey3)
                3 -> Translator.getString(R.string.bscscanKey4)
                4 -> Translator.getString(R.string.bscscanKey5)
                else -> Translator.getString(R.string.bscscanKey6)
            }
        }
    }


    val cachePolygonscanApiKey by lazy {
        ApiKeyUtil.getApiKey("polygonscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        return@lazy null
    }


    val polygonscanApiKey by lazy {
        if (cachePolygonscanApiKey != null) {
            cachePolygonscanApiKey!!
        } else {
            val index = Random().nextInt(4)
            val key = when (index) {
                0 -> Translator.getString(R.string.polygonscanKey)
                1 -> Translator.getString(R.string.polygonscanKey2)
                2 -> Translator.getString(R.string.polygonscanKey3)
                else -> Translator.getString(R.string.polygonscanKey4)
            }
            key
        }
    }
    val snowtraceApiKey by lazy {
        Translator.getString(R.string.snowtraceApiKey)
    }
    val optimisticEtherscanApiKey by lazy {
        ApiKeyUtil.getApiKey("optimisticEtherscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.optimisticEtherscanApiKey)
    }
    val arbiscanApiKey by lazy {
        ApiKeyUtil.getApiKey("arbiscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.arbiscanApiKey)
    }
    val gnosisscanApiKey by lazy {
        ApiKeyUtil.getApiKey("gnosisscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.gnosisscanApiKey)
    }
    val ftmscanApiKey by lazy {
        ApiKeyUtil.getApiKey("ftmscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.ftmscanApiKey)
    }

    val otherScanApiKey by lazy {
        Translator.getString(R.string.otherScanKey).split(",")
    }
    val guidesUrl by lazy {
        Translator.getString(R.string.guidesUrl)
    }
    val eduUrl by lazy {
        Translator.getString(R.string.eduUrl)
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

    val openSeaApiKey by lazy {
        Translator.getString(R.string.openSeaApiKey)
    }

    val solanaAlchemyApiKey by lazy {
//        Translator.getString(R.string.solanaAlchemyApiKey)
        ApiKeyUtil.getApiKey("solscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.solscanApiKey)
    }

    val solanaJupiterApiKey by lazy {
        Translator.getString(R.string.solanaJupiterApiKey)
    }

    val solscanApiKey by lazy {
        ApiKeyUtil.getApiKey("solscan")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        Translator.getString(R.string.solscanApiKey)
    }

    val trongridApiKeys: List<String> by lazy {
        Translator.getString(R.string.trongridApiKeys).split(",")
    }

    val udnApiKey by lazy {
        Translator.getString(R.string.udnApiKey)
    }

    val oneInchApiKey by lazy {
        ApiKeyUtil.getApiKey("oneInch")?.let {
            val index = Random().nextInt(it.size)
            return@lazy it.get(index)
        }
        val projectKey = when(index) {
            0 -> Translator.getString(R.string.oneInchApiKey)
            1 -> Translator.getString(R.string.oneInchApiKey2)
            else -> Translator.getString(R.string.oneInchApiKey3)
        }
        projectKey
    }

    val safeSwapv2Safe4Factory by lazy {
        Translator.getString(R.string.safeswapv2safe4factory)
    }

    val safeSwapv2Safe4CodeHash by lazy {
        Translator.getString(R.string.safeswapv2safe4codehash)
    }

    val safeSwapv2Safe4Router by lazy {
        Translator.getString(R.string.safeswapv2safe4router)
    }

    val fiatDecimal: Int = 2
    val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")
    val maxDecimal: Int = 8

    val currencies: List<Currency> = listOf(
        Currency("AUD", "A$", 2, R.drawable.icon_32_flag_australia),
        Currency("ARS", "$", 2, R.drawable.icon_32_flag_argentine),
        Currency("BRL", "R$", 2, R.drawable.icon_32_flag_brazil),
        Currency("CAD", "C$", 2, R.drawable.icon_32_flag_canada),
        Currency("CHF", "₣", 2, R.drawable.icon_32_flag_switzerland),
        Currency("CNY", "¥", 2, R.drawable.icon_32_flag_china),
        Currency("EUR", "€", 2, R.drawable.icon_32_flag_europe),
        Currency("GBP", "£", 2, R.drawable.icon_32_flag_england),
        Currency("HKD", "HK$", 2, R.drawable.icon_32_flag_hongkong),
        Currency("HUF", "Ft", 2, R.drawable.icon_32_flag_hungary),
        Currency("ILS", "₪", 2, R.drawable.icon_32_flag_israel),
        Currency("INR", "₹", 2, R.drawable.icon_32_flag_india),
        Currency("JPY", "¥", 2, R.drawable.icon_32_flag_japan),
        Currency("NOK", "kr", 2, R.drawable.icon_32_flag_norway),
        Currency("PHP", "₱", 2, R.drawable.icon_32_flag_philippine),
        Currency("RUB", "₽", 2, R.drawable.icon_32_flag_russia),
        Currency("SGD", "S$", 2, R.drawable.icon_32_flag_singapore),
        Currency("USD", "$", 2, R.drawable.icon_32_flag_usa),
        Currency("ZAR", "R", 2, R.drawable.icon_32_flag_south_africa),
    )

    val donateAddresses: Map<BlockchainType, String> by lazy {
        if (BuildConfig.FDROID_BUILD) {
            mapOf(
                BlockchainType.Bitcoin to "bc1qy3ekl877sll3pzw9ramknx0wgyxfhzlccv940y",
                BlockchainType.BitcoinCash to "bitcoincash:qq0uv2s4nd8g7htpnsp77pwjpfdyrvgncu3tfk252s",
                BlockchainType.ECash to "ecash:qznldf5phm5dgq6rd2dpxd4qcypm32dyqsgux0v94h",
                BlockchainType.Litecoin to "ltc1q8x0zcezuz82mhhmclpa2hv2jf8vry66ap08d2w",
                BlockchainType.Dash to "XcHq4AuSC2CMQuf8wLojDJ9QEFQJShzgvf",
                BlockchainType.Zcash to "zs1rqg09d8t6utx3znyrzw4jz0y3tdh633yqychhumsm4z69y4dcye9z9aghpvpygzgktnexwnzagl",
                BlockchainType.Ethereum to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.BinanceSmartChain to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Polygon to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Avalanche to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Optimism to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Base to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.ZkSync to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.ArbitrumOne to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Solana to "EKQVqxaXVJf1QaVUeNynKkJC7rT4abMnWtg5TtqY2S5F",
                BlockchainType.Gnosis to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Fantom to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Ton to "UQA94iEyQI0iVD0ssowbHGizBEY5uMm9tMz72IecYjA_nnZG",
                BlockchainType.Tron to "TXkwDeqz77793xYJqxCHuEPiqqj8B8Cf2Z",
                BlockchainType.Monero to "4B5Dc1VFUpsVxDu8d8y8r44FQAjfmaSL4c3SSydUATpJPGsMkV4qswkenLAY4g9wm98bsvskVZXgDWgW2jA1t31MNcCZ8AZ",
            ).toList().sortedBy { (key, _) -> key.order }.toMap()
        } else {
            mapOf(
                BlockchainType.Bitcoin to "bc1qy0dy3ufpup9eyeprnd8a6fe2scg2m4rr4peasy",
                BlockchainType.BitcoinCash to "bitcoincash:qqlwaf0vrvq722pta5jfc83m6cv7569nzya0ry6prk",
                BlockchainType.ECash to "ecash:qp9cqsjfttdv2x9y0el3ghk7xy4dy07p6saz7w2xvq",
                BlockchainType.Litecoin to "ltc1qtnyd4vq4yvu4g00jd3nl25w8qftj32dvfanyfx",
                BlockchainType.Dash to "XqCrPRKwBeW4pNPbNUTQTsnKQ626RNz4no",
                BlockchainType.Zcash to "zs1r9gf53xg3206g7wlhwwq7lcdrtzalepnvk7kwpm8yxr0z3ng0y898scd505rsekj8c4xgwddz4m",
                BlockchainType.Ethereum to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.BinanceSmartChain to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Polygon to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Avalanche to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Optimism to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Base to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.ZkSync to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.ArbitrumOne to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Solana to "ELFQmFXqdS6C1zVqZifs7WAmLKovdEPbWSnqomhZoK3B",
                BlockchainType.Gnosis to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Fantom to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Ton to "UQDgkDkU_3Mtujk2FukZEsiXV9pOhVzkdvvYH8es0tZylTZY",
                BlockchainType.Tron to "TXKA3SxjLsUL4n6j3v2h85fzb4V7Th6yh6",
                BlockchainType.Monero to "46ZLVbtaBZFBdztK3L2sJEEwhvKL9B5jbEjBtJWS5DLTAsBS7K4KBpHU3M738qvVcZ1ejUoFichxubCnHLwvGQnu2SWtoeK"
            ).toList().sortedBy { (key, _) -> key.order }.toMap()
        }
    }

    // coinCode -> min value
    val spamCoinValueLimits: Map<String, BigDecimal> = mapOf(
        "USDT" to BigDecimal("0.01"),
        "USDC" to BigDecimal("0.01"),
        "DAI" to BigDecimal("0.01"),
        "BUSD" to BigDecimal("0.01"),
        "EURS" to BigDecimal("0.01"),
        "BSC-USD" to BigDecimal("0.01"),
        "TRX" to BigDecimal("10"),
        "XLM" to BigDecimal("0.01"),
    )

    val chainalysisBaseUrl by lazy {
        Translator.getString(R.string.chainalysisBaseUrl)
    }

    val chainalysisApiKey by lazy {
        Translator.getString(R.string.chainalysisApiKey)
    }

    val hashDitBaseUrl by lazy {
        Translator.getString(R.string.hashDitBaseUrl)
    }

    val hashDitApiKey by lazy {
        Translator.getString(R.string.hashDitApiKey)
    }

    val uswapApiBaseUrl by lazy {
        Translator.getString(R.string.uswapApiBaseUrl)
    }

    val uswapApiKey by lazy {
        Translator.getString(R.string.uswapApiKey)
    }

    val safeTwitterUser = "safeanwang"
}
