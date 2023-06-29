package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class Connect {

    private static final Logger log = LoggerFactory.getLogger(Connect.class);

//    private static final String endpoint = "https://bsc-mainnet.nodereal.io/v1/9e5c8cd94c754b6f84550fcece3f7d42";

    private static final String endpoint = "https://bsc-dataseed.binance.org/";

    public static Web3j connect(){
        log.info("connect to {}" , endpoint);
        return Web3j.build(new HttpService(endpoint));
    }

}
