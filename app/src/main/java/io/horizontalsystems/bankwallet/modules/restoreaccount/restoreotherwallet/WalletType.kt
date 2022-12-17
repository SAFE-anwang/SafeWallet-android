package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import kotlinx.android.parcel.Parcelize

sealed class WalletType: Parcelable {

    @Parcelize
    object HD: WalletType()

    @Parcelize
    object SafeWallet: WalletType()

    @Parcelize
    object SafeGem: WalletType()

    @Parcelize
    object ImToken: WalletType()

    @Parcelize
    object Bither: WalletType()

    @Parcelize
    object TokenPocket: WalletType()

}

val WalletType.name: Int
    get() = when(this) {
        WalletType.HD -> R.string.Restore_HD_Wallet
        WalletType.SafeWallet -> R.string.Restore_Safe_Wallet
        WalletType.SafeGem -> R.string.Restore_SafeGem_Wallet
        WalletType.ImToken -> R.string.Restore_ImToken_Wallet
        WalletType.Bither -> R.string.Restore_BitPie_Wallet
        WalletType.TokenPocket -> R.string.Restore_Token_Pocket_Wallet
    }


val WalletType.icon: Int
    get() = when(this) {
        WalletType.HD -> R.drawable.logo_safe_24
        WalletType.SafeWallet -> R.drawable.logo_safe_24
        WalletType.SafeGem -> R.drawable.logo_safe_24
        WalletType.ImToken -> R.drawable.logo_safe_24
        WalletType.Bither -> R.drawable.logo_safe_24
        WalletType.TokenPocket -> R.drawable.logo_safe_24
    }