package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import java.math.BigInteger;

public class Constants {

    public static final String NULL_ADDRESS = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    public static class DexFee {

        public static final String UNI_SWAP = "0.003";

        public static final String PANCAKE_SWAP = "0.0025";

    }

    public static class Tokens {

        public static final String USDT = "0x55d398326f99059ff775485246999027b3197955";

        public static final String SAFE = "0x4d7fa587ec8e50bd0e9cd837cb4da796f47218a1";

        public static final String BTP = "0x40f75ed09c7bc89bf596ce0ff6fb2ff8d02ac019";

    }

    public static class MultiCall {

        public static final String ETH_MAINNET = "";

        public static final String BSC_MAINNET = "0x1ee38d535d541c55c9dae27b12edf090c608e6fb";

    }

    public static class DEX {

        public static final String SAFESWAP_V2_ROUTER_ADDRESS = "0x6476008c612df9f8db166844ffe39d24aea12271";

        public static final String SAFESWAP_V2_FACTORY_ADDRESS = "0xb3c827077312163c53e3822defe32caffe574b42";

        public static final String PANCAKE_V2_ROUTER_ADDRESS = "0x10ed43c718714eb63d5aa57b78b54704e256024e";

        public static final String PANCAKE_V2_FACTORY_ADDRESS = "0xca143ce32fe78f1f7019d7d551a6402fc5350c73";

        public static final String UNISWAP_V2_ROUTER_ADDRESS = "0x7a250d5630b4cf539739df2c5dacb4c659f2488d";

        public static final String UNISWAP_V2_FACTORY_ADDRESS = "0x5c69bee701ef814a2b6a3edd4b1652cb9cc5aa6f";

    }

    public static class INIT_CODE_HASH {

        public static final String PANCAKE_SWAP_FACTORY_INIT_CODE_HASH = "0x00fb7f630766e6a796048ea87d01acd3068e8ff67d078148a3fa3f4a84f69bd5";

        public static final String SAFE_SWAP_FACTORY_INIT_CODE_HASH = "0xad0e51aa7a058efb9eb40fd6385473f0175ee7419e8d4f91a4e0294ec12b2d13";

        public static final String UNI_SWAP_FACTORY_INIT_CODE_HASH = "0x96e8ac4277198ff8b6f785478aa9a39f403cb768dd02cbee326c3e7da348845f";

    }

    public static final Integer deadLine = 20; // 20 min

    public static BigInteger getDeadLine(){
        long txDeadLine = (System.currentTimeMillis()) / 1000 + (60 * deadLine);
        return new BigInteger( txDeadLine + "" );
    }

    // 0.5% 假设的用户设置的滑点
    public static final String slippage = "0.005";
}
