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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.logging.Logger

class SafeNetService() {

    private val logger = Logger.getLogger("SafeNetService")
    private val service: SafeNetServiceApi
    private val gson: Gson

    private val url: String = "https://chain.anwang.org/"

    init {
        gson = GsonBuilder()
            .setLenient()
            .create()

        service = RetrofitUtils.build(url).create(SafeNetServiceApi::class.java)
    }

    fun getVpnNodes(): Single<List<Map<String, String>>> {
        return service.getVpnNode().map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    private fun parseResponse(response: JsonElement): List<Map<String, String>> {
        try {
            val responseObj = response.asJsonArray
            return gson.fromJson(
                responseObj,
                object : TypeToken<List<Map<String, String>>>() {}.type
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
        @GET("/insight-api-safe/utils/address/vpn")
        fun getVpnNode(): Single<JsonElement>
    }

    data class VpnInfo(
        val endpoint: String,
        val uid: String
    )
}