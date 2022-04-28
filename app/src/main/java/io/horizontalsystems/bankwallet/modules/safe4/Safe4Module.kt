package io.horizontalsystems.bankwallet.modules.safe4

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.anwang.safewallet.safekit.model.SafeNet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SafeConvertSendActivity
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SafeChainModel
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SafeNetModel
import io.horizontalsystems.marketkit.models.CoinType

object Safe4Module {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4ViewModel() as T
        }
    }

    fun handlerSafe2eth(safeNet: SafeNet) {
        val context = App.instance
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
            Toast.makeText(context, "请在钱包管理打开Safe", Toast.LENGTH_SHORT).show()
            return
        }
        if (wsafeWallet == null) {
            Toast.makeText(context, "请在钱包管理打开Ethereum", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent(context, SafeConvertSendActivity::class.java).apply {
            val chain = SafeChainModel(
                safeNet.eth.price,
                safeNet.eth.gas_price_gwei,
                safeNet.eth.safe_fee,
                safeNet.eth.safe2eth,
                safeNet.eth.eth2safe)
            val safeNetModel = SafeNetModel(safeNet.safe_usdt, safeNet.minamount, chain)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(SafeConvertSendActivity.WALLET_SAFE, safeWallet)
            putExtra(SafeConvertSendActivity.WALLET_WSAFE, wsafeWallet)
            putExtra(SafeConvertSendActivity.SAFE_NET, safeNetModel)
        })
    }

    fun handlerEth2safe(safeNet: SafeNet, navController: NavController) {
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var wsafeWallet: Wallet? = null
        for (it in walletList) {
            if (it.coinType == CoinType.Safe) {
                safeWallet = it
            } else if(it.coin.uid == "custom_safe-erc20-SAFE") {
                wsafeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, "请在钱包管理打开SAFE", Toast.LENGTH_SHORT).show()
            return
        }
        if (wsafeWallet == null) {
            Toast.makeText(context, "请在钱包管理打开SAFE ERC20", Toast.LENGTH_SHORT).show()
            return
        }
        val chain = SafeChainModel(
            safeNet.eth.price,
            safeNet.eth.gas_price_gwei,
            safeNet.eth.safe_fee,
            safeNet.eth.safe2eth,
            safeNet.eth.eth2safe)
        val safeNetModel = SafeNetModel(safeNet.safe_usdt, safeNet.minamount, chain)
        val bundle = Bundle()
        bundle.putParcelable(SafeConvertSendActivity.WALLET_SAFE, safeWallet)
        bundle.putParcelable(SafeConvertSendActivity.WALLET_WSAFE, wsafeWallet)
        bundle.putParcelable(SafeConvertSendActivity.SAFE_NET, safeNetModel)
        navController.slideFromBottom(
            R.id.mainFragment_to_sendWsafeFragment,
            bundle
        )
    }

}
