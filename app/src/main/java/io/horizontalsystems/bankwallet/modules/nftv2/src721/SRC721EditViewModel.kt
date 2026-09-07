package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean

data class SRC721EditUiState(
    val orgName: String? = null,
    val officialUrl: String? = null,
    val whitePaperUrl: String? = null,
    val description: String? = null,
    val baseURI: String? = null,
    val mintPrice: String? = null,
    val maxSupply: String? = null,
    val logoFee: String? = null,
    val loading: Boolean = true,
    val hasUpdate: Boolean = false,
)

/**
 * SRC721 编辑页：合约 owner 可修改组织名、官网、白皮书、描述、baseURI、铸造价格、最大供应量、Logo。
 * 每个字段独立提交（只有发生变化的字段才会发送交易）。
 */
class SRC721EditViewModel(
    private val contract: SRC721ContractInfo,
    private val web3j: Web3j,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<SRC721EditUiState>() {

    private val disposables = CompositeDisposable()
    private val service = SRC721Service(web3j, contract.address)

    private var orgName: String? = null
    private var officialUrl: String? = null
    private var whitePaperUrl: String? = null
    private var description: String? = null
    private var baseURI: String? = null
    private var mintPrice: String? = null
    private var maxSupply: String? = null
    private var logoFee: String? = null
    private var loading = true

    private var orgNameUpdate = false
    private var officialUrlUpdate = false
    private var whitePaperUrlUpdate = false
    private var descriptionUpdate = false
    private var baseURIUpdate = false
    private var mintPriceUpdate = false
    private var maxSupplyUpdate = false

    private val isUpdating = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    private val privateKey: String
        get() = evmKitWrapper.signer!!.privateKey.toHexString()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                orgName = runCatching { service.orgName() }.getOrNull()
                officialUrl = runCatching { service.officialUrl() }.getOrNull()
                whitePaperUrl = runCatching { service.whitePaperUrl() }.getOrNull()
                description = runCatching { service.description() }.getOrNull()
                baseURI = runCatching { service.baseURI() }.getOrNull()
                mintPrice = runCatching { service.mintPrice() }.let { result ->
                    result.getOrNull()?.let { weiToSafe(it) }
                }
                maxSupply = runCatching { service.maxSupply() }.getOrNull()?.toString()
                logoFee = runCatching { service.getLogoPayAmount() }.getOrNull()?.let { weiToSafe(it) }
            } catch (e: Throwable) {
                // ignore
            }
            withContext(Dispatchers.Main) {
                loading = false
                emitState()
            }
        }
    }

    override fun createState(): SRC721EditUiState {
        return SRC721EditUiState(
            orgName = orgName,
            officialUrl = officialUrl,
            whitePaperUrl = whitePaperUrl,
            description = description,
            baseURI = baseURI,
            mintPrice = mintPrice,
            maxSupply = maxSupply,
            logoFee = logoFee,
            loading = loading,
            hasUpdate = orgNameUpdate || officialUrlUpdate || whitePaperUrlUpdate ||
                    descriptionUpdate || baseURIUpdate || mintPriceUpdate || maxSupplyUpdate
        )
    }

    fun setOrgName(value: String) {
        if (orgName != value) { orgNameUpdate = true; orgName = value; emitState() }
    }
    fun setOfficialUrl(value: String) {
        if (officialUrl != value) { officialUrlUpdate = true; officialUrl = value; emitState() }
    }
    fun setWhitePaperUrl(value: String) {
        if (whitePaperUrl != value) { whitePaperUrlUpdate = true; whitePaperUrl = value; emitState() }
    }
    fun setDescription(value: String) {
        if (description != value) { descriptionUpdate = true; description = value; emitState() }
    }
    fun setBaseURI(value: String) {
        if (baseURI != value) { baseURIUpdate = true; baseURI = value; emitState() }
    }
    fun setMintPrice(value: String) {
        if (mintPrice != value) { mintPriceUpdate = true; mintPrice = value; emitState() }
    }
    fun setMaxSupply(value: String) {
        if (maxSupply != value) { maxSupplyUpdate = true; maxSupply = value; emitState() }
    }

    fun update() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        sendResult = SendResult.Sending

        val privateKey = this.privateKey

        if (orgNameUpdate && orgName != null) {
            service.setOrgName(privateKey, orgName!!)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (officialUrlUpdate && officialUrl != null) {
            service.setOfficialUrl(privateKey, officialUrl!!)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (whitePaperUrlUpdate && whitePaperUrl != null) {
            service.setWhitePaperUrl(privateKey, whitePaperUrl!!)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (descriptionUpdate && description != null) {
            service.setDescription(privateKey, description!!)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (baseURIUpdate && baseURI != null) {
            service.setBaseURI(privateKey, baseURI!!)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (mintPriceUpdate && mintPrice != null) {
            val wei = safeToWei(mintPrice!!)
            service.setMintPrice(privateKey, wei)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }
        if (maxSupplyUpdate && maxSupply != null) {
            val supply = maxSupply!!.toBigIntegerOrNull() ?: BigInteger.ZERO
            service.setMaxSupply(privateKey, supply)
                .subscribeIO({ }, { }).let { disposables.add(it) }
        }

        // 所有交易发出后稍等再标记完成
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(2000)
            isUpdating.set(false)
            orgNameUpdate = false; officialUrlUpdate = false; whitePaperUrlUpdate = false
            descriptionUpdate = false; baseURIUpdate = false; mintPriceUpdate = false; maxSupplyUpdate = false
            sendResult = SendResult.Sent()
            load()
        }
    }

    fun updateLogo(logo: ByteArray) {
        if (isUpdating.get()) return
        isUpdating.set(true)
        sendResult = SendResult.Sending
        service.setLogo(privateKey, logo).subscribeIO({
            isUpdating.set(false)
            sendResult = SendResult.Sent()
        }, { e ->
            isUpdating.set(false)
            sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
        })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    private fun weiToSafe(wei: BigInteger): String {
        return BigDecimal(wei)
            .divide(BigDecimal.TEN.pow(18), 8, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun safeToWei(safe: String): BigInteger {
        val bd = safe.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return bd.multiply(BigDecimal.TEN.pow(18)).toBigInteger()
    }
}
