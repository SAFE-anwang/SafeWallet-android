package io.horizontalsystems.bankwallet.modules.dapp.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.modules.dapp.DAppItem
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.logging.Logger

class DAppApiService() {

    private val logger = Logger.getLogger("DAppApiService")
    private val service: DAppServiceApi
    private val gson: Gson

    private val url: String = "https://safewallet.anwang.com/"

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = NetworkUtils.getUnsafeOkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor)

        gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create(DAppServiceApi::class.java)
    }

    fun getAllList(): Single<List<DAppItem>> {
        return service.getAllList().retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getRecommends(): Single<List<DAppItem>> {
        return service.getRecommends().retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getListByType(type: String): Single<List<DAppItem>> {
        return service.getListByType(type).retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getListBySubType(subType: String): Single<List<DAppItem>> {
        return service.getListBySubType(subType).retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getListByName(name: String): Single<List<DAppItem>> {
        return service.getListByName(name).retryWhenError(RequestError.RateLimitExceed::class)
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

    private interface DAppServiceApi {
        @GET("/api/walletcontent/getAll")
        fun getAllList(): Single<List<DAppItem>>

        @GET("/api/walletcontent/recommends")
        fun getRecommends(): Single<List<DAppItem>>

        @GET("/api/walletcontent/byType")
        fun getListByType(@Query("type") type: String): Single<List<DAppItem>>

        @GET("/api/walletcontent/bySubType")
        fun getListBySubType(@Query("subType") type: String): Single<List<DAppItem>>

        @GET("/api/walletcontent/byName")
        fun getListByName(@Query("name") type: String): Single<List<DAppItem>>
    }

    data class VpnInfo(
        val endpoint: String,
        val uid: String
    )
}