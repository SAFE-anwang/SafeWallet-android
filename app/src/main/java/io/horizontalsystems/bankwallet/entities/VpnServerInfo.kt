package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import androidx.room.Entity
import com.v2ray.ang.dto.EConfigType
import kotlinx.android.parcel.Parcelize

@Entity(primaryKeys = ["address"])
data class VpnServerInfo(
    val address: String,
    val port: Int,
    val clientId: String,
    val alterId: String = "0",
    val protocol: String = "ws",
    val camouflageType: String = "none",
    val type: EConfigType = EConfigType.VMESS,
    val securitys: String = "auto"
) {

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun toString(): String {
        return "VpnServerInfo(address='$address', port=$port, clientId='$clientId', alterId='$alterId', protocol='$protocol', camouflageType='$camouflageType', type=$type, securitys='$securitys')"
    }


}
