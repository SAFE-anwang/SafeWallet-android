package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.anwang.types.safe3.AvailableSafe3Info
import com.anwang.types.safe3.LockedSafe3Info
import com.anwang.utils.Safe3Util
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.storage.RedeemStorage
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.createCaution
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class RedeemSafe3ViewModel(
		val wallet: Wallet,
		val safe3Wallet: Wallet,
		private val addressService: SendBitcoinAddressService,
		private val safe4: RpcBlockchainSafe4,
		private val evmKitWrapper: EvmKitWrapper,
		private val bitcoinCore: BitcoinCore,
		private val redeemStorage: RedeemStorage,
		val isOneClickMigration: Boolean = false,
): ViewModelUiState<RedeemSafe3Module.RedeemSafe3UiState>() {

	private var step = 1
	private var addressState = addressService.stateFlow.value
	private var availableSafe3Info: AvailableSafe3Info? = null
	private var lockList: List<LockedSafe3Info>? = null

	private var loadedPageNumber = 0
	private val loading = AtomicBoolean(false)
	private var allLoaded = AtomicBoolean(false)
	private var maxLockedCount: Int = -1
	private var existAvailable = false
	private var existLocked = false
	private var existMasterNode = false
	private var privateKeyError = false

	private var showConfirmationDialg = false
	private var privateKey: String = ""

	private var isRedeeming = AtomicBoolean(false)

	var sendResult by mutableStateOf<SendResult?>(null)

	private val disposables = CompositeDisposable()

	init {
		addressService.stateFlow.collectWith(viewModelScope) {
			handleUpdatedAddressState(it)
		}
	}

	private fun handleUpdatedAddressState(addressState: SendBitcoinAddressService.State) {
		val tempAddressState = this.addressState
		this.addressState = addressState
//		if (!tempAddressState.validAddress?.hex.equals(addressState.validAddress?.hex, true)) {
			existAvailable = false
			existLocked = false
			existMasterNode = false
			if (addressState.canBeSend) {
				step = 1
				check(addressState.validAddress!!.hex)
			}
//		}
		emitState()
	}

	fun onEnterPrivateKey(privateKey: String) {
		try {
			val privKey = Numeric.toBigInt(privateKey)
			val compressedPublicKey = Safe3Util.getCompressedPublicKey(privKey)
			val compressedSafe3Addr = Safe3Util.getSafe3Addr(compressedPublicKey)
			if (compressedSafe3Addr.equals(addressState.validAddress?.hex)) {
				this.privateKey = privateKey
				privateKeyError = false
				step = 3
			} else {
				privateKeyError = true
			}
		} catch (e: Exception) {
			privateKeyError = true
		}
		emitState()
	}

	fun receiveAddress(): String {
		return evmKitWrapper.evmKit.receiveAddress.eip55
	}

	fun closeDialog() {
		showConfirmationDialg = false
		emitState()
	}

	override fun createState() : RedeemSafe3Module.RedeemSafe3UiState{
		val lockValue = lockBalance()
		return RedeemSafe3Module.RedeemSafe3UiState(
				step,
				addressState.addressError,
				convertLockInfo(),
				availableBalance(),
				lockValue,
				privateKeyError,
				existAvailable,
				existLocked,
				existAvailable || existLocked,
				getSafe4Address(privateKey),
				if (maxLockedCount == -1) 0 else maxLockedCount,
				masterLockBalance(),
				showConfirmationDialg
		)
	}

	private fun  availableBalance(): String {
		return if (availableSafe3Info != null) {
			NodeCovertFactory.formatSafe(availableSafe3Info!!.amount)
		} else {
			NodeCovertFactory.formatSafe(BigInteger.ZERO)
		}
	}

	private fun lockBalance(): String {
		return if (lockList?.isNotEmpty() == true) {
			lockList!!.sumOf { it.amount }.let {
				if (it == BigInteger.ZERO)	existLocked = false
				NodeCovertFactory.formatSafe(it)
			}
		} else {
			existLocked = false
			NodeCovertFactory.formatSafe(BigInteger.ZERO)
		}
	}

	private fun masterLockBalance(): String? {
		return if (lockList?.isNotEmpty() == true) {
			lockList!!.filter { it.isMN }.sumOf { it.amount }.let {
				NodeCovertFactory.formatSafe(it)
			}
		} else {
			null
		}
	}

	private fun convertLockInfo(): List<RedeemSafe3Module.Safe3LockItemView>? {
		return lockList?.map {
			RedeemSafe3Module.Safe3LockItemView(
					it.safe3Addr,
					NodeCovertFactory.formatSafe(it.amount),
					it.txid,
					it.lockHeight.toLong(),
					it.unlockHeight.toLong(),
					it.remainLockHeight.toLong(),
					it.lockDay.toInt(),
					it.isMN,
					it.safe4Addr.value,
					it.redeemHeight.toLong()
			)
		}
	}

	fun onEnterAddress(address: Address?) {
		step = 1
		addressService.setAddress(address)
	}

	private fun checkNeedToRedeem(address: String) {
		existAvailable = safe4.existAvailableNeedToRedeem(address)
		existLocked = safe4.existLockedNeedToRedeem(address)
		existMasterNode = safe4.existMasterNodeNeedToRedeem(address)
	}

	private fun check(address: String?) {
		if (address == null) return
		viewModelScope.launch(Dispatchers.IO) {
			try {
				checkNeedToRedeem(address)
				availableSafe3Info = getAvailableSafe3Info(address)
				loadItems(0, address)
				step = 2
			} catch (e: Exception) {
				Log.e("Redeem", "error=$e")
			}
		}
	}

	fun redeem() {
		redeem(privateKey)
	}
	fun redeem(privateKey: String) {
		sendResult = SendResult.Sending
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val redeemResult = safe4.redeemSafe3(privateKey).blockingGet()
				if (existMasterNode) {
					safe4.redeemMasterNode(privateKey, "")
				}
				sendResult = SendResult.Sent
				existAvailable = false
				existLocked = false
				existMasterNode = false
				step = 1
				emitState()
			} catch (e: Exception) {
				Log.e("Redeem", "redeem error=$e")
				sendResult = SendResult.Failed(createCaution(e))
			}
		}
	}

	private fun getAvailableSafe3Info(address: String): AvailableSafe3Info? {
		return try {
			 safe4.safe3GetAvailableInfo(address).blockingGet()
		} catch (e: Exception) {
			Log.e("Redeem", "getAvailableSafe3Info error=$e")
			null
		}
	}

	private fun loadItems(page: Int, address: String) {
		if (loading.get()) return
		loading.set(true)
		if (maxLockedCount == -1) {
			maxLockedCount = safe4.safe3GetLockedNum(address).blockingGet().toInt()
		}
		val itemsCount = page * itemsPerPage
		if (itemsCount >= maxLockedCount) {
			loading.set(false)
			return
		}
		val single = safe4.safe3GetLockedInfo(address, itemsCount, maxLockedCount)
		single.subscribeOn(Schedulers.io())
				.doFinally {
					loading.set(false)
				}
				.subscribe( {
					allLoaded.set(it.isEmpty() || it.size < itemsPerPage)

					lockList = it
					loadedPageNumber = page
					emitState()
				},{
					Log.e("Redeem", "get locked error=$it")
				}).let {
					disposables.add(it)
				}
	}

	private fun getLockedAmount(address: String): Pair<BigInteger, BigInteger> {
		val maxLockedCount = safe4.safe3GetLockedNum(address).blockingGet().toInt()
		val listLockInfo = safe4.safe3GetLockedInfo(address, 0, maxLockedCount).blockingGet()
		val lockAmount = if (listLockInfo?.isNotEmpty() == true) {
			listLockInfo!!.sumOf { it.amount }.let {
				it
			}
		} else {
			BigInteger.ZERO
		}
		val materLockAmount = if (listLockInfo?.isNotEmpty() == true) {
			listLockInfo!!.filter { it.isMN }.sumOf { it.amount }.let {
				it
			}
		} else {
			BigInteger.ZERO
		}
		return Pair(lockAmount, materLockAmount)
	}

	private fun getSafe4Address(privateKey: String): String? {
		if (privateKey.isNullOrBlank())	return null
		return EthereumKit.ethereumAddress(Numeric.toBigInt(privateKey)).eip55
	}

	fun loadNext() {
		if (!allLoaded.get()) {
			viewModelScope.launch(Dispatchers.IO) {
				loadItems(loadedPageNumber + 1, addressState.validAddress!!.hex)
			}
		}
	}

	fun showConfirmation() {
		if (isRedeeming.get())	return
		showConfirmationDialg = true
		emitState()
	}

	fun redeemCurrentWalletSafe3() {
		closeDialog()
		sendResult = SendResult.Sending
		isRedeeming.set(true)
		viewModelScope.launch(Dispatchers.IO) {
			val alreadyRedeem = redeemStorage.allRedeem()
			val unspentOutputs = bitcoinCore.storage.getUnspentOutputs().distinctBy { it.transaction.blockHash.toHexString() }

			unspentOutputs.forEach {
				val address = bitcoinCore.addressConverter.convert(it.publicKey, ScriptType.P2PKH).stringValue
				val isSuccess = (alreadyRedeem.find { it.address == address }?.success ?: 0) == 1
				if (isSuccess)	return@forEach
				try {
					val existAvailable = safe4.existAvailableNeedToRedeem(address)
					val existLocked = safe4.existLockedNeedToRedeem(address)
					val existMasterNode = safe4.existMasterNodeNeedToRedeem(address)
					if (existAvailable || existLocked || existMasterNode) {
						redeemStorage.save(Redeem(
								address,
								existAvailable,
								existLocked,
								existMasterNode,
								false
						))
//						val safe3Info = getAvailableSafe3Info(address) ?: return@forEach
//						bitcoinCore.getPrivateKey(it.publicKey)?.let { privateKey ->
//							val (amount1, amount2) = getLockedAmount(address)
							Log.d("Redeem", "existAvailable=$existAvailable, existLocked=$existLocked")
							Log.e("Redeem", "redeemCurrentWalletSafe3: $address= ${evmKitWrapper.signer?.privateKey?.toHexString()}")

							safe4.redeemSafe3(evmKitWrapper.signer!!.privateKey!!.toHexString()).blockingGet()
							if (existMasterNode) {
								safe4.redeemMasterNode(evmKitWrapper.signer!!.privateKey!!.toHexString(), "")
							}

//						}
					}
					redeemStorage.save(Redeem(
							address,
							existAvailable,
							existLocked,
							existMasterNode,
							true
					))
				} catch (e: Exception) {
					Log.e("Redeem", "redeemCurrentWalletSafe3 erro=$e")
				}
			}
			isRedeeming.set(false)
			sendResult = SendResult.Sent
		}
	}

	override fun onCleared() {
		super.onCleared()
		disposables.clear()

		sendResult = SendResult.Sent
	}

	companion object {
		const val itemsPerPage = 10
	}
}