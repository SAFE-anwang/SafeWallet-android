package io.horizontalsystems.bankwallet.modules.safe4

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SafeConvertSendActivity
import io.horizontalsystems.marketkit.models.CoinType

object Safe4Module {



    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4ViewModel() as T
        }
    }

    fun startSafe2wsafe(context: Activity) {
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var wsafeWallet: Wallet? = null
        for (it in walletList) {
            if (it.coinType == CoinType.Safe) {
                safeWallet = it
            } else if(it.coinType == CoinType.Ethereum) {
                wsafeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, "请在钱包管理打开SAFE", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wsafeWallet == null) {
            Toast.makeText(context, "请在钱包管理打开ETH", Toast.LENGTH_SHORT).show();
            return;
        }
        context.startActivity(Intent(context, SafeConvertSendActivity::class.java).apply {
            putExtra(SafeConvertSendActivity.WALLET_SAFE, safeWallet)
            putExtra(SafeConvertSendActivity.WALLET_WSAFE, wsafeWallet)
        })
    }

}
