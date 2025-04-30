package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import com.anwang.utils.Safe4Contract
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.EvmLabelProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.storage.EvmAddressLabelDao
import io.horizontalsystems.bankwallet.core.storage.EvmMethodLabelDao
import io.horizontalsystems.bankwallet.core.storage.SyncerStateDao
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.EvmAddressLabel
import io.horizontalsystems.bankwallet.entities.EvmMethodLabel
import io.horizontalsystems.bankwallet.entities.SyncerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import java.util.Arrays
import java.util.concurrent.Executors

class EvmLabelManager(
    private val provider: EvmLabelProvider,
    private val addressLabelDao: EvmAddressLabelDao,
    private val methodLabelDao: EvmMethodLabelDao,
    private val syncerStateStorage: SyncerStateDao
) {
    private val keyMethodLabelsTimestamp = "evm-label-manager-method-labels-timestamp"
    private val keyAddressLabelsTimestamp = "evm-label-manager-address-labels-timestamp"

    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)

    fun sync() {
        coroutineScope.launch {
            try {
                val updatesStatus = provider.updatesStatus()
                syncMethodLabels(updatesStatus.evmMethodLabels)
                syncAddressLabels(updatesStatus.addressLabels)
            } catch (e: Exception) {
                Log.e("EvmLabelManager", "sync() error: ${e.message}", e)
            }
        }
    }

    fun methodLabel(input: ByteArray, to: String? = ""): String? {
        val methodId = input.take(4).toByteArray().toHexString()
        return when(methodId) {
            "0x03c4c7f3" -> Translator.getString(R.string.Method_Vote_Super_Node)
            "0xc54256ed" -> Translator.getString(R.string.Method_Create_Proposal)
            "0xb384abef" -> Translator.getString(R.string.Method_Create_Proposal_Vote)
            "0xa57afda4" -> {
                var isUnion = false
                try {
                    isUnion = NodeRegister.decodeCreateSuperNode(input.toRawHexString())[0].value as Boolean
                } catch (e: Exception) {
                }
                if (isUnion) {
                    Translator.getString(R.string.Method_Create_Super_Node_Union)
                } else {
                    Translator.getString(R.string.Method_Create_Super_Node)
                }
            }
            "0x082ed4d5" -> if (to == Safe4Contract.SuperNodeLogicContractAddr) {
                var isUnion = false
                try {
                    isUnion = NodeRegister.decodeCreateSuperNode(input.toRawHexString())[0].value as Boolean
                } catch (e: Exception) {
                }
                if (isUnion) {
                    Translator.getString(R.string.Method_Create_Super_Node_Union)
                } else {
                    Translator.getString(R.string.Method_Create_Super_Node)
                }
            } else {
                var isUnion = false
                try {
                    isUnion = NodeRegister.decodeCreateMasterNode(input.toRawHexString())[0].value as Boolean
                } catch (e: Exception) {
                }
                if (isUnion) {
                    Translator.getString(R.string.Method_Create_Master_Node_Union)
                } else {
                    Translator.getString(R.string.Method_Create_Master_Node)
                }
            }
            "0x7255acae" -> Translator.getString(R.string.Method_Change_Enode)
            "0x45ca25ed" -> Translator.getString(R.string.Method_Change_Name)
            "0x1ed6f423" -> Translator.getString(R.string.Method_Change_Desc)
            "0xefe08a7d" -> Translator.getString(R.string.Method_Change_Address)
            "0x978a11d1" -> if (to == Safe4Contract.SuperNodeLogicContractAddr)
                Translator.getString(R.string.Method_Join_Super_Node)
            else Translator.getString(R.string.Method_Join_Master_Node)
            "0x3ccfd60b" -> Translator.getString(R.string.Method_Withdraw)
            "0x092c8749" -> Translator.getString(R.string.Method_Vote_Super_Node)
            "0xcd9d6fca" -> Translator.getString(R.string.Method_Income)
            "0x8e5cd5ec",
            "0xdb5b287d",
            "0x2b56909b" -> Translator.getString(R.string.Method_Redeem_Available)
            "0xe70c2626",
            "0x3ecc9516",
            "0x8000e9a6" -> Translator.getString(R.string.Method_Redeem_MasterNode)
            "0xd885085f",
            "0x4c9e906a",
            "0x6d5b08d3" -> Translator.getString(R.string.Method_Redeem_Locked)
            "0xa6aa19d2" -> Translator.getString(R.string.Method_Node_Status_Upload)
            "0x60806040" -> Translator.getString(R.string.Method_Node_Contract_Deployment)
            "0x38e06620" -> Translator.getString(R.string.Method_Add_Lock_Day)
            "0xe8e33700",
            "0xf305d719" -> Translator.getString(R.string.Method_Add_Liquidity)
            "0x2e1a7d4d",
            "0xd0e30db0" -> Translator.getString(R.string.SAFE4_Swap_Tx_Title)
            else -> methodLabelDao.get(methodId.lowercase())?.label
        }
    }

    private fun addressLabel(address: String): String? {
        return addressLabelDao.get(address.lowercase())?.label
    }

    fun mapped(address: String): String {
        return addressLabel(address) ?: address.shorten()
    }

    private suspend fun syncAddressLabels(timestamp: Long) {
        val lastSyncTimestamp = syncerStateStorage.get(keyAddressLabelsTimestamp)?.value?.toLongOrNull()
        if (lastSyncTimestamp == timestamp) return

        val addressLabels = provider.evmAddressLabels()
        addressLabelDao.update(addressLabels.map { EvmAddressLabel(it.address.lowercase(), it.label) })

        syncerStateStorage.insert(SyncerState(keyAddressLabelsTimestamp, timestamp.toString()))
    }

    private suspend fun syncMethodLabels(timestamp: Long) {
        val lastSyncTimestamp = syncerStateStorage.get(keyMethodLabelsTimestamp)?.value?.toLongOrNull()
        if (lastSyncTimestamp == timestamp) return

        val methodLabels = provider.evmMethodLabels()
        methodLabelDao.update(methodLabels.map { EvmMethodLabel(it.methodId.lowercase(), it.label) })

        syncerStateStorage.insert(SyncerState(keyMethodLabelsTimestamp, timestamp.toString()))
    }

}
