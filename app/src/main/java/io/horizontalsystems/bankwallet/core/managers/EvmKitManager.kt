package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.Looper
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supportedNftTypes
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.core.NftKit
import io.horizontalsystems.nftkit.models.NftType
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.TokenFactory.UnsupportedChainError
import io.horizontalsystems.uniswapkit.TokenFactory
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.net.URI

class EvmKitManager(
    val chain: Chain,
    backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager
) : BackgroundManager.Listener {

    private val disposables = CompositeDisposable()

    init {
        backgroundManager.registerListener(this)

        syncSourceManager.syncSourceObservable
            .subscribeIO { blockchain ->
                handleUpdateNetwork(blockchain)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdateNetwork(blockchainType: BlockchainType) {
        if (blockchainType != evmKitWrapper?.blockchainType) return

        stopEvmKit()

        evmKitUpdatedSubject.onNext(Unit)
    }

    private val kitStartedSubject = BehaviorSubject.createDefault(false)
    val kitStartedObservable: Observable<Boolean> = kitStartedSubject

    var evmKitWrapper: EvmKitWrapper? = null
        private set(value) {
            field = value

            kitStartedSubject.onNext(value != null)
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val evmKitUpdatedSubject = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedSubject

    val statusInfo: Map<String, Any>?
        get() = evmKitWrapper?.evmKit?.statusInfo()

    @Synchronized
    fun getEvmKitWrapper(account: Account, blockchainType: BlockchainType): EvmKitWrapper {
        if (evmKitWrapper != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKitWrapper == null) {
            val accountType = account.type
            evmKitWrapper = createKitInstance(accountType, account, blockchainType)
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.evmKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType,
        account: Account,
        blockchainType: BlockchainType
    ): EvmKitWrapper {
        val syncSource = syncSourceManager.getSyncSource(blockchainType)

        val address: Address
        var signer: Signer? = null
        var seed: ByteArray? = null
        var isAnBaoWallet = false

        when (accountType) {
            is AccountType.Mnemonic -> {
                isAnBaoWallet = accountType.isAnBaoWallet
                seed = accountType.seed
                address = Signer.address(seed, chain, isAnBaoWallet)
                signer = Signer.getInstance(seed, chain, isAnBaoWallet)
            }
            is AccountType.PrivateKey -> {
                address = Signer.address(accountType.key.toBigInteger())
                signer = Signer.getInstance(accountType.key, chain, isAnBaoWallet)
            }
            is AccountType.EvmPrivateKey -> {
                address = Signer.address(accountType.key)
                signer = Signer.getInstance(accountType.key, chain, isAnBaoWallet)
            }
            is AccountType.EvmAddress -> {
                address = Address(accountType.address)
            }
            else -> throw UnsupportedAccountException()
        }

        val evmKit = EthereumKit.getInstance(
            App.instance,
            address,
            chain,
            syncSource.rpcSource,
            syncSource.transactionSource,
            account.id,
            isAnBaoWallet
        )

        Erc20Kit.addTransactionSyncer(evmKit)
        Erc20Kit.addDecorators(evmKit)

        UniswapKit.addDecorators(evmKit)
        try {
            UniswapV3Kit.addDecorators(evmKit)
        } catch (e: UnsupportedChainError.NoWethAddress) {
            //do nothing
        }
        OneInchKit.addDecorators(evmKit)

        var nftKit: NftKit? = null
        val supportedNftTypes = blockchainType.supportedNftTypes
        if (supportedNftTypes.isNotEmpty()) {
            val nftKitInstance = NftKit.getInstance(App.instance, evmKit)
            supportedNftTypes.forEach {
                when (it) {
                    NftType.Eip721 -> {
                        nftKitInstance.addEip721TransactionSyncer()
                        nftKitInstance.addEip721Decorators()
                    }
                    NftType.Eip1155 -> {
                        nftKitInstance.addEip1155TransactionSyncer()
                        nftKitInstance.addEip1155Decorators()
                    }
                }
            }
            nftKit = nftKitInstance
        }

        evmKit.start()
        seed?.let {
            evmKit.getAnBaoAllAddressInfo(it)
        }

        return EvmKitWrapper(evmKit, nftKit, blockchainType, signer)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stopEvmKit()
            }
        }
    }

    private fun stopEvmKit() {
        evmKitWrapper?.evmKit?.stop()
        evmKitWrapper = null
        currentAccount = null
    }

    //
    // BackgroundManager.Listener
    //

    override fun willEnterForeground() {
        this.evmKitWrapper?.evmKit?.let { kit ->
            Handler(Looper.getMainLooper()).postDelayed({
                kit.refresh()
            }, 1000)
        }
    }

    override fun didEnterBackground() = Unit
}

val RpcSource.uris: List<URI>
    get() = when (this) {
        is RpcSource.WebSocket -> listOf(uri)
        is RpcSource.Http -> uris
    }

class EvmKitWrapper(
    val evmKit: EthereumKit,
    val nftKit: NftKit?,
    val blockchainType: BlockchainType,
    val signer: Signer?
) {

    fun sendSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
        lockTime: Int? = null
    ): Single<FullTransaction> {
        return if (signer != null) {
            if (lockTime == null) {
                evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
                        .flatMap { rawTransaction ->
                            val signature = signer.signature(rawTransaction)
                            evmKit.send(rawTransaction, signature, signer.privateKey)
                        }
            } else {
                evmKit.safe4LockRawTransaction(transactionData, gasPrice, gasLimit, lockTime, nonce)
                        .flatMap { rawTransaction ->
                            val signature = signer.signature(rawTransaction)
                            evmKit.send(rawTransaction, signature, signer.privateKey, lockTime)
                        }
            }
        } else {
            Single.error(Exception())
        }
    }

    fun withdraw() {
        if (blockchainType == BlockchainType.SafeFour && signer != null) {
            evmKit.withdraw(signer.privateKey)
        }
    }

    fun createSuperNode(
            value: BigInteger,
            isUnion: Boolean,
            addr: String,
            lockDay: BigInteger,
            name: String,
            enode: String,
            description: String,
            creatorIncentive: BigInteger,
            partnerIncentive: BigInteger,
            voterIncentive: BigInteger
    ): Single<String> {
        return if (signer != null) {
            evmKit.superNodeRegister(signer.privateKey, value, isUnion, addr, lockDay, name, enode, description, creatorIncentive, partnerIncentive, voterIncentive)
        } else {
            Single.error(Exception())
        }
    }

    fun createMasterNode(
            value: BigInteger,
            isUnion: Boolean,
            addr: String,
            lockDay: BigInteger,
            enode: String,
            description: String,
            creatorIncentive: BigInteger,
            partnerIncentive: BigInteger
    ): Single<String> {
        return if (signer != null) {
            evmKit.masterNodeRegister(signer.privateKey, value, isUnion, addr, lockDay, enode, description, creatorIncentive, partnerIncentive)
        } else {
            Single.error(Exception())
        }
    }

    fun voteOrApprovalWithAmount(value: BigInteger, isVote: Boolean, dstAddr: String): Single<String> {
        return if (signer != null) {
            evmKit.voteOrApprovalWithAmount(signer.privateKey, value, isVote,
                    dstAddr)
        } else {
            return Single.error(Exception())
        }
    }

    fun voteOrApproval(isVote: Boolean, dstAddr: String, recordIDs: List<BigInteger>): Single<String> {
        return if (signer != null) {
            evmKit.voteOrApproval(signer.privateKey, isVote, dstAddr, recordIDs)
        } else {
            return Single.error(Exception())
        }
    }
}
