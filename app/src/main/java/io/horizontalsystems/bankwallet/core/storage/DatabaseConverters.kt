package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeIncentivePlan
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeMemberInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeStatus
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date

class DatabaseConverters {

    private val gson by lazy { Gson() }

    // BigDecimal

    @TypeConverter
    fun fromString(value: String?): BigDecimal? = try {
        value?.let { BigDecimal(it) }
    } catch (e: Exception) {
        null
    }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toPlainString()
    }

    // SecretString

    @TypeConverter
    fun decryptSecretString(value: String?): SecretString? {
        if (value == null) return null

        return try {
            SecretString(App.encryptionManager.decrypt(value))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretString(secretString: SecretString?): String? {
        return secretString?.value?.let { App.encryptionManager.encrypt(it) }
    }

    // SecretList

    @TypeConverter
    fun decryptSecretList(value: String?): SecretList? {
        if (value == null) return null

        return try {
            SecretList(App.encryptionManager.decrypt(value).split(","))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretList(secretList: SecretList?): String? {
        return secretList?.list?.joinToString(separator = ",")?.let {
            App.encryptionManager.encrypt(it)
        }
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun fromBlockchainType(blockchainType: BlockchainType): String {
        return blockchainType.uid
    }

    @TypeConverter
    fun toBlockchainType(string: String): BlockchainType {
        return BlockchainType.fromUid(string)
    }

    @TypeConverter
    fun fromNftUid(nftUid: NftUid): String {
        return nftUid.uid
    }

    @TypeConverter
    fun toNftUid(string: String): NftUid {
        return NftUid.fromUid(string)
    }

    @TypeConverter
    fun fromMap(v: Map<String, String?>): String {
        return gson.toJson(v)
    }

    @TypeConverter
    fun toMap(v: String): Map<String, String?> {
        return gson.fromJson(v, object : TypeToken<Map<String, String?>>() {}.type)
    }

    @TypeConverter
    fun fromBalanceData(v: BalanceData?): String? {
        return v?.serialize(gson)
    }

    @TypeConverter
    fun toBalanceData(v: String?): BalanceData? {
        v ?: return null

        return BalanceData.deserialize(v, gson)
    }

    @TypeConverter
    fun fromBigInteger(value: BigInteger?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toBigInteger(value: String?): BigInteger? {
        return value?.let { BigInteger(it) }
    }

    // List<NodeMemberInfo> 转换
    @TypeConverter
    fun fromNodeMemberInfo(list: List<NodeMemberInfo>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toNodeMemberInfo(json: String): List<NodeMemberInfo> {
        val type = object : TypeToken<List<NodeMemberInfo>>() {}.type
        return gson.fromJson(json, type)
    }

    // NodeIncentivePlan 转换
    @TypeConverter
    fun fromNodeIncentivePlan(plan: NodeIncentivePlan): String {
        return gson.toJson(plan)
    }

    @TypeConverter
    fun toNodeIncentivePlan(json: String): NodeIncentivePlan {
        return gson.fromJson(json, NodeIncentivePlan::class.java)
    }

    // Address 类型转换
    /*@TypeConverter
    fun fromAddress(address: Address): String {
        return address.hex
    }

    @TypeConverter
    fun toAddress(addressJson: String): Address {
        return Address(addressJson)
    }*/

    // NodeStatus 枚举转换
    @TypeConverter
    fun fromNodeStatus(status: NodeStatus): String {
        return NodeStatus.convert(status).toString()
    }

    @TypeConverter
    fun toNodeStatus(statusStr: String): NodeStatus {
        return NodeStatus.get(statusStr.toInt())
    }
}
