package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BalanceOf {

    public static BigInteger balanceOf(Web3j web3j , String address , String walletAddress) throws Exception {
        List list = new ArrayList<Type>();
        list.add(new Address(walletAddress));
        String encode = FunctionEncoder.encode(MethodID.generate("balanceOf(address)") , list);
        String raw = ReadonlyContractCall.call(web3j , Constants.NULL_ADDRESS, address , encode);
        List outputTypes = new ArrayList<TypeReference<Type>>();
        outputTypes.add(new TypeReference<Uint256>(){});
        List<Type> result = FunctionReturnDecoder.decode(raw , outputTypes);
        return  ((Uint256)result.get(0)).getValue();
    }

}
