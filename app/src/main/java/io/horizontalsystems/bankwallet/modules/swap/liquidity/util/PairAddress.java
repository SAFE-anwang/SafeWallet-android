package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

public class PairAddress {

    public static String[] sort(String tokenA,String tokenB){
        String[] result = new String[2];
        if ( Numeric.toBigInt(tokenA).compareTo(Numeric.toBigInt(tokenB)) < 1  ){
            result[0] = tokenA;
            result[1] = tokenB;
        }else{
            result[0] = tokenB;
            result[1] = tokenA;
        }
        return result;
    }

    /**
     * 参考实现:
     *  https://stackoverflow.com/questions/68490061/compute-uniswap-pair-address-via-javascript
     *  https://github.com/web3/web3.js/blob/1.x/packages/web3-utils/src/soliditySha3.js
     *
     * @param factoryAddress
     * @param initCodeHash
     * @param tokenA
     * @param tokenB
     * @return
     */
    public static String getPairAddress(
            String factoryAddress,
            String initCodeHash,
            String tokenA,
            String tokenB
        ){

        String[] orderTokens = sort(tokenA,tokenB);
        String token0 = orderTokens[0];
        String token1 = orderTokens[1];

        String abiEncoded1 = TypeEncoder.encodePacked(new Address(token0))
                            + TypeEncoder.encodePacked(new Address(token1));

        String salt = Hash.sha3(abiEncoded1);

        String abiEncoded2 = TypeEncoder.encodePacked(new Address(factoryAddress)) +
                salt.replace("0x","");

        byte[] arr0 = Numeric.hexStringToByteArray( "0xff" + abiEncoded2 );
        byte[] arr1 = Numeric.hexStringToByteArray(  initCodeHash );
        byte[] arr = new byte[arr0.length + arr1.length];
        System.arraycopy(arr0,0,arr,0,arr0.length);
        System.arraycopy(arr1,0,arr,arr0.length,arr1.length);

        String result = Numeric.toHexString(Hash.sha3(arr));
        return "0x" + result.substring(26);
    }

    public static String getPairAddressForPancakeSwap(
            String tokenA,
            String tokenB
    ){
        return getPairAddress(
                Constants.DEX.PANCAKE_V2_FACTORY_ADDRESS ,
                Constants.INIT_CODE_HASH.PANCAKE_SWAP_FACTORY_INIT_CODE_HASH,
                tokenA,
                tokenB
        );
    }

    public static String getPairAddressForSafeSwap(
            String tokenA,
            String tokenB
    ){
        return getPairAddress(
                Constants.DEX.SAFESWAP_V2_FACTORY_ADDRESS ,
                Constants.INIT_CODE_HASH.SAFE_SWAP_FACTORY_INIT_CODE_HASH,
                tokenA,
                tokenB
        );
    }



    public static void main(String[] args) {

        String tokenA = "0x4d7fa587ec8e50bd0e9cd837cb4da796f47218a1";   // SAFE
        String tokenB = "0x55d398326f99059ff775485246999027b3197955";   // USDT

        String[] sorted = sort(tokenA,tokenB);
        System.out.println("token0:"+sorted[0]);
        System.out.println("token1:"+sorted[1]);

        String pairAddress = getPairAddressForSafeSwap(tokenA,tokenB);
        System.out.println("PairAddress:"+pairAddress);
    }

}
