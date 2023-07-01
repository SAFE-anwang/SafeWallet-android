package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import com.google.android.exoplayer2.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

public class TransactionContractSend {

    private static final Logger log = LoggerFactory.getLogger(TransactionContractSend.class);

    public static String send(Web3j admin,Credentials credentials, String to , String fnName , String[] inputTypes , Object[] inputParams , BigInteger value , BigInteger nonce , BigInteger gasPrice , BigInteger gasLimit) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function function = FunctionEncoder.makeFunction(fnName , Arrays.asList(inputTypes) , Arrays.asList(inputParams) , new ArrayList<>());
        String raw = FunctionEncoder.encode(function);
        return send(admin,credentials,to,raw,value,nonce,gasPrice,gasLimit);

    }

    public static String send(Web3j admin , Credentials credentials , String to , String raw , BigInteger value , BigInteger nonce , BigInteger gasPrice , BigInteger gasLimit ) throws IOException {
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                value,
                raw
        );
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction,credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return admin.ethSendRawTransaction(hexValue).send().getTransactionHash();
    }

}
