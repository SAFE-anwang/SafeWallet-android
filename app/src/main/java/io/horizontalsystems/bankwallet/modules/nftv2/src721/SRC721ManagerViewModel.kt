package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftAssetsService
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftToken
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import java.math.BigInteger

private const val TAG = "SRC721ManagerViewModel"

class SRC721ManagerViewModel(
    private val web3j: Web3j,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<SRC721ManagerUiState>() {

    sealed class Dialog {
        data class Burn(val info: SRC721ContractInfo) : Dialog()
        data class Remove(val info: SRC721ContractInfo) : Dialog()
    }

    private var items: List<SRC721ManagerItem> = emptyList()
    private var refreshing = false

    var activeDialog by mutableStateOf<Dialog?>(null)
    var burnTokenId by mutableStateOf("")
    var sendResult by mutableStateOf<SendResult?>(null)
    var alert by mutableStateOf<SRC721ManagerAlert?>(null)

    private val creator: String
        get() = evmKitWrapper.evmKit.receiveAddress.hex

    private val privateKey: String
        get() = evmKitWrapper.signer!!.privateKey.toHexString()

    init {
        refresh()
    }

    override fun createState(): SRC721ManagerUiState {
        return SRC721ManagerUiState(
            list = items,
            refreshing = refreshing
        )
    }

    fun refresh() {
        refreshing = true
        emitState()
        viewModelScope.launch(Dispatchers.IO) {
            // 合约索引来自 insight：一次请求即可拿到名称与已铸造数量，
            // 避免逐个合约发链上调用（不兼容 SDK 的合约链上读取会直接失败）
            val apiTokens = Safe4NftAssetsService.fetchTokens()
                ?.associateBy { it.address.lowercase() }
                .orEmpty()

            val localByAddress = SRC721Storage.list(creator).associateBy { it.address.lowercase() }
            val removed = SRC721Storage.removedAddresses()

            // 本地注册表 ∪ 接口中 creator 为本账户的合约
            // （重装 App / 换设备后本地注册表会缺失，需要靠接口补全）
            val addresses = LinkedHashSet<String>()
            localByAddress.keys.forEach { addresses.add(it) }
            apiTokens.values
                .filter { it.creator.equals(creator, ignoreCase = true) }
                .forEach { addresses.add(it.address.lowercase()) }

            val loaded = addresses
                .filterNot { it in removed }
                .mapNotNull { address ->
                    val apiToken = apiTokens[address]
                    val local = localByAddress[address]
                    val info = local ?: apiToken?.let {
                        SRC721ContractInfo(
                            address = it.address,
                            name = it.displayName.orEmpty(),
                            symbol = it.symbol.orEmpty(),
                            // 接口不下发 burnable，只有本地部署记录才能确定，这里按不可销毁处理
                            burnable = false,
                            creator = creator,
                        )
                    } ?: return@mapNotNull null

                    // 本地名称为空时用接口补全
                    val resolved = if (info.name.isBlank()) {
                        apiToken?.displayName?.let { info.copy(name = it) } ?: info
                    } else {
                        info
                    }

                    // 合约 logo 由用户在编辑页付费设置，列表直接展示
                    val logoPath = SRC721LogoProvider.fetchPath(resolved.address)
                    val counts = readCounts(resolved.address, apiToken)
                    SRC721ManagerItem(
                        info = resolved,
                        totalSupply = counts.first,
                        remainSupply = counts.second,
                        loadFailed = counts.first == null && counts.second == null,
                        logoPath = logoPath,
                    )
                }

            withContext(Dispatchers.Main) {
                items = loaded
                refreshing = false
                emitState()
            }
        }
    }

    /**
     * 读取「已铸造 / 剩余可铸造」数量。
     *
     * 优先链上 SDK（能同时给出剩余量），失败时回退到 insight 接口的
     * [Safe4NftToken.totalAssets]。接口不提供剩余可铸造量，故第二项为 null。
     */
    private fun readCounts(address: String, apiToken: Safe4NftToken?): Pair<String?, String?> {
        try {
            val service = SRC721Service(web3j, address)
            return Pair(service.totalSupply().toString(), service.remainSupply().toString())
        } catch (e: Throwable) {
            Log.d(TAG, "链上读取 $address 失败，回退接口数据: $e")
        }
        return Pair(apiToken?.totalAssets?.toString(), null)
    }

    fun showBurnDialog(info: SRC721ContractInfo) {
        burnTokenId = ""
        activeDialog = Dialog.Burn(info)
    }

    fun showRemoveDialog(info: SRC721ContractInfo) {
        activeDialog = Dialog.Remove(info)
    }

    fun dismissDialog() {
        activeDialog = null
    }

    fun onEnterBurnTokenId(value: String) {
        burnTokenId = value.filter { it.isDigit() }
    }

    fun burn(info: SRC721ContractInfo) {
        val tokenId = burnTokenId.toBigIntegerOrNull() ?: return
        dismissDialog()
        sendResult = SendResult.Sending
        SRC721Service(web3j, info.address)
            .burn(privateKey, tokenId)
            .subscribeIO({
                sendResult = SendResult.Sent()
                refresh()
            }, { e ->
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            })
    }

    /**
     * 删除本地合约记录。
     *
     * 只有链上已铸造的 NFT 全部销毁（totalSupply == 0）后才允许删除，
     * 否则合约下还存有 NFT，删除后无法再管理与销毁。
     */
    fun remove(info: SRC721ContractInfo) {
        dismissDialog()
        val item = items.firstOrNull { it.info.address.equals(info.address, ignoreCase = true) }
        val totalSupply = item?.totalSupply?.toBigIntegerOrNull()

        // 拉取失败（未拿到总量）时不允许删除，避免误删仍有 NFT 的合约
        if (totalSupply == null) {
            alert = SRC721ManagerAlert.RemoveNotAllowed
            return
        }
        if (totalSupply > BigInteger.ZERO) {
            alert = SRC721ManagerAlert.RemoveNotAllowed
            return
        }

        SRC721Storage.remove(info.address, creator)
        refresh()
    }

    fun dismissAlert() {
        alert = null
    }
}
