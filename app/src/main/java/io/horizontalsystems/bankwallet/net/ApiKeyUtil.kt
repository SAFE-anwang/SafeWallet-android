package io.horizontalsystems.bankwallet.net

import com.google.android.exoplayer2.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.net.SafeApiKeyService.ApiKey
import io.horizontalsystems.bankwallet.net.SafeApiKeyService.RpcEndpoint
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.reactivex.schedulers.Schedulers

object ApiKeyUtil {

    val RPC_ENDPOINT_KEY = "rpc_endpoint_key"
    val API_KEY_KEY = "api_key_key"

    var apiKeyList = emptyList<ApiKey>()

    fun initApiKey() {
        getCacheRpcEndpoint()?.let { cache ->
            RpcSource.rpcList = cache.map { RpcSource.RpcInfo(it.network, it.endpoint) }
        }
        getCacheApiKey()?.let {
            apiKeyList = it
        }
        val safeApiKeyService = SafeApiKeyService()
        safeApiKeyService.getRpcEndpoint()
            .subscribeOn(Schedulers.io())
            .subscribe({
                saveRpcEndpoint(it)
                RpcSource.rpcList = it.map { RpcSource.RpcInfo(it.network, it.endpoint) }
            }, {
                getCacheRpcEndpoint()?.let { cache ->
                    RpcSource.rpcList = cache.map { RpcSource.RpcInfo(it.network, it.endpoint) }
                }

            })
        safeApiKeyService.getApiKey()
            .subscribeOn(Schedulers.io())
            .subscribe({
                saveApiKey(it)
                apiKeyList = it
            }, {

            })
    }

    private fun saveRpcEndpoint(rpcEndpoint: List<SafeApiKeyService.RpcEndpoint>) {
        try {
            val gson = Gson()
            MMKV.defaultMMKV()?.encode(RPC_ENDPOINT_KEY, gson.toJson(rpcEndpoint))
        } catch (e: Exception) {

        }
    }

    private fun saveApiKey(apiKey: List<SafeApiKeyService.ApiKey>) {
        try {
            val gson = Gson()
            MMKV.defaultMMKV()?.encode(API_KEY_KEY, gson.toJson(apiKey))
        } catch (e: Exception) {

        }
    }

    fun getCacheRpcEndpoint(): List<SafeApiKeyService.RpcEndpoint>? {
        try {
            val gson = Gson()
            return gson.fromJson(MMKV.defaultMMKV()?.decodeString(RPC_ENDPOINT_KEY),
                object : TypeToken<List<RpcEndpoint>>() {}.type)
        } catch (e: Exception) {

        }
        return null
    }

    fun getCacheApiKey(): List<SafeApiKeyService.ApiKey>? {
        try {
            val gson = Gson()
            return gson.fromJson(MMKV.defaultMMKV()?.decodeString(API_KEY_KEY),
                object : TypeToken<List<ApiKey>>() {}.type)
        } catch (e: Exception) {

        }
        return null
    }

    fun getEthRpcEndpoint(): String? {
        getCacheRpcEndpoint()?.let { list ->
            list.forEach {
                if (it.network == "eth") {
                    return it.endpoint
                }
            }
        }
        return null
    }

    fun getApiKey(key: String): List<String>? {
        if (apiKeyList.isEmpty()) {
            getCacheApiKey()?.let {
                apiKeyList = it
            }
        }
        if (apiKeyList.isEmpty()) {
            return null
        }
        apiKeyList.forEach {
            if (it.name == key)  return it.key
        }
        return null
    }
}