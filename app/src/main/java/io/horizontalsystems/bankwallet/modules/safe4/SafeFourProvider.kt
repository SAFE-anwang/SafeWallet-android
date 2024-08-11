package io.horizontalsystems.bankwallet.modules.safe4

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeInfo
import io.horizontalsystems.bankwallet.modules.safe4.node.reward.RewardInfo
import io.horizontalsystems.ethereumkit.api.models.EtherscanResponse
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.marketkit.providers.RetrofitUtils
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class SafeFourProvider(baseUrl: String) {

    private val gson: Gson

    init {
        gson = GsonBuilder()
                .setLenient()
                .create()
    }

    private val service by lazy {
        RetrofitUtils.build(baseUrl)
                .create(SafeFourService::class.java)
    }

    fun getRewards(address: String): Single<EtherscanResponse> {
        return service.getRewards(address)
                .map {
                    parseResponse(it)
                }.retryWhenError(EtherscanService.RequestError.RateLimitExceed::class)
    }


    private fun parseResponse(response: JsonElement): EtherscanResponse {
        try {
            val responseObj = response.asJsonObject
            val status = responseObj["status"].asJsonPrimitive.asString
            val message = responseObj["message"].asJsonPrimitive.asString

            if (status == "0" && message != "No transactions found") {
                val result = responseObj["result"].asJsonPrimitive.asString
                if (message == "NOTOK" && result == "Max rate limit reached") {
                    throw EtherscanService.RequestError.RateLimitExceed()
                }
            }
            val result: List<Map<String, String>> = gson.fromJson(responseObj["result"], object : TypeToken<List<Map<String, String>>>() {}.type)
            return EtherscanResponse(status, message, result)

        } catch (rateLimitExceeded: EtherscanService.RequestError.RateLimitExceed) {
            throw rateLimitExceeded
        } catch (err: Throwable) {
            throw EtherscanService.RequestError.ResponseError("Unexpected response: $response")
        }
    }

    private interface SafeFourService {
        @GET("rewards/{address}")
        fun getRewards(
                @Path("address") address: String,
        ): Single<JsonElement>
    }
}