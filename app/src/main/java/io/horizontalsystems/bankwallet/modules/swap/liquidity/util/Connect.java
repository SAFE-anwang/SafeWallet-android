package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

import io.horizontalsystems.bankwallet.core.App;
import io.horizontalsystems.ethereumkit.models.Chain;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Connect {

    private static final Logger log = LoggerFactory.getLogger(Connect.class);

//    private static final String endpoint = "https://bsc-mainnet.nodereal.io/v1/9e5c8cd94c754b6f84550fcece3f7d42";

    private static final String endpoint = "https://bsc-dataseed.binance.org/";

    private static final String eth_endpoint = "https://mainnet.infura.io/v3/" + App.appConfigProvider.getInfuraProjectId();
    public static Web3j connect(Chain chain){
        log.info("connect to {}" , endpoint);
        OkHttpClient.Builder builder = HttpService.getOkHttpClientBuilder();
        if (chain == Chain.Ethereum) {
            Interceptor headersInterceptor = new Interceptor() {
                @NonNull
                @Override
                public Response intercept(@NonNull Chain chain) throws IOException {
                    Request.Builder requestBuilder = chain.request().newBuilder();
                    requestBuilder.header("Authorization", Credentials.basic("", App.appConfigProvider.getInfuraProjectSecret()));
                    return chain.proceed(requestBuilder.build());
                }
            };
            builder.addInterceptor(headersInterceptor);
        }
        String url = "";
        switch (chain) {
            case Ethereum -> url = eth_endpoint;
            case BinanceSmartChain -> url = endpoint;
            case SafeFour -> {
                if (Chain.SafeFour.isSafe4TestNetId()) {
                    url = "https://safe4testnet.anwang.com/rpc";
                } else {
                    url = "https://safe4.anwang.com/rpc";
                }
            }
        }
        /*if (isETH) {
            url = eth_endpoint;
        } else {
            url = endpoint;
        }*/
        return Web3j.build(new HttpService(url, builder.build()));
    }

}
