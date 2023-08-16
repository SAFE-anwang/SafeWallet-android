package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.ReadonlyTransactionManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class ReadonlyContractCall {

    public static String call(
            Web3j admin ,
            String from ,
            String to ,
            String raw
    ) throws IOException {
        ReadonlyTransactionManager readonlyTransactionManager = new ReadonlyTransactionManager(
                admin,
                from
        );
        return readonlyTransactionManager.sendCall(to, raw, DefaultBlockParameterName.LATEST);
    }

    public static String call(
            Web3j web3j,
            String from,
            String to,
            String fnName,
            String[] inputTypes,
            Object[] inputParams
    ) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        String[] outputTypes = new String[]{};
        Function function = FunctionEncoder.makeFunction( fnName , Arrays.asList(inputTypes) , Arrays.asList(inputParams) , Arrays.asList(outputTypes));
        String raw = FunctionEncoder.encode(function);
        return call(web3j,from,to,raw);
    }

    public static List<Type> call(
            Web3j web3j,
            String from,
            String to ,
            String fnName,
            String[] inputTypes,
            Object[] inputParams,
            String[] outputTypes
    ) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function function = FunctionEncoder.makeFunction( fnName , Arrays.asList(inputTypes) , Arrays.asList(inputParams) , Arrays.asList(outputTypes));
        return call(web3j,from,to,function);
    }

    public static List<Type> call(
            Web3j web3j,
            String from,
            String to,
            Function function
    ) throws IOException {
        String raw = FunctionEncoder.encode( function );
        String response = call(web3j,from,to,raw);
        return FunctionReturnDecoder.decode(response,function.getOutputParameters());
    }


}
