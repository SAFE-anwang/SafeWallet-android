package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

data class RevokeConnectInfo(
    var walletAddress: String,
    var chainId: Int,
    var selectedAccountId: String
)
