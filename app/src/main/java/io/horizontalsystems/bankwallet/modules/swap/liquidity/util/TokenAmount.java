package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class TokenAmount {

    public static BigDecimal toBigDecimal(Token token , BigInteger amount){
        Integer decimal = token.getDecimals();
        return new BigDecimal( amount ).divide( BigDecimal.TEN.pow(decimal) , decimal , RoundingMode.DOWN );
    }

    public static BigDecimal toBigDecimal(Token token , BigInteger amount, Integer scale){
        return toBigDecimal(token,amount).setScale(scale,RoundingMode.DOWN);
    }

    public static BigInteger toRawBigInteger(Token token, BigDecimal amount){
        Integer decimal = token.getDecimals();
        return amount.multiply( BigDecimal.TEN.pow(decimal) ).toBigInteger();
    }

}
