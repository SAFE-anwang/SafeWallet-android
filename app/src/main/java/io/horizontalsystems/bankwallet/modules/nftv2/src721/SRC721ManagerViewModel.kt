package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftAssetsService
import io.horizontalsystems.bankwallet.core.providers.nft.Safe4NftToken
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
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
            val removed = SRC721Storage.removedAddresses()

            // 管理列表数据来自 insight 接口：每次刷新都重新请求（force=true），
            // 不复用上次的索引缓存；请求失败时回退到最近一次成功的索引，
            // 避免一次网络抖动就把列表清空。
            val apiTokens = Safe4NftAssetsService.fetchTokens(force = true)
                ?: Safe4NftAssetsService.cachedTokens()
            val apiAddresses = apiTokens.map { it.address.lowercase() }.toSet()

            // 接口数据：顺序沿用接口返回顺序的倒序，只取 creator 为本账户的合约
            val fromApi = apiTokens.asReversed()
                .filter { it.creator.equals(creator, ignoreCase = true) }
                .mapNotNull { apiToken ->
                    val address = apiToken.address
                    if (address.isBlank() || address.lowercase() in removed) return@mapNotNull null

                    buildItem(
                        SRC721ContractInfo(
                            address = address,
                            name = apiToken.displayName.orEmpty(),
                            symbol = apiToken.symbol.orEmpty(),
                            // 接口不下发 burnable，从链上确认，否则「销毁」按钮不会出现
                            burnable = false,
                            creator = creator,
                        ),
                        apiToken,
                    )
                }

            // 本地注册表里「接口没有、链上也不存在」的残留记录顺手清掉
            pruneMissingLocalRecords(apiAddresses)

            withContext(Dispatchers.Main) {
                // 管理列表只展示接口数据：本地注册表不再参与渲染
                items = fromApi
                emitState()
            }
        }.invokeOnCompletion {
            // 无论成功失败都要结束加载态，否则页面会一直停在加载动画、
            // 永远等不到「无数据」的默认提示
            viewModelScope.launch(Dispatchers.Main) {
                refreshing = false
                emitState()
            }
        }
    }

    /**
     * 组装列表行：确认可销毁标记、读取 logo 与供应数量。
     *
     * [apiToken] 为接口命中的合约信息；本地兜底记录（接口尚未收录）传 null。
     */
    private suspend fun buildItem(info: SRC721ContractInfo, apiToken: Safe4NftToken?): SRC721ManagerItem {
        val resolved = resolveBurnableFromChain(info)
        // 合约 logo 由用户在编辑页付费设置，列表直接展示
        val logoPath = SRC721LogoProvider.fetchPath(resolved.address)
        val counts = readCounts(resolved.address, apiToken)
        return SRC721ManagerItem(
            info = resolved,
            totalSupply = counts.minted,
            remainSupply = counts.remain,
            burnedTotal = counts.burned,
            loadFailed = counts.minted == null && counts.remain == null,
            logoPath = logoPath,
        )
    }

    /**
     * 确认合约是否可销毁（运行时代码是否含 `burn()`）。
     *
     * insight 接口不下发该字段，因此按链上判定；本地发布记录已标记可销毁时直接沿用。
     * 结果只用于本次渲染，不写回本地注册表，避免残留标记与链上实际不一致。
     */
    private fun resolveBurnableFromChain(info: SRC721ContractInfo): SRC721ContractInfo {
        if (info.burnable) return info

        val burnable = try {
            SRC721Service(web3j, info.address).isBurnable()
        } catch (e: Throwable) {
            Log.d(TAG, "resolveBurnableFromChain ${info.address} failed: $e")
            false
        }
        Log.d(TAG, "resolveBurnableFromChain ${info.address}: burnable=$burnable")
        return if (burnable) info.copy(burnable = true) else info
    }

    /**
     * 丢弃本地注册表中「接口没有、链上也不存在」的残留记录。
     *
     * 测试网重置、曾在另一条链部署等情况会让本地记录指向已不存在的合约，
     * 这类记录既不在 insight 索引里，也读不到链上数据（列表里表现为 `--`），
     * 这里从注册表清掉，避免继续污染合集名称兜底。
     *
     * 只删记录、不写「已删除」标记：合约之后若真的出现，接口数据仍会正常展示。
     */
    private fun pruneMissingLocalRecords(apiAddresses: Set<String>) {
        SRC721Storage.list(creator)
            .filter { it.address.lowercase() !in apiAddresses }
            .forEach { info ->
                // hasCode 读取异常时按「存在」处理，避免误删本地记录
                if (SRC721Service(web3j, info.address).hasCode()) return@forEach
                Log.d(TAG, "pruneMissingLocalRecords: drop ${info.address} (${info.name})")
                SRC721Storage.dropRecord(info.address)
            }
    }

    /** 合约数量信息：已铸造 / 剩余可铸造 / 已销毁 */
    private data class SupplyCounts(
        val minted: String?,
        val remain: String?,
        val burned: String?,
    )

    /**
     * 读取「已铸造 / 剩余可铸造 / 已销毁」数量。
     *
     * - 已铸造：合约 `mintedTotal()`（累计铸造量，含已销毁的），旧合约没有该方法时回退 `totalSupply()`；
     * - 剩余：`remainSupply()`；
     * - 已销毁：`mintedTotal() - totalSupply()`；旧合约无 `mintedTotal()` 时无从计算，返回 null
     *   （界面显示 `--`，不显示成 0 以免误导）。
     *
     * 链上读取失败时回退 insight 接口的 [Safe4NftToken.totalAssets]（仅已铸造，其余为 null）。
     */
    private fun readCounts(address: String, apiToken: Safe4NftToken?): SupplyCounts {
        try {
            val service = SRC721Service(web3j, address)
            val mintedTotal = service.mintedTotal()
            val totalSupply = service.totalSupply()
            return SupplyCounts(
                minted = totalSupply.toString(),
                remain = service.remainSupply().toString(),
                // 理论上 mintedTotal >= totalSupply，这里兜底避免出现负数
                burned = mintedTotal?.let { (it - totalSupply).max(BigInteger.ZERO).toString() }
            )
        } catch (e: Throwable) {
            Log.d(TAG, "链上读取 $address 失败，回退接口数据: $e")
        }
        return SupplyCounts(
            minted = apiToken?.totalAssets?.toString(),
            remain = null,
            burned = null
        )
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
        viewModelScope.launch(Dispatchers.IO) {
            val service = SRC721Service(web3j, info.address)

            // 前置校验：ERC721 只允许持有者（或被授权地址）销毁。
            // 先链上读 ownerOf/getApproved，把「caller is not token owner or approved」
            // 这类原始 revert 提前拦截，转换为可理解的提示
            val checkErrorRes = try {
                val owner = service.ownerOf(tokenId)
                val approved = service.getApproved(tokenId)
                if (owner.equals(creator, true) || approved.equals(creator, true)) {
                    null
                } else {
                    R.string.Nft_Burn_Error_NotOwner
                }
            } catch (e: Throwable) {
                // ownerOf 对不存在的 tokenId 会直接 revert
                Log.d(TAG, "burn pre-check failed for #$tokenId: $e")
                R.string.Nft_Burn_Error_NotExist
            }
            if (checkErrorRes != null) {
                sendResult = SendResult.Failed(HSCaution(TranslatableString.ResString(checkErrorRes)))
                return@launch
            }

            service.burn(privateKey, tokenId)
                .subscribeIO({
                    sendResult = SendResult.Sent()
                    refresh()
                }, { e ->
                    // 校验通过到上链之间 token 可能被转移，仍可能 revert，做同样转换
                    sendResult = if (e.message?.contains("not token owner or approved", true) == true) {
                        SendResult.Failed(HSCaution(TranslatableString.ResString(R.string.Nft_Burn_Error_NotOwner)))
                    } else {
                        SendResult.Failed(NodeCovertFactory.createCaution(e))
                    }
                })
        }
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
