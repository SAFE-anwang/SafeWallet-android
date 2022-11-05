package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.privatekey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey.RestorePrivateKeyModule
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.reactivex.BackpressureStrategy

class PrivateKeyImportViewModel(
    private val service: RestorePrivateKeyService
): ViewModel() {

    val restoreEnabledLiveData: LiveData<Boolean>
        get() = LiveDataReactiveStreams.fromPublisher(
            service.canRestore.toFlowable(BackpressureStrategy.DROP)
        )

    var keyInvalidState = SingleLiveEvent<String?>()

    val successLiveEvent = SingleLiveEvent<Unit>()

    fun resolveAccountType(text: String): AccountType? {
        keyInvalidState.value = null
        return try {
            accountType(text)
        } catch (e: Throwable) {
            keyInvalidState.value = Translator.getString(R.string.Restore_PrivateKey_InvalidKey)
            null
        }
    }

    private fun accountType(text: String): AccountType {
        val textCleaned = text.trim()

        if (textCleaned.isEmpty()) {
            throw RestorePrivateKeyModule.RestoreError.EmptyText
        }

        try {
            val extendedKey = HDExtendedKey(textCleaned)
            if (!extendedKey.info.isPublic) {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master,
                    HDExtendedKey.DerivedType.Account -> {
                        return AccountType.HdExtendedKey(extendedKey.serializePrivate())
                    }
                    else -> throw RestorePrivateKeyModule.RestoreError.NotSupportedDerivedType
                }
            } else {
                throw RestorePrivateKeyModule.RestoreError.NonPrivateKey
            }
        } catch (e: Throwable) {
            //do nothing
        }

        try {
            val privateKey = Signer.privateKey(text)
            return AccountType.EvmPrivateKey(privateKey)
        } catch (e: Throwable) {
            //do nothing
        }

        throw RestorePrivateKeyModule.RestoreError.NoValidKey
    }

    fun onRestore(accountType: AccountType) {
        service.restore(accountType)
        successLiveEvent.call()
    }

    fun enable(type: String) {
        val blockchain = when(type) {
            "ETH" -> RestoreBlockchainsModule.Blockchain.Ethereum
            "BTC" -> RestoreBlockchainsModule.Blockchain.Bitcoin
            else -> {
                RestoreBlockchainsModule.Blockchain.Ethereum
            }
        }
        service.enable(blockchain)
    }

    fun disable(type: String) {
        val blockchain = when(type) {
            "ETH" -> RestoreBlockchainsModule.Blockchain.Ethereum
            "BTC" -> RestoreBlockchainsModule.Blockchain.Bitcoin
            else -> {
                RestoreBlockchainsModule.Blockchain.Ethereum
            }
        }
        service.disable(blockchain)
    }

}