package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonParser
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.dapp.core.HSDAppRequest
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "WCRequestEvmViewModel"

private const val PERSONAL_SIGN_METHOD = "personal_sign"
private const val TYPED_DATA_METHOD = "eth_signTypedData"
private const val TYPED_DATA_METHOD_V4 = "eth_signTypedData_v4"
private const val ETH_SIGN_METHOD = "eth_sign"
private const val SEND_TRANSACTION_METHOD = "eth_sendTransaction"
private const val SIGN_TRANSACTION_METHOD = "eth_signTransaction"

class WCRequestEvmViewModel(
    private val accountManager: IAccountManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val wcManager: WCManager,
) : ViewModel() {

    private val sessionRequestEvent = WCDelegate.sessionRequestEvent
    private val dappRequestEvent = WCDelegate.dappRequestEvent

    val blockchainType: io.horizontalsystems.marketkit.models.BlockchainType? =
        dappRequestEvent?.blockchainType
            ?: wcManager.getBlockchainType(sessionRequestEvent?.chainId)

    private val chainData = sessionRequestEvent?.let {
        wcManager.getChainData(it.chainId)
    }
    private val chainName = dappRequestEvent?.chainName ?: chainData?.name
    private val chainAddress = chainData?.address

    private val evmKitWrapper: EvmKitWrapper? = getEthereumKitWrapper()

    /**
     * 构造阶段解析请求数据，异常一律降级为 [SessionRequestUI.Initial]（页面显示错误态）。
     *
     * 这里在 ViewModel 初始化时执行，任何抛异常都会直接「打开请求页即崩溃」
     * （历史上 `getParam` 的 `Invalid Data` 就是这样崩的），因此整体兜底。
     */
    var sessionRequestUi: SessionRequestUI = runCatching { generateSessionRequestUI() }
        .getOrElse { e ->
            Log.e(TAG, "generateSessionRequestUI failed", e)
            SessionRequestUI.Initial
        }

    private fun clearSessionRequest() {
        sessionRequestUi = SessionRequestUI.Initial
    }

    private fun generateSessionRequestUI(): SessionRequestUI {
        // Handle DApp bridge request
        dappRequestEvent?.let { dapp ->
            if (evmKitWrapper == null) {
                clearSessionRequest()
                return SessionRequestUI.Initial
            }
            return SessionRequestUI.Content(
                peerUI = PeerUI(
                    peerName = dapp.peerName,
                    peerIcon = dapp.peerIcon,
                    peerUri = dapp.peerUrl,
                    peerDescription = "",
                ),
                topic = WCDelegate.DAPP_BRIDGE_TOPIC,
                requestId = dapp.id,
                param = getParam(dapp.method, dapp.params),
                method = dapp.method,
                chainName = chainName,
                chainAddress = chainAddress,
                walletName = accountManager.activeAccount?.name ?: ""
            )
        }

        // Handle WC session request
        return sessionRequestEvent?.let { sessionRequest ->
            if (evmKitWrapper == null) {
                clearSessionRequest()
                return@let SessionRequestUI.Initial
            }

            SessionRequestUI.Content(
                peerUI = PeerUI(
                    peerName = sessionRequest.peerMetaData?.name ?: "",
                    peerIcon = sessionRequest.peerMetaData?.icons?.firstOrNull() ?: "",
                    peerUri = sessionRequest.peerMetaData?.url ?: "",
                    peerDescription = sessionRequest.peerMetaData?.description ?: "",
                ),
                topic = sessionRequest.topic,
                requestId = sessionRequest.requestId,
                param = getParam(sessionRequest),
                method = sessionRequest.method,
                chainName = chainName,
                chainAddress = chainAddress,
                walletName = accountManager.activeAccount?.name ?: ""
            )
        } ?: SessionRequestUI.Initial
    }

    /** Extract param from DApp request params JSON array string */
    private fun getParam(method: String, paramsJson: String): String = when (method) {
        PERSONAL_SIGN_METHOD -> extractMessageParamFromPersonalSign(paramsJson)
        ETH_SIGN_METHOD -> extractStringParam(paramsJson) ?: fallbackParam(method, paramsJson)
        TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4, SEND_TRANSACTION_METHOD, SIGN_TRANSACTION_METHOD ->
            extractJsonObjectParam(paramsJson) ?: fallbackParam(method, paramsJson)
        else -> paramsJson
    }

    private fun getParam(sessionRequest: HSDAppRequest) =
        when (sessionRequest.method) {
            PERSONAL_SIGN_METHOD -> {
                extractMessageParamFromPersonalSign(sessionRequest.params)
            }

            ETH_SIGN_METHOD -> extractStringParam(sessionRequest.params)
                ?: fallbackParam(sessionRequest.method, sessionRequest.params)

            TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4, SEND_TRANSACTION_METHOD, SIGN_TRANSACTION_METHOD ->
                extractJsonObjectParam(sessionRequest.params)
                    ?: fallbackParam(sessionRequest.method, sessionRequest.params)

            else -> {
                sessionRequest.params
            }
        }

    /** params[1] 的字符串值；参数不合规时返回 null（不抛异常） */
    private fun extractStringParam(paramsJson: String): String? = runCatching {
        JsonParser.parseString(paramsJson).asJsonArray.get(1).asString
    }.getOrNull()

    /** params 中第一个 JSON 对象；参数不合规时返回 null（不抛异常） */
    private fun extractJsonObjectParam(paramsJson: String): String? = runCatching {
        JsonParser.parseString(paramsJson).asJsonArray
            .firstOrNull { it.isJsonObject }?.asJsonObject?.toString()
    }.getOrNull()

    /**
     * 参数不合规时退化为「展示原始参数」。
     *
     * 这个方法在 ViewModel 构造（[generateSessionRequestUI]）里执行，
     * 一旦抛异常就是「打开请求页即崩溃」，因此这里只记日志、返回原始内容，
     * 让用户至少能看到请求内容并自行拒绝。签名参数确实不可用时，
     * [allow] 会抛异常由页面提示错误，不会静默签错数据。
     */
    private fun fallbackParam(method: String, paramsJson: String): String {
        Log.w(TAG, "invalid params for $method, fallback to raw: $paramsJson")
        return paramsJson
    }

    private fun extractMessageParamFromPersonalSign(input: String): String {
        val jsonArray = runCatching { JSONArray(input) }.getOrNull()
            ?: return fallbackParam(PERSONAL_SIGN_METHOD, input)

        val message = if (jsonArray.length() > 0) {
            runCatching { jsonArray.getString(0) }.getOrNull()
        } else {
            null
        } ?: return fallbackParam(PERSONAL_SIGN_METHOD, input)

        return try {
            String(message.hexStringToByteArray())
        } catch (_: Throwable) {
            message
        }
    }

    private fun getEthereumKitWrapper(): EvmKitWrapper? {
        val blockchainType = blockchainType ?: return null
        val account = accountManager.activeAccount ?: return null
        val evmKitManager = evmBlockchainManager.getEvmKitManager(blockchainType)

        return evmKitManager.getEvmKitWrapper(account, blockchainType)
    }

    suspend fun allow() {
        val evmKit = evmKitWrapper ?: throw WCSessionManager.RequestDataError.NoSuitableEvmKit
        val signer = evmKit.signer ?: throw WCSessionManager.RequestDataError.NoSigner
        return suspendCoroutine { continuation ->
            val sessionRequest = sessionRequestUi as? SessionRequestUI.Content
            if (sessionRequest != null) {
                val result = when (sessionRequest.method) {
                    ETH_SIGN_METHOD -> {
                        val message = sessionRequest.param.hexStringToByteArray()
                        if (message.size == 32) {
                            signer.signByteArrayLegacy(message = message)
                        } else {
                            signer.signByteArray(message = message)
                        }
                    }

                    PERSONAL_SIGN_METHOD -> {
                        signer.signByteArray(message = sessionRequest.param.toByteArray())
                    }

                    TYPED_DATA_METHOD, TYPED_DATA_METHOD_V4 -> {
                        signer.signTypedData(rawJsonMessage = sessionRequest.param)
                    }

                    else -> throw Exception("Unsupported Chain")
                }

                WCDelegate.respondPendingRequest(
                    sessionRequest.requestId,
                    sessionRequest.topic,
                    result.toHexString(),
                    onSuccessResult = {
                        continuation.resume(Unit)
                        clearSessionRequest()
                    },
                    onErrorResult = {
                        continuation.resumeWithException(it)
                        clearSessionRequest()
                    }
                )
            }
        }
    }

    suspend fun reject() {
        return suspendCoroutine { continuation ->
            val sessionRequest = sessionRequestUi as? SessionRequestUI.Content
            if (sessionRequest != null) {
                WCDelegate.rejectRequest(
                    sessionRequest.topic,
                    sessionRequest.requestId,
                    onSuccessResult = {
                        clearSessionRequest()
                        continuation.resume(Unit)
                    },
                    onErrorResult = {
                        clearSessionRequest()
                        continuation.resumeWithException(it)
                    }
                )
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestEvmViewModel(
                App.accountManager,
                App.evmBlockchainManager,
                App.wcManager
            ) as T
        }
    }
}

sealed class SessionRequestUI {
    object Initial : SessionRequestUI()

    data class Content(
        val peerUI: PeerUI,
        val topic: String,
        val requestId: Long,
        val param: String,
        val method: String,
        val chainName: String?,
        val chainAddress: String?,
        val walletName: String,
    ) : SessionRequestUI()
}

@Parcelize
data class WCChainData(
    val id: Int,
    val name: String,
    val address: String?
) : Parcelable

data class PeerUI(
    val peerIcon: String,
    val peerName: String,
    val peerUri: String,
    val peerDescription: String,
) {
    companion object {
        val Empty = PeerUI("", "", "", "")
    }
}