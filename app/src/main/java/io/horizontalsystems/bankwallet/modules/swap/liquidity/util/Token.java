package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

public class Token {

    private String name;

    private String symbol;

    private Integer decimals;

    private String address;

    public Token(String address ,String name, String symbol, Integer decimals ) {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Token{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", decimals=" + decimals +
                ", address='" + address + '\'' +
                '}';
    }
}
