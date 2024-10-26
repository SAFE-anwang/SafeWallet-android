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
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.createCaution
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.crypto.Base58
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
		private val safe4: RpcBlockchainSafe4,
		private val evmKitWrapper: EvmKitWrapper,
): ViewModelUiState<RedeemSafe3Module.RedeemSafe3UiState>() {

	private var step = 1
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

	fun onEnterPrivateKey(privateKey: String) {
		if (privateKey.isNullOrBlank()) {
			reset()
			return
		}
		var privateKey = privateKey
		val isWIF = isWIFPrivateKey(privateKey)
		if (isWIF) {
			val base58 = Base58.decode(privateKey)
			privateKey = base58.copyOfRange(1, 33).toRawHexString()
		}
		try {
			val privKey = Numeric.toBigInt(privateKey)
			val compressedPublicKey = Safe3Util.getCompressedPublicKey(privKey)
			val compressedSafe3Addr = Safe3Util.getSafe3Addr(compressedPublicKey)
			this.privateKey = privateKey
			privateKeyError = false
			check(compressedSafe3Addr)
		} catch (e: Exception) {
			privateKeyError = true
		}
		emitState()
	}

	private fun isWIFPrivateKey(wifPrivateKey: String): Boolean {
		wifPrivateKey.lowercase().forEach {
			if (it.code >= 103) return true
		}
		return false
	}

	private fun reset() {
		privateKeyError = false
		step = 1
		availableSafe3Info = null
		existAvailable = false
		existLocked = false
		existMasterNode = false
		lockList = null
		emitState()
	}

	fun receiveAddress(): String {
		return evmKitWrapper.evmKit.receiveAddress.eip55
	}

	private fun receivePrivateKey(): String {
		return evmKitWrapper.signer!!.privateKey.toHexString()
	}

	fun closeDialog() {
		showConfirmationDialg = false
		emitState()
	}

	override fun createState() : RedeemSafe3Module.RedeemSafe3UiState{
		val lockValue = lockBalance()
		return RedeemSafe3Module.RedeemSafe3UiState(
				step,
				availableBalance(),
				lockValue,
				privateKeyError,
				existAvailable,
				existLocked,
				existAvailable || existLocked,
				getSafe4Address(privateKey),
				if (maxLockedCount == -1) 0 else maxLockedCount,
				masterLockBalance()
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
					it.remainLockHeight.toLong(),
					it.lockDay.toInt(),
					it.isMN,
					it.safe4Addr.value,
					it.redeemHeight.toLong()
			)
		}
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
				step = 3
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
				val redeemResult = safe4.redeemSafe3(receivePrivateKey(), listOf( privateKey), getSafe4Address(privateKey)!!).blockingGet()
				if (existMasterNode) {
					safe4.redeemMasterNode(receivePrivateKey(), listOf(privateKey), getSafe4Address(privateKey)!!)
				}
				sendResult = SendResult.Sent
				reset()
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
			emitState()
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


	override fun onCleared() {
		super.onCleared()
		disposables.clear()

		sendResult = SendResult.Sent
	}

	companion object {
		const val itemsPerPage = 10
	}
}