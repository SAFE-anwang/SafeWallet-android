package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;

import java.util.ArrayList;
import java.util.List;

public class Name {

    public static String name(Web3j web3j , String pairAddress) throws Exception {
        String encode = FunctionEncoder.encode(MethodID.generate("name()") , new ArrayList<>());
        String raw = ReadonlyContractCall.call(web3j , Constants.NULL_ADDRESS, pairAddress , encode);
        List outputTypes = new ArrayList<TypeReference<Type>>();
        outputTypes.add(new TypeReference<Utf8String>(){});
        List<Type> result = FunctionReturnDecoder.decode(raw , outputTypes);
        return  ((Utf8String)result.get(0) ).getValue();
    }


}
