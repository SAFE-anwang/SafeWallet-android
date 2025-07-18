package io.horizontalsystems.bankwallet.modules.safe4

import android.os.Parcelable
import androidx.room.Entity
import io.horizontalsystems.bankwallet.modules.safe4.src20.DeployType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(primaryKeys = ["address"])
data class CustomToken(
    val address: String,
    val symbol: String,
    val creator: String,
    val chainId: String,
    val decimals: String,
    val name: String,
    val type: Int,
    val logoURI: String = "",
    val version: String = ""
) : Parcelable {

    fun getDeployType(): DeployType {
        return DeployType.valueOf(type)
    }

    fun getTypeForVersion(): DeployType {
        return if (version.contains("SRC20-mintable")) {
            DeployType.SRC20Mintable
        } else if (version.contains("SRC20-burnable")) {
            DeployType.SRC20Burnable
        } else {
            DeployType.SRC20
        }
    }

}
