package io.horizontalsystems.bankwallet.modules.configuredtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.assetUrl
import io.horizontalsystems.bankwallet.core.eip20TokenUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.jettonUrl
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.safe4.src20.SyncSafe4Tokens
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.marketkit.SafeExtend.isSafeFourCustomCoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class ConfiguredTokenInfoViewModel(
    private val token: Token,
    private val accountManager: IAccountManager,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {

    val uiState: ConfiguredTokenInfoUiState

    init {
        val imageUrl = if (token.tokenQuery.customCoinUid.isSafeFourCustomCoin()) {
            SyncSafe4Tokens.getLogo(token.tokenQuery.customCoinUid) ?: ""
        } else {
            token.coin.imageUrl
        }
        val type = when (val type = token.type) {
            is TokenType.Eip20 -> {
                ConfiguredTokenInfoType.Contract(token.coin.uid, type.address, imageUrl, token.blockchain.eip20TokenUrl(type.address))
            }
            is TokenType.Spl -> {
                ConfiguredTokenInfoType.Contract(token.coin.uid, type.address, token.blockchain.type.imageUrl, token.blockchain.eip20TokenUrl(type.address))
            }
            is TokenType.Jetton -> {
                ConfiguredTokenInfoType.Contract(token.coin.uid, type.address, token.blockchain.type.imageUrl, token.blockchain.jettonUrl(type.address))
            }
            is TokenType.Asset -> {
                ConfiguredTokenInfoType.Contract(token.coin.uid,"${type.code}-${type.issuer}", token.blockchain.type.imageUrl, token.blockchain.assetUrl(type.code, type.issuer))
            }
            is TokenType.Derived -> {
                ConfiguredTokenInfoType.Bips(token.blockchain.name)
            }
            is TokenType.AddressTyped -> {
                ConfiguredTokenInfoType.Bch
            }
            TokenType.Native -> when (token.blockchainType) {
                BlockchainType.Monero,
                BlockchainType.Zcash -> {
                    ConfiguredTokenInfoType.BirthdayHeight(getBirthdayHeight(token))
                }
                else -> null
            }
            is TokenType.Unsupported -> null
        }

        uiState = ConfiguredTokenInfoUiState(
            iconSource = if (token.coin.isSafeCoin() && imageUrl.isEmpty()) ImageSource.Local(R.drawable.logo_safe_24) else ImageSource.Remote(imageUrl, token.iconPlaceholder),
            title = token.coin.code,
            subtitle = token.coin.name,
            tokenInfoType = type
        )
    }

    private fun getBirthdayHeight(token: Token): Long? {
        val account = accountManager.activeAccount ?: return null
        val restoreSettings = restoreSettingsManager.settings(account, token.blockchainType)

        return restoreSettings.birthdayHeight
    }

    class Factory(private val token: Token) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfiguredTokenInfoViewModel(
                token,
                App.accountManager,
                App.restoreSettingsManager
            ) as T
        }
    }

}

data class ConfiguredTokenInfoUiState(
    val iconSource: ImageSource,
    val title: String,
    val subtitle: String,
    val tokenInfoType: ConfiguredTokenInfoType?
)

sealed class ConfiguredTokenInfoType {
    data class Contract(
        val coinUid: String,
        val reference: String,
        val platformImageUrl: String,
        val explorerUrl: String?
    ) : ConfiguredTokenInfoType()

    data class Bips(val blockchainName: String): ConfiguredTokenInfoType()
    object Bch: ConfiguredTokenInfoType()
    data class BirthdayHeight(val height: Long?): ConfiguredTokenInfoType()
}