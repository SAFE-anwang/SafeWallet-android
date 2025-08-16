package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RevokeConnectInfo(
    var walletAddress: String,
    var chainId: Int,
    var selectedAccountId: String,
    var isConnect: Boolean = false
): Parcelable {

}
