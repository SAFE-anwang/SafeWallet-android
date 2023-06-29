package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class ApproveMAX {

    public static String encode(String spender) throws Exception{
        BigInteger n = new BigInteger("2");
        BigInteger UINT256_MAX = n.pow(256) .subtract( BigInteger.ONE );
        Function approve = FunctionEncoder.makeFunction(
                "approve",
                Arrays.asList("address","uint256"),
                Arrays.asList(
                        new Address(spender),
                        UINT256_MAX
                ),
                new ArrayList<>()
        );
        return FunctionEncoder.encode(approve);
    }

}
