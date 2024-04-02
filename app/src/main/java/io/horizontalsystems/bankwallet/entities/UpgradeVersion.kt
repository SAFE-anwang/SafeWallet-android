package io.horizontalsystems.bankwallet.entities

data class UpgradeVersion(
        val version: String,
        val versionCode: Int,
        val upgradeMsg: String,
        val url: String
) {
}