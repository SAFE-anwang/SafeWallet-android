package io.horizontalsystems.bankwallet.modules.dapp

import android.util.Log
import android.webkit.JavascriptInterface
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthCall
import org.web3j.protocol.core.methods.response.EthGetBalance
import java.math.BigInteger

/**
 * Native-JS bridge for DApp WebView.
 * Creates window.ethereum provider in WebView and handles EIP-1193 RPC calls.
 */
class DAppWeb3Bridge(
    private val url: String,
    /** Callback to get wallet accounts. Returns null if not available. */
    private val onRequestAccounts: () -> String?,
    /** Callback for eth_sendTransaction: (requestId, txParams JSONObject) */
    private val onSendTransaction: (Int, JSONObject) -> Unit,
    /** Callback for signing: (requestId, method, params) */
    private val onSign: (Int, String, String) -> Unit,
    /** Callback to switch chain: chainId (int) -> success */
    private val onSwitchChain: (Int) -> Boolean,
) {
    companion object {
        private const val TAG = "DAppWeb3Bridge"
    }

    // Determine default chain from URL
    val defaultChain: Chain by lazy {
        val urlLower = url.lowercase()
        when {
            urlLower.contains("pancakeswap") || urlLower.contains("pancake") -> Chain.BinanceSmartChain
            urlLower.contains("bsc") || urlLower.contains("binance") -> Chain.BinanceSmartChain
            urlLower.contains("uniswap") -> Chain.Ethereum
            urlLower.contains("safe4") -> Chain.SafeFour
            else -> Chain.Ethereum
        }
    }

    val defaultBlockchainType: BlockchainType by lazy {
        when (defaultChain) {
            Chain.Ethereum -> BlockchainType.Ethereum
            Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
            Chain.SafeFour -> BlockchainType.SafeFour
            Chain.Polygon -> BlockchainType.Polygon
            else -> BlockchainType.Ethereum
        }
    }

    private var web3j: Web3j? = null

    private fun getWeb3j(): Web3j {
        if (web3j == null) {
            web3j = io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Connect.connect(defaultChain)
        }
        return web3j!!
    }

    @Volatile
    private var connectedAccounts = mutableListOf<String>()

    @Volatile
    private var currentChainIdHex = "0x" + defaultChain.id.toString(16)

    /** Pending async request IDs (e.g. eth_sendTransaction, personal_sign) */
    private val pendingRequests = mutableSetOf<Int>()

    // Response callback (set by fragment)
    @Volatile
    private var responseCallback: ((String) -> Unit)? = null

    fun setResponseCallback(callback: (String) -> Unit) {
        this.responseCallback = callback
    }

    /**
     * Called from fragment after user confirms a pending request.
     * @param id request id
     * @param result JSON-RPC result string (e.g. "0x" + txHash or "0x" + signature)
     */
    fun resolveRequest(id: Int, result: String) {
        pendingRequests.remove(id)
        sendResponse(id, result, null)
    }

    /**
     * Called from fragment after user rejects a pending request.
     */
    fun rejectRequest(id: Int, message: String = "Rejected by user") {
        pendingRequests.remove(id)
        sendResponse(id, null, mapOf("code" to 4001, "message" to message))
    }

    fun setAccounts(accounts: List<String>) {
        connectedAccounts = accounts.toMutableList()
    }

    fun setChainId(chainId: Int) {
        currentChainIdHex = "0x" + chainId.toString(16)
    }

    // ============ JavaScriptInterface ============

    /** Methods that can be handled synchronously (no network, no user interaction) */
    private val syncMethods = setOf(
        "eth_accounts", "eth_chainId", "net_version", "web3_clientVersion",
        "eth_requestAccounts", "wallet_getCapabilities",
        "wallet_switchEthereumChain", "wallet_addEthereumChain", "wallet_watchAsset"
    )

    /**
     * Handle RPC requests from JS.
     * For synchronous methods, returns JSON-RPC response string directly (eliminates webView.post delay).
     * For async methods (network calls, user confirmations), returns empty string and sends response via callback.
     */
    @JavascriptInterface
    fun postMessage(jsonMessage: String): String {
        Log.d(TAG, "postMessage: $jsonMessage")
        try {
            val msg = JSONObject(jsonMessage)
            val id = msg.getInt("id")
            val method = msg.getString("method")
            val params = msg.optJSONArray("params") ?: JSONArray()

            Log.d(TAG, "handle: id=$id, method=$method")

            if (method in syncMethods) {
                // Synchronous path: return result directly to JS caller (no thread, no webView.post)
                val result = handleRequest(id, method, params)
                if (id in pendingRequests) return ""  // e.g. eth_requestAccounts or wallet_switchEthereumChain that triggered async
                return buildJsonRpcResponse(id, result, null)
            }

            // Async path: handle on background thread, send response via callback
            Thread {
                try {
                    val result = handleRequest(id, method, params)
                    if (id in pendingRequests) return@Thread
                    sendResponse(id, result, null)
                } catch (e: Exception) {
                    Log.e(TAG, "error: ${e.message}", e)
                    try {
                        sendResponse(id, null, mapOf("code" to -32603, "message" to (e.message ?: "Internal error")))
                    } catch (_: Exception) {}
                }
            }.start()

            return ""
        } catch (e: Exception) {
            Log.e(TAG, "error: ${e.message}", e)
            try {
                val id = JSONObject(jsonMessage).getInt("id")
                return buildJsonRpcResponse(id, null, mapOf("code" to -32603, "message" to (e.message ?: "Internal error")))
            } catch (_: Exception) {
                return ""
            }
        }
    }

    private fun buildJsonRpcResponse(id: Int, result: Any?, error: Map<String, Any>?): String {
        val msg = JSONObject()
        msg.put("jsonrpc", "2.0")
        msg.put("id", id)
        if (error != null) {
            msg.put("error", JSONObject(error))
        } else {
            val jsonResult = when (result) {
                is JSONArray -> result
                is JSONObject -> result
                is Boolean -> result
                is Number -> result
                null -> JSONObject.NULL
                else -> result.toString()
            }
            msg.put("result", jsonResult)
        }
        return msg.toString()
    }

    // ============ RPC Handler ============

    private fun handleRequest(requestId: Int, method: String, params: JSONArray): Any? {
        return when (method) {
            "eth_requestAccounts" -> handleRequestAccounts()
            "eth_accounts" -> handleAccounts()
            "eth_chainId" -> currentChainIdHex
            "net_version" -> defaultChain.id.toString()
            "eth_getBalance" -> handleGetBalance(params.optString(0), params.optString(1, "latest"))
            "eth_call" -> handleEthCall(params.optJSONObject(0), params.optString(1, "latest"))
            "eth_blockNumber" -> handleBlockNumber()
            "eth_gasPrice" -> handleGasPrice()
            "eth_estimateGas" -> handleEstimateGas(params.optJSONObject(0))
            "eth_getTransactionCount" -> handleGetTxCount(params.optString(0), params.optString(1, "latest"))
            "eth_getBlockByNumber" -> handleGetBlock(params.optString(0, "latest"), params.optBoolean(1, false))
            "eth_getCode" -> handleGetCode(params.optString(0), params.optString(1, "latest"))

            "eth_sendTransaction" -> {
                val txObj = params.optJSONObject(0)
                if (txObj != null) {
                    pendingRequests.add(requestId)
                    onSendTransaction.invoke(requestId, txObj)
                }
                null // response will be sent asynchronously via resolveRequest/rejectRequest
            }
            "personal_sign" -> {
                val msg = params.optString(0)
                pendingRequests.add(requestId)
                onSign.invoke(requestId, "personal_sign", msg)
                null
            }
            "eth_signTypedData", "eth_signTypedData_v4" -> {
                val typedData = params.optString(1)
                pendingRequests.add(requestId)
                onSign.invoke(requestId, method, typedData)
                null
            }
            "eth_sign" -> {
                pendingRequests.add(requestId)
                onSign.invoke(requestId, "eth_sign", params.optString(1))
                null
            }

            "wallet_switchEthereumChain" -> {
                val chainObj = params.optJSONObject(0)
                val chainIdHex = chainObj?.optString("chainId") ?: "0x0"
                handleSwitchChain(chainIdHex)
            }
            "wallet_addEthereumChain" -> {
                val chainObj = params.optJSONObject(0)
                val chainIdHex = chainObj?.optString("chainId") ?: "0x0"
                handleSwitchChain(chainIdHex)
            }
            "wallet_watchAsset" -> true

            // EIP-5792: wallet_getCapabilities
            "wallet_getCapabilities" -> JSONObject().apply {
                put("0x1", JSONObject().apply { // Ethereum mainnet
                    put("atomicBatch", JSONObject().apply { put("supported", true) })
                })
            }

            "web3_clientVersion" -> "SafeWallet/v1.0"

            else -> {
                Log.w(TAG, "Unsupported method: $method")
                throw Exception("Method not supported: $method")
            }
        }
    }

    // ============ Account Methods ============

    private fun handleRequestAccounts(): JSONArray {
        if (connectedAccounts.isEmpty()) {
            // Try active account
            try {
                val account = App.accountManager.activeAccount
                if (account != null) {
                    val evmKitManager = App.evmBlockchainManager.getEvmKitManager(defaultBlockchainType)
                    val wrapper = evmKitManager.getEvmKitWrapper(account, defaultBlockchainType)
                    val addr = wrapper?.evmKit?.receiveAddress?.hex
                    if (addr != null) {
                        connectedAccounts.add(addr)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getEvmAddress error: ${e.message}")
            }

            // Callback to fragment
            if (connectedAccounts.isEmpty()) {
                onRequestAccounts.invoke()?.let { json ->
                    try {
                        val arr = JSONArray(json)
                        for (i in 0 until arr.length()) {
                            connectedAccounts.add(arr.getString(i))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        if (connectedAccounts.isEmpty()) {
            throw Exception("No accounts found. Please create or import a wallet first.")
        }

        return JSONArray(connectedAccounts)
    }

    private fun handleAccounts() = JSONArray(connectedAccounts)

    // ============ Read-only RPC ============

    private fun handleGetBalance(address: String, block: String): String {
        val result: EthGetBalance = getWeb3j().ethGetBalance(address, resolveBlock(block)).send()
        checkError(result)
        return toHex0x(result.balance)
    }

    private fun handleEthCall(txObj: JSONObject?, block: String): String {
        if (txObj == null) throw Exception("Missing transaction object")
        val tx = toWeb3jTx(txObj)
        val result: EthCall = getWeb3j().ethCall(tx, resolveBlock(block)).send()
        checkError(result)
        return result.value ?: "0x"
    }

    private fun handleBlockNumber(): String {
        val result = getWeb3j().ethBlockNumber().send()
        checkError(result)
        return toHex0x(result.blockNumber)
    }

    private fun handleGasPrice(): String {
        val result = getWeb3j().ethGasPrice().send()
        checkError(result)
        return toHex0x(result.gasPrice)
    }

    private fun handleEstimateGas(txObj: JSONObject?): String {
        if (txObj == null) throw Exception("Missing transaction object")
        val tx = toWeb3jTx(txObj)
        val result = getWeb3j().ethEstimateGas(tx).send()
        checkError(result)
        return toHex0x(result.amountUsed)
    }

    private fun handleGetTxCount(address: String, block: String): String {
        val result = getWeb3j().ethGetTransactionCount(address, resolveBlock(block)).send()
        checkError(result)
        return toHex0x(result.transactionCount)
    }

    private fun handleGetBlock(blockStr: String, fullTx: Boolean): JSONObject {
        val result = getWeb3j().ethGetBlockByNumber(resolveBlock(blockStr), fullTx).send()
        checkError(result)
        val block = result.block
        return JSONObject().apply {
            put("number", toHex0x(block.number))
            put("hash", block.hash)
            put("parentHash", block.parentHash)
            put("timestamp", toHex0x(block.timestamp))
            put("gasLimit", toHex0x(block.gasLimit))
            put("gasUsed", toHex0x(block.gasUsed))
        }
    }

    private fun handleGetCode(address: String, block: String): String {
        val result = getWeb3j().ethGetCode(address, resolveBlock(block)).send()
        checkError(result)
        return result.code ?: "0x"
    }

    private fun handleSwitchChain(chainIdHex: String): Boolean {
        val chainId = chainIdHex.removePrefix("0x").toIntOrNull(16) ?: return false
        if (onSwitchChain.invoke(chainId)) {
            currentChainIdHex = chainIdHex
            return true
        }
        throw Exception("Failed to switch chain")
    }

    // ============ Response ============

    private fun sendResponse(id: Int, result: Any?, error: Map<String, Any>?) {
        try {
            val msg = JSONObject()
            msg.put("jsonrpc", "2.0")
            msg.put("id", id)
            if (error != null) {
                msg.put("error", JSONObject(error))
            } else {
                // Convert result to appropriate JSON type
                val jsonResult = when (result) {
                    is JSONArray -> result
                    is JSONObject -> result
                    is Boolean -> result
                    is Number -> result
                    null -> JSONObject.NULL
                    else -> result.toString()
                }
                msg.put("result", jsonResult)
            }
            responseCallback?.invoke(msg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "sendResponse error: ${e.message}")
        }
    }

    // ============ Helpers ============

    private fun toWeb3jTx(obj: JSONObject): Transaction {
        val from = obj.optString("from", connectedAccounts.firstOrNull() ?: "0x0000000000000000000000000000000000000000")
        val to = obj.optString("to", "")
        val data = obj.optString("data", "0x")
        val value = toBigInt(obj.optString("value", "0x0"))
        val gas = obj.optString("gas", "").let { if (it.isNotEmpty()) toBigInt(it) else null }
        val gasPrice = obj.optString("gasPrice", "").let { if (it.isNotEmpty()) toBigInt(it) else null }

        return Transaction.createFunctionCallTransaction(
            from,
            null,
            gasPrice,
            gas,
            to,
            value,
            data
        )
    }

    private fun resolveBlock(block: String): DefaultBlockParameter {
        return when (block.lowercase()) {
            "latest" -> DefaultBlockParameterName.LATEST
            "pending" -> DefaultBlockParameterName.PENDING
            "earliest" -> DefaultBlockParameterName.EARLIEST
            else -> {
                val clean = block.removePrefix("0x")
                val num = BigInteger(if (clean.isEmpty()) "0" else clean, 16)
                DefaultBlockParameter.valueOf(num)
            }
        }
    }

    private fun toBigInt(hex: String): BigInteger {
        val clean = hex.removePrefix("0x")
        return if (clean.isEmpty()) BigInteger.ZERO else BigInteger(clean, 16)
    }

    private fun toHex0x(value: BigInteger?): String = "0x" + (value ?: BigInteger.ZERO).toString(16)

    private fun checkError(response: org.web3j.protocol.core.Response<*>) {
        if (response.hasError()) {
            throw Exception(response.error.message)
        }
    }

    /**
     * Build the JS provider script to inject into WebView.
     * @param chainIdHex hex-encoded chain ID (e.g. "0x1" for Ethereum mainnet)
     * @param networkVersion decimal chain ID string (e.g. "1" for Ethereum mainnet)
     * @param selectedAddressHex connected account address, or empty string
     */
    fun buildProviderScript(
        chainIdHex: String,
        networkVersion: String,
        selectedAddressHex: String
    ): String {
        val hasAccount = selectedAddressHex.isNotEmpty()
        return """
(function() {
    if (window.ethereum && window.ethereum._safeWalletInjected) return;

    const bridge = window._safeWalletBridge || {};

    var requestId = 0;
    const pendingRequests = {};
    const eventListeners = {};

    function sendRequest(method, params) {
        return new Promise(function(resolve, reject) {
            const id = ++requestId;
            const msg = JSON.stringify({ id: id, method: method, params: params || [] });
            if (bridge.postMessage) {
                var syncResult = bridge.postMessage(msg);
                if (syncResult) {
                    // Synchronous response from native (no delay)
                    try {
                        var resp = JSON.parse(syncResult);
                        if (resp.error) {
                            reject(new Error(resp.error.message || 'RPC Error'));
                        } else {
                            resolve(resp.result);
                        }
                    } catch(e) {
                        reject(e);
                    }
                } else {
                    // Async request - wait for _safeWalletOnResponse callback
                    pendingRequests[id] = { resolve: resolve, reject: reject, _method: method };
                }
            } else {
                reject(new Error('Bridge not available'));
            }
        });
    }

    function emit(eventName, data) {
        // Call listeners registered via .on() (this is how wagmi/web3-react listens)
        const listeners = eventListeners[eventName] || [];
        console.log('SafeWallet: emit ' + eventName, data, '(' + listeners.length + ' listeners)');
        listeners.forEach(function(fn) {
            try { fn(data); } catch(e) { console.error('SafeWallet: emit error', eventName, e); }
        });
    }

    // Synchronous state (set from native before injection)
    const syncedChainId = "$chainIdHex";
    const syncedNetworkVersion = "$networkVersion";
    const syncedAddress = "$selectedAddressHex";

    window.ethereum = {
        _safeWalletInjected: true,
        isMetaMask: true,
        isSafeWallet: true,
        chainId: syncedChainId,
        networkVersion: syncedNetworkVersion,
        selectedAddress: ${if (hasAccount) "syncedAddress" else "null"},
        _events: eventListeners,
        _state: {
            chainId: syncedChainId,
            accounts: ${if (hasAccount) "[syncedAddress]" else "[]"},
            isConnected: true,
            isUnlocked: true,
        },

        // EIP-1193
        request: function(args) {
            return sendRequest(args.method, args.params);
        },

        // EIP-1193: returns whether the provider is connected
        isConnected: function() {
            return true;
        },

        // EIP-1193 event subscription
        on: function(event, listener) {
            if (!eventListeners[event]) eventListeners[event] = [];
            eventListeners[event].push(listener);
        },

        removeListener: function(event, listener) {
            if (eventListeners[event]) {
                eventListeners[event] = eventListeners[event].filter(function(fn) { return fn !== listener; });
            }
        },

        removeAllListeners: function(event) {
            if (event) {
                delete eventListeners[event];
            } else {
                for (var k in eventListeners) { delete eventListeners[k]; }
            }
        },

        emit: emit,
        _emit: emit,

        // Legacy send API (still used by older libraries)
        send: function(methodOrPayload, paramsOrCallback) {
            if (typeof methodOrPayload === 'string') {
                return sendRequest(methodOrPayload, Array.isArray(paramsOrCallback) ? paramsOrCallback : []);
            }
            var payload = methodOrPayload;
            return sendRequest(payload.method, payload.params);
        },

        sendAsync: function(payload, callback) {
            sendRequest(payload.method, payload.params)
                .then(function(r) { callback(null, { id: payload.id, jsonrpc: '2.0', result: r }); })
                .catch(function(e) { callback(e); });
        },

        enable: function() { return sendRequest('eth_requestAccounts', []); },

        // MetaMask compatibility
        _metamask: {
            isUnlocked: function() {
                return new Promise(function(resolve) { resolve(true); });
            },
        },
    };

    // Handle responses from native
    window._safeWalletOnResponse = function(jsonStr) {
        try {
            var resp = JSON.parse(jsonStr);
            var id = resp.id;
            var pending = pendingRequests[id];
            console.log('SafeWallet: onResponse id=' + id, 'method=' + (pending ? pending._method : '?'), 'error=' + !!resp.error);
            if (!pending) {
                console.warn('SafeWallet: no pending request for id=' + id);
                return;
            }
            delete pendingRequests[id];
            if (resp.error) {
                pending.reject(new Error(resp.error.message || 'RPC Error'));
            } else {
                pending.resolve(resp.result);
            }
        } catch(e) {
            console.error('SafeWallet: parse error', e);
        }
    };

    // Mark provider as ready - some frameworks check this
    window.ethereum._state.isConnected = true;
    window.ethereum.isReady = true;

    console.log('SafeWallet: window.ethereum injected (chainId=' + syncedChainId + ', address=' + syncedAddress + ')');

    // Dispatch EIP-6963 discovery events synchronously
    window.dispatchEvent(new CustomEvent('ethereum#initialized', { detail: { provider: window.ethereum } }));
    window.dispatchEvent(new CustomEvent('eip6963:announceProvider', {
        detail: {
            info: {
                uuid: 'safeWallet_' + Date.now(),
                name: 'SafeWallet',
                icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg"/>',
                rdns: 'io.horizontalsystems.bankwallet'
            },
            provider: window.ethereum
        }
    }));

    // Dispatch EIP-1193 connection events after a short delay to let wagmi register listeners.
    // Also dispatch immediately for libraries that register listeners synchronously.
    function dispatchEip1193Events() {
        emit('connect', { chainId: syncedChainId });

        if (${if (hasAccount) "true" else "false"}) {
            emit('accountsChanged', [syncedAddress]);
        }

        console.log('SafeWallet: EIP-1193 events dispatched');
    }

    // First attempt: immediate (for frameworks that already registered listeners)
    dispatchEip1193Events();

    // Second attempt: delayed (for frameworks that register listeners after provider detection)
    setTimeout(function() {
        var listeners = eventListeners['connect'] || [];
        var acctsListeners = eventListeners['accountsChanged'] || [];
        console.log('SafeWallet: re-emitting events, connect listeners=' + listeners.length + ', accountsChanged listeners=' + acctsListeners.length);

        // Re-emit with a slight offset so listeners registered during/after init get these events
        if (listeners.length >= 0) {
            emit('connect', { chainId: syncedChainId });
        }
        if (${if (hasAccount) "true" else "false"}) {
            emit('accountsChanged', [syncedAddress]);
        }
    }, 300);

})();
        """.trimIndent()
    }
}
