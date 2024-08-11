package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.anwang.types.safe3.AvailableSafe3Info
import com.anwang.types.safe3.LockedSafe3Info
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
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
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
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
		private val bitcoinCore: BitcoinCore
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

	private var privateKey: String = ""

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
		if (!tempAddressState.validAddress?.hex.equals(addressState.validAddress?.hex, true)) {
			existAvailable = false
			existLocked = false
			existMasterNode = false

			step = 1
			check()
		}
		emitState()
	}

	fun onEnterPrivateKey(privateKey: String) {
		if (privateKey.startsWith("0x") && privateKey.length == 66) {
			this.privateKey = privateKey
			privateKeyError = false
			step = 3
			emitState()
		} else {
			privateKeyError = true
			emitState()
		}
	}

	override fun createState() = RedeemSafe3Module.RedeemSafe3UiState(
			step,
			addressState.addressError,
			convertLockInfo(),
			availableBalance(),
			lockBalance(),
			privateKeyError,
			existAvailable,
			existLocked,
			existAvailable || existLocked,
			getSafe4Address(privateKey),
			if (maxLockedCount == -1) 0 else maxLockedCount,
			masterLockBalance()
	)

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
				NodeCovertFactory.formatSafe(it)
			}
		} else {
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

	private fun check() {
		if (addressState.validAddress == null) return
		viewModelScope.launch(Dispatchers.IO) {
			try {
				existAvailable = safe4.existAvailableNeedToRedeem(addressState.validAddress!!.hex)
				existLocked = safe4.existLockedNeedToRedeem(addressState.validAddress!!.hex)
				existMasterNode = safe4.existMasterNodeNeedToRedeem(addressState.validAddress!!.hex)
				getAvailableSafe3Info()
				loadItems(0)
				step = 2
			} catch (e: Exception) {
				Log.e("Redeem", "error=$e")
			}
		}
	}

	fun redeem() {
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
				step = 4
				emitState()
			} catch (e: Exception) {
				Log.e("Redeem", "redeem error=$e")
				sendResult = SendResult.Failed(createCaution(e))
			}
		}
	}

	private fun getAvailableSafe3Info() {
		if (!addressState.canBeSend) return
		viewModelScope.launch(Dispatchers.IO) {
			try {
				availableSafe3Info = safe4.safe3GetAvailableInfo(addressState.validAddress!!.hex).blockingGet()
				emitState()
			} catch (e: Exception) {
				Log.e("Redeem", "getAvailableSafe3Info error=$e")
			}
		}
	}

	private fun loadItems(page: Int) {
		if (!addressState.canBeSend) return
		if (loading.get()) return
		loading.set(true)
		if (maxLockedCount == -1) {
			maxLockedCount = safe4.safe3GetLockedNum(addressState.validAddress!!.hex).blockingGet().toInt()
		}
		val itemsCount = page * itemsPerPage
		if (itemsCount >= maxLockedCount) {
			loading.set(false)
			return
		}
		val single = safe4.safe3GetLockedInfo(addressState.validAddress!!.hex, itemsCount, maxLockedCount)
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

	private fun getSafe4Address(privateKey: String): String? {
		if (privateKey.isNullOrBlank())	return null
		return EthereumKit.ethereumAddress(Numeric.toBigInt(privateKey)).eip55
	}

	fun loadNext() {
		if (!allLoaded.get()) {
			viewModelScope.launch(Dispatchers.IO) {
				loadItems(loadedPageNumber + 1)
			}
		}
	}

	fun redeemCurrentWalletSafe3() {
		getCurrentWalletAddress()
	}

	private fun getCurrentWalletAddress() {
		val publicKeysUsed = bitcoinCore.storage.getPublicKeysUsed()
		val size = publicKeysUsed.size
		publicKeysUsed.forEach {
			val address = bitcoinCore.addressConverter.convert(it, ScriptType.P2PKH).stringValue
		}
	}

	override fun onCleared() {
		super.onCleared()
		disposables.clear()
	}

	companion object {
		const val itemsPerPage = 10
	}
}