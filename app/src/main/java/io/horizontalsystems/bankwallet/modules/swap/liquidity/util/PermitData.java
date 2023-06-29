package io.horizontalsystems.bankwallet.modules.swap.liquidity.util;

import java.math.BigInteger;

public class PermitData {
    public Types types = new Types();

    public String primaryType = "Permit";

    public Domain domain = new Domain();

    public Message message = new Message();

    public static class Domain {

        public String name;

        public String version;

        public Integer chainId;

        public String verifyingContract;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getChainId() {
            return chainId;
        }

        public void setChainId(Integer chainId) {
            this.chainId = chainId;
        }

        public String getVerifyingContract() {
            return verifyingContract;
        }

        public void setVerifyingContract(String verifyingContract) {
            this.verifyingContract = verifyingContract;
        }
    }

    public static class Message {


        public String owner;
        public String spender;
        public BigInteger value;
        public String nonce;
        public long deadline;

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getSpender() {
            return spender;
        }

        public void setSpender(String spender) {
            this.spender = spender;
        }

        public BigInteger getValue() {
            return value;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public long getDeadline() {
            return deadline;
        }

        public void setDeadline(long deadline) {
            this.deadline = deadline;
        }
    }

    private static class Types {

        public TypeDefine[] EIP712Domain = {
                new TypeDefine("name", "string"),
                new TypeDefine("version", "string"),
                new TypeDefine("chainId", "uint256"),
                new TypeDefine("verifyingContract", "address"),
        };

        public TypeDefine[] Permit = {
                new TypeDefine("owner", "address"),
                new TypeDefine("spender", "address"),
                new TypeDefine("value", "uint256"),
                new TypeDefine("nonce", "uint256"),
                new TypeDefine("deadline", "uint256"),
        };

        public TypeDefine[] getEIP712Domain() {
            return EIP712Domain;
        }

        public void setEIP712Domain(TypeDefine[] EIP712Domain) {
            this.EIP712Domain = EIP712Domain;
        }

        public TypeDefine[] getPermit() {
            return Permit;
        }

        public void setPermit(TypeDefine[] permit) {
            Permit = permit;
        }
    }

    private static class TypeDefine {
        String name;
        String type;

        public TypeDefine(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public Types getTypes() {
        return types;
    }

    public void setTypes(Types types) {
        this.types = types;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
