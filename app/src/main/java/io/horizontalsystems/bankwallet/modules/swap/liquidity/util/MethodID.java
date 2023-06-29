package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

public class MethodID {

    public static String generate(String methodSignature) {
        final byte[] input = methodSignature.getBytes();
        final byte[] hash = Hash.sha3(input);
        return Numeric.toHexString(hash).substring(0, 10);
    }

}
