package io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SafeNetModel(
    val safe_usdt: String,
    val minamount: String,
    val chain: SafeChainModel,
) : Parcelable

@Parcelize
data class SafeChainModel(
    val price: String,
    val gas_price_gwei: String,
    val safe_fee: String,
    val safe2eth: Boolean,
    val eth2safe: Boolean
)  : Parcelable


