package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

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
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.logging.Logger

class Safe3TestCoinService() {

    private val logger = Logger.getLogger("Safe3TestCoinService")
    private val service: GetTestCoinServiceApi
    private val gson: Gson

    private val url: String = "https://safe4testnet.anwang.com/"

    init {
        gson = GsonBuilder()
            .setLenient()
            .create()

        service = RetrofitUtils.build(url).create(GetTestCoinServiceApi::class.java)
    }

    fun getTestCoin(address: String): Single<Map<String, String>> {
        return service.getTestCoin(mapOf("address" to address)).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    private fun parseResponse(response: JsonElement): Map<String, String> {
        try {
            val responseObj = response.asJsonObject
            return gson.fromJson(
                responseObj,
                object : TypeToken<Map<String, String>>() {}.type
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

    private interface GetTestCoinServiceApi {
        @POST("/5005/get_test_coin")
        fun getTestCoin(@Body body: Map<String, String>): Single<JsonElement>
    }
}