package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.util.Log
import com.anwang.src20.SRC20
import com.anwang.src20.SRC20Burnable
import com.anwang.src20.SRC20Mintable
import io.horizontalsystems.ethereumkit.models.Chain
import io.reactivex.Single
import org.web3j.abi.datatypes.Address
import org.web3j.protocol.Web3j
import java.math.BigInteger

class SRC20Service(
    val type: DeployType,
    val web3j: Web3j,
    val contract: String = "",
) {

    private val src20 = SRC20(web3j, Chain.SafeFour.id.toLong(), contract)
    private val src20Burnable = SRC20Burnable(web3j, Chain.SafeFour.id.toLong(), contract)
    private val src20Mintble = SRC20Mintable(web3j, Chain.SafeFour.id.toLong(), contract)


    fun src20Deploy(
        privateKey: String,
        name: String,
        symbol: String,
        totalSupply: BigInteger
    ): Single<List<String>> {
        return Single.create<List<String>> { emitter ->
            try {
                val result = src20.deploy(
                    privateKey,
                    name,
                    symbol,
                    totalSupply
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun src20MintableDeploy(
        privateKey: String,
        name: String,
        symbol: String,
        totalSupply: BigInteger
    ): Single<List<String>> {
        return Single.create<List<String>> { emitter ->
            try {
                val result = src20Mintble.deploy(
                    privateKey,
                    name,
                    symbol,
                    totalSupply
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun src20MintableMint(
        privateKey: String,
        address: String,
        amount: BigInteger
    ): Single<String> {
        return Single.create<String> { emitter ->
            try {
                val result = src20Mintble.mint(
                    privateKey,
                    org.web3j.abi.datatypes.Address(address),
                    amount
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun src20BurnableDeploy(
        privateKey: String,
        name: String,
        symbol: String,
        totalSupply: BigInteger
    ): Single<List<String>> {
        return Single.create<List<String>> { emitter ->
            try {
                val result = src20Burnable.deploy(
                    privateKey,
                    name,
                    symbol,
                    totalSupply
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun src20BurnableMint(
        privateKey: String,
        address: String,
        amount: BigInteger
    ): Single<String> {
        return Single.create<String> { emitter ->
            try {
                val result = src20Burnable.mint(
                    privateKey,
                    org.web3j.abi.datatypes.Address(address),
                    amount
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun src20BurnableBurn(privateKey: String, amount: BigInteger): Single<String> {
        return Single.create<String> { emitter ->
            try {
                val result = src20Burnable.burn(
                    privateKey,
                    amount
                )
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun getLogoPayAmount(type: Int): Single<BigInteger> {
        return Single.create<BigInteger> { emitter ->
            try {
                val result = if (type == 0) {
                    src20.logoPayAmount
                } else if (type == 1) {
                    src20Mintble.logoPayAmount
                } else {
                    src20Burnable.logoPayAmount
                }
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("src20Deploy", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun setLogoPayAmount(
        type: Int,
        privateKey: String,
        logo: ByteArray
    ): Single<String> {
        return Single.create<String> { emitter ->
            try {
                val result = if (type == 0) {
                    src20.setLogo(privateKey, logo)
                } else if (type == 1) {
                    src20Mintble.setLogo(privateKey, logo)
                } else {
                    src20Burnable.setLogo(privateKey, logo)
                }
                emitter.onSuccess(result)
            } catch (e: Throwable) {
                Log.e("setLogo", "error=$e")
                emitter.onError(e)
            }
        }
    }

    fun orgName(type: Int): String {
        return if (type == 0) {
            src20.orgName()
        } else if (type == 1) {
            src20Mintble.orgName()
        } else {
            src20Burnable.orgName()
        }
    }

    fun description(type: Int): String {
        return if (type == 0) {
            src20.description()
        } else if (type == 1) {
            src20Mintble.description()
        } else {
            src20Burnable.description()
        }
    }

    fun officialUrl(type: Int): String {
        return if (type == 0) {
            src20.officialUrl()
        } else if (type == 1) {
            src20Mintble.officialUrl()
        } else {
            src20Burnable.officialUrl()
        }
    }

    fun whitePaperUrl(type: Int): String {
        return if (type == 0) {
            src20.whitePaperUrl()
        } else if (type == 1) {
            src20Mintble.whitePaperUrl()
        } else {
            src20Burnable.whitePaperUrl()
        }
    }

    fun setOrgName(type: Int, privateKey: String, orgName: String): String {
        return if (type == 0) {
            src20.setOrgName(privateKey, orgName)
        } else if (type == 1) {
            src20Mintble.setOrgName(privateKey, orgName)
        } else {
            src20Burnable.setOrgName(privateKey, orgName)
        }
    }

    fun setDescription(type: Int, privateKey: String, description: String): String {
        return if (type == 0) {
            src20.setDescription(privateKey, description)
        } else if (type == 1) {
            src20Mintble.setDescription(privateKey, description)
        } else {
            src20Burnable.setDescription(privateKey, description)
        }
    }

    fun setOfficialUrl(type: Int, privateKey: String, officialUrl: String): String {
        return if (type == 0) {
            src20.setOfficialUrl(privateKey, officialUrl)
        } else if (type == 1) {
            src20Mintble.setOfficialUrl(privateKey, officialUrl)
        } else {
            src20Burnable.setOfficialUrl(privateKey, officialUrl)
        }
    }

    fun setWhitePaperUrl(type: Int, privateKey: String, whitePaperUrl: String): String {
        return if (type == 0) {
            src20.setWhitePaperUrl(privateKey, whitePaperUrl)
        } else if (type == 1) {
            src20Mintble.setWhitePaperUrl(privateKey, whitePaperUrl)
        } else {
            src20Burnable.setWhitePaperUrl(privateKey, whitePaperUrl)
        }
    }

    fun getVersion(contract: String, chainId: String): String? {
        try {
            val src20 = SRC20(web3j, chainId.toLong(), contract)
            return src20.version()
        } catch (e: Exception) {
            return null
        }
    }


    fun totalSupply(type: Int): BigInteger {
        return if (type == 0) {
            src20.totalSupply()
        } else if (type == 1) {
            src20Mintble.totalSupply()
        } else {
            src20Burnable.totalSupply()
        }
    }

    fun balance(address: String): BigInteger {
        return src20Burnable.balanceOf(Address(address))
    }

}