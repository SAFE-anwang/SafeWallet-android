package io.horizontalsystems.bankwallet.modules.nftv2.src721

import android.util.Log
import com.anwang.src721.SRC721
import com.anwang.src721.SRC721Burnable
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module.getSafeChain
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Single
import org.web3j.abi.datatypes.Address
import org.web3j.protocol.Web3j
import java.math.BigInteger

/**
 * SAFE4 标准 SRC721（NFT）合约封装，基于 anwang SDK 的 com.anwang.src721.SRC721。
 *
 * 两种初始化方式：
 * - 无合约地址（contract = ""，默认）：用于发布（deploy）新 NFT 合约
 * - 有合约地址：用于操作已有的 SRC721 合约
 *
 * 写方法返回 Single（交易ID / [合约地址, 交易ID]），失败走 onError；
 * 读方法为阻塞调用，需在 IO 线程执行，失败抛异常。
 */
class SRC721Service(
    val web3j: Web3j,
    val contract: String = "",
) {

    private val src721 = SRC721(web3j, getSafeChain().id.toLong(), contract)
    private val src721Burnable = SRC721Burnable(web3j, getSafeChain().id.toLong(), contract)

    // region 发布与写操作

    /**
     * 发布 SRC721 合约。
     * 成功后 privateKey 对应地址为合约 owner。
     * 返回 [合约地址, 交易ID]。
     */
    fun deploy(
        privateKey: String,
        name: String,
        symbol: String,
        baseURI: String,
        maxSupply: BigInteger,
        mintPrice: BigInteger
    ): Single<List<String>> = call("deploy") {
        src721.deploy(privateKey, name, symbol, baseURI, maxSupply, mintPrice)
    }

    /** 发布可销毁的 SRC721Burnable 合约，返回 [合约地址, 交易ID] */
    fun deployBurnable(
        privateKey: String,
        name: String,
        symbol: String,
        baseURI: String,
        maxSupply: BigInteger,
        mintPrice: BigInteger
    ): Single<List<String>> = call("deployBurnable") {
        src721Burnable.deploy(privateKey, name, symbol, baseURI, maxSupply, mintPrice)
    }

    /** 销毁指定 tokenId 的 NFT（仅 SRC721Burnable 合约） */
    fun burn(privateKey: String, tokenId: BigInteger): Single<String> = call("burn") {
        src721Burnable.burn(privateKey, tokenId)
    }

    /** 铸造（非合约 owner），value = amount * mintPrice，需在 allowList 中有额度 */
    fun mint(
        privateKey: String,
        value: BigInteger,
        to: String,
        amount: BigInteger
    ): Single<String> = call("mint") {
        src721.mint(privateKey, value, Address(to), amount)
    }

    /** 铸造（合约 owner），无需支付铸造费用 */
    fun adminMint(
        privateKey: String,
        to: String,
        amount: BigInteger
    ): Single<String> = call("adminMint") {
        src721.adminMint(privateKey, Address(to), amount)
    }

    /** 由合约 owner 提取合约中的 SAFE */
    fun withdraw(privateKey: String): Single<String> = call("withdraw") {
        src721.withdraw(privateKey)
    }

    /** 授权指定地址操作指定 tokenId 的 NFT */
    fun approve(
        privateKey: String,
        to: String,
        tokenId: BigInteger
    ): Single<String> = call("approve") {
        src721.approve(privateKey, Address(to), tokenId)
    }

    /** NFT 转账 */
    fun safeTransferFrom(
        privateKey: String,
        from: String,
        to: String,
        tokenId: BigInteger
    ): Single<String> = call("safeTransferFrom") {
        src721.safeTransferFrom(privateKey, Address(from), Address(to), tokenId)
    }

    /** 设置 baseURI（合约 owner） */
    fun setBaseURI(privateKey: String, baseURI: String): Single<String> = call("setBaseURI") {
        src721.setBaseURI(privateKey, baseURI)
    }

    /** 设置铸造价格（合约 owner） */
    fun setMintPrice(privateKey: String, mintPrice: BigInteger): Single<String> = call("setMintPrice") {
        src721.setMintPrice(privateKey, mintPrice)
    }

    /** 设置最大供应量（合约 owner） */
    fun setMaxSupply(privateKey: String, maxSupply: BigInteger): Single<String> = call("setMaxSupply") {
        src721.setMaxSupply(privateKey, maxSupply)
    }

    /** 设置允许铸造明细（合约 owner），addresses 与 amounts 个数需对应 */
    fun setAllowList(
        privateKey: String,
        addresses: List<String>,
        amounts: List<BigInteger>
    ): Single<String> = call("setAllowList") {
        src721.setAllowList(privateKey, addresses.map { Address(it) }, amounts)
    }

    /** 设置组织名 */
    fun setOrgName(privateKey: String, orgName: String): Single<String> = call("setOrgName") {
        src721.setOrgName(privateKey, orgName)
    }

    /** 设置 logo（最大 128K，费用见 getLogoPayAmount） */
    fun setLogo(privateKey: String, logo: ByteArray): Single<String> = call("setLogo") {
        src721.setLogo(privateKey, logo)
    }

    /** 设置描述信息 */
    fun setDescription(privateKey: String, description: String): Single<String> = call("setDescription") {
        src721.setDescription(privateKey, description)
    }

    /** 设置官网 URL */
    fun setOfficialUrl(privateKey: String, officialUrl: String): Single<String> = call("setOfficialUrl") {
        src721.setOfficialUrl(privateKey, officialUrl)
    }

    /** 设置白皮书 URL */
    fun setWhitePaperUrl(privateKey: String, whitePaperUrl: String): Single<String> = call("setWhitePaperUrl") {
        src721.setWhitePaperUrl(privateKey, whitePaperUrl)
    }

    // endregion

    // region 读操作

    /** 获取合约 owner 地址 */
    fun owner(): String = src721.owner().value

    /** 获取 NFT 名称 */
    fun name(): String = src721.name()

    /** 获取 NFT 缩写 */
    fun symbol(): String = src721.symbol()

    /** 获取 NFT 基础 URI */
    fun baseURI(): String = src721.baseURI()

    /** 获取 NFT 铸造价格 */
    fun mintPrice(): BigInteger = src721.mintPrice()

    /** 获取 NFT 最大供应量 */
    fun maxSupply(): BigInteger = src721.maxSupply()

    /** 获取 NFT 当前供应量 */
    fun totalSupply(): BigInteger = src721.totalSupply()

    /** 获取 NFT 剩余可铸造量 */
    fun remainSupply(): BigInteger = src721.remainSupply()

    /** 获取账户 NFT 余额 */
    fun balanceOf(address: String): BigInteger = src721.balanceOf(Address(address))

    /** 根据 tokenId 获取所有者地址 */
    fun ownerOf(tokenId: BigInteger): String = src721.ownerOf(tokenId).value

    /** 在已铸造 NFT 中根据序号查询 tokenId，范围 [0, totalSupply) */
    fun tokenByIndex(index: BigInteger): BigInteger = src721.tokenByIndex(index)

    /** 根据地址和序号查询 tokenId，范围 [0, balanceOf(owner)) */
    fun tokenOfOwnerByIndex(owner: String, index: BigInteger): BigInteger =
        src721.tokenOfOwnerByIndex(Address(owner), index)

    /** 获取账户可铸造数量 */
    fun amountAllowToMint(address: String): BigInteger = src721.amountAllowToMint(Address(address))

    /** 判断账户是否可铸造 */
    fun canMint(address: String): Boolean = src721.canMint(Address(address))

    /** 获取 tokenId 对应的授权地址，全 0 表示未授权 */
    fun getApproved(tokenId: BigInteger): String = src721.getApproved(tokenId).value

    /** 获取 NFT URI（baseURI + "/" + tokenId） */
    fun tokenURI(tokenId: BigInteger): String = src721.tokenURI(tokenId)

    /** 获取组织名 */
    fun orgName(): String = src721.orgName()

    /** 获取 logo（二进制数据） */
    fun logo(): ByteArray = src721.logo()

    /** 获取描述信息 */
    fun description(): String = src721.description()

    /** 获取官网 URL */
    fun officialUrl(): String = src721.officialUrl()

    /** 获取白皮书 URL */
    fun whitePaperUrl(): String = src721.whitePaperUrl()

    /** 获取版本号 */
    fun version(): String = src721.version()

    /** 获取设置 Logo 的费用 */
    fun getLogoPayAmount(): BigInteger = src721.getLogoPayAmount()

    /** 获取设置 Logo 费用的接收地址 */
    fun getLogoPayAddress(): String = src721.getLogoPayAddress().value

    // endregion

    private fun <T : Any> call(method: String, block: () -> T): Single<T> {
        return Single.create<T> { emitter ->
            try {
                emitter.onSuccess(block())
            } catch (e: Throwable) {
                Log.e("SRC721.$method", "error=$e")
                emitter.onError(e)
            }
        }
    }

    companion object {
        /**
         * 基于当前活跃账户的 SafeFour RPC 创建实例。
         * contractAddress 为空时用于发布合约，否则用于操作已有 NFT。
         */
        fun create(contractAddress: String = ""): SRC721Service {
            val account = App.accountManager.activeAccount
                ?: throw IllegalStateException("No active account")
            val evmKitWrapper = App.evmBlockchainManager
                .getEvmKitManager(BlockchainType.SafeFour)
                .getEvmKitWrapper(account, BlockchainType.SafeFour)
            val web3j = (evmKitWrapper.evmKit.blockchain as RpcBlockchainSafe4).web3j
            return SRC721Service(web3j, contractAddress)
        }
    }
}
