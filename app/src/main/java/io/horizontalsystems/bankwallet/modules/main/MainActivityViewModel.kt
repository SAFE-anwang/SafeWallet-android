package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.DAppRequestEntityWrapper
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.safeprice.SRC20InfoService
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationError
import io.horizontalsystems.dapp.core.HSDAppEvent
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect

class MainActivityViewModel(
    private val userManager: UserManager,
    private val accountManager: IAccountManager,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    private val localStorage: ILocalStorage,
    private val tonConnectManager: TonConnectManager
) : ViewModel() {

    val navigateToMainLiveData = MutableLiveData(false)
    val wcEvent = MutableLiveData<HSDAppEvent?>()
    val tcSendRequest = MutableLiveData<SignTransaction?>()
    val tcDappRequest = MutableLiveData<DAppRequestEntityWrapper?>()
    val intentLiveData = MutableLiveData<Intent?>()

    init {
        viewModelScope.launch {
            userManager.currentUserLevelFlow.collect {
                navigateToMainLiveData.postValue(true)
            }
        }
        viewModelScope.launch {
            WCDelegate.walletEvents.collect {
                wcEvent.postValue(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.sendRequestFlow.collect {
                tcSendRequest.postValue(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.dappRequestFlow.collect {
                tcDappRequest.postValue(it)
            }
        }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow.collect {
                LockRecordManager.switchWallet()
            }
        }
        updateSRC20Price()
    }

    fun onWcEventHandled() {
        wcEvent.postValue(null)
    }

    fun reEmitPendingWcProposalIfNeeded() {
        if (wcEvent.value == null && WCDelegate.sessionProposalEvent != null) {
            wcEvent.postValue(HSDAppEvent.SessionProposal(WCDelegate.sessionProposalEvent!!))
        }
    }

    fun onTcSendRequestHandled() {
        tcSendRequest.postValue(null)
    }

    fun onTcDappRequestHandled() {
        tcDappRequest.postValue(null)
    }

    fun validate() {
        if (systemInfoManager.isSystemLockOff) {
            throw MainScreenValidationError.NoSystemLock()
        }

        try {
            keyStoreManager.validateKeyStore()
        } catch (e: KeyStoreValidationError.UserNotAuthenticated) {
            throw MainScreenValidationError.UserAuthentication()
        } catch (e: KeyStoreValidationError.KeyIsInvalid) {
            throw MainScreenValidationError.KeyInvalidated()
        } catch (e: RuntimeException) {
            throw MainScreenValidationError.KeystoreRuntimeException()
        }

        if (accountManager.isAccountsEmpty && !localStorage.mainShowedOnce) {
            throw MainScreenValidationError.Welcome()
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(false)
    }

    fun setIntent(intent: Intent) {
        intentLiveData.postValue(intent)
    }

    fun intentHandled() {
        intentLiveData.postValue(null)
    }

    private fun updateSRC20Price() {
        viewModelScope.launch(Dispatchers.IO) {
            val service = SRC20InfoService()
            service.getPrice()
            service.itemsObservable.collect {
                val prices = it.map {
                    CoinPrice(
                        "custom-safe4-coin|eip20:${it.address.lowercase()}", "USD", it.price.toBigDecimal(), it.change.toBigDecimal(), it.change.toBigDecimal(), System.currentTimeMillis()/1000
                    )
                }
                App.marketKit.saveCoinPrice(prices)
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(
                App.userManager,
                App.accountManager,
                App.systemInfoManager,
                App.keyStoreManager,
                App.localStorage,
                App.tonConnectManager,
            ) as T
        }
    }
}

sealed class MainScreenValidationError : Exception() {
    class Welcome : MainScreenValidationError()
    class Unlock : MainScreenValidationError()
    class NoSystemLock : MainScreenValidationError()
    class KeyInvalidated : MainScreenValidationError()
    class UserAuthentication : MainScreenValidationError()
    class KeystoreRuntimeException : MainScreenValidationError()
}
