package io.horizontalsystems.bankwallet.core.managers;

import android.util.Log;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class NodeRegister {

    public static List<Type> decodeCreateSuperNode(String raw) {
        //                val function = Function("register",
//                        Arrays.asList<Type<*>>(
//                        Bool(isUnion),
//                        addr,
//                        Uint256(lockDay),
//                        Utf8String(name),
//                        Utf8String(enode),
//                        Utf8String(description),
//                        Uint256(creatorIncentive),
//                        Uint256(partnerIncentive),
//                        Uint256(voterIncentive)), emptyList<TypeReference<*>>())

        List outputTypes = new ArrayList<TypeReference<Type>>();
        outputTypes.add(new TypeReference<Bool>(){});
        outputTypes.add(new TypeReference<Address>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        outputTypes.add(new TypeReference<Utf8String>(){});
        outputTypes.add(new TypeReference<Utf8String>(){});
        outputTypes.add(new TypeReference<Utf8String>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        List types = FunctionReturnDecoder.decode(raw.substring(8), outputTypes);
        return types;
    }

    public static List<Type> decodeCreateMasterNode(String raw) {

        //Arrays.asList(new
        // Bool(isUnion),
        // addr, new
        // Uint256(lockDay), new
        // Utf8String(enode), new
        // Utf8String(description), new
        // Uint256(creatorIncentive), new
        // Uint256(partnerIncentive))
        List outputTypes = new ArrayList<TypeReference<Type>>();
        outputTypes.add(new TypeReference<Bool>(){});
        outputTypes.add(new TypeReference<Address>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        outputTypes.add(new TypeReference<Utf8String>(){});
        outputTypes.add(new TypeReference<Utf8String>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        outputTypes.add(new TypeReference<Uint256>(){});
        List types = FunctionReturnDecoder.decode(raw.substring(8), outputTypes);
        return types;
    }

}
