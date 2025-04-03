package io.horizontalsystems.bankwallet.net

import com.anwang.safewallet.safekit.netwok.RetrofitUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.logging.Logger

class SafeApiKeyService() {

    private val logger = Logger.getLogger("SafeApiService")
    private val service: SafeNetServiceApi
    private val gson: Gson

    private val url: String = "https://safewallet.anwang.com/"

    init {
        gson = GsonBuilder()
            .setLenient()
            .create()

        service = RetrofitUtils.build(url).create(SafeNetServiceApi::class.java)
    }

    fun getRpcEndpoint(): Single<List<RpcEndpoint>> {
        return service.getRpcEndpoint().map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getApiKey(): Single<List<ApiKey>> {
        return service.getApiKey().map {
            parseApiKeyResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    private fun parseResponse(response: JsonElement): List<RpcEndpoint> {
        try {
            val responseObj = response.asJsonArray
            return gson.fromJson(
                responseObj,
                object : TypeToken<List<RpcEndpoint>>() {}.type
            )
        } catch (rateLimitExceeded: EtherscanService.RequestError.RateLimitExceed) {
            throw rateLimitExceeded
        } catch (err: Throwable) {
            throw EtherscanService.RequestError.ResponseError("Unexpected response: $response")
        }
    }

    private fun parseApiKeyResponse(response: JsonElement): List<ApiKey> {
        try {
            val responseObj = response.asJsonArray
            return gson.fromJson(
                responseObj,
                object : TypeToken<List<ApiKey>>() {}.type
            )
        } catch (rateLimitExceeded: EtherscanService.RequestError.RateLimitExceed) {
            throw rateLimitExceeded
        } catch (err: Throwable) {
            throw EtherscanService.RequestError.ResponseError("Unexpected response: $response")
        }
    }

    open class RequestError(message: String? = null) : Exception(message ?: "") {
        class ResponseError(message: String) : RequestError(message)
        class RateLimitExceed : RequestError()
    }

    private interface SafeNetServiceApi {
        @GET("/v1/evm/rpc/services")
        fun getRpcEndpoint(): Single<JsonElement>

        @GET("/v1/apiKeys")
        fun getApiKey(): Single<JsonElement>
    }

    data class RpcEndpoint(
        val id: Int,
        val endpoint: String,
        val network: String,
        val networkId: Int,
        val type: String
    )

    data class ApiKey(
        val name: String,
        val key: List<String>
    )
}