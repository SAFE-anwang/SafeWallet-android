package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.anwang.safewallet.safekit.MainNetSafe
import com.anwang.types.safe3.AvailableSafe3Info
import com.anwang.types.safe3.LockedSafe3Info
import com.anwang.utils.Safe3Util
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.storage.RedeemStorage
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.createCaution
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.valueConvert
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class RedeemSafe3LocalViewModel(
		val wallet: Wallet,
		val safe3Wallet: Wallet,
		private val safe4: RpcBlockchainSafe4,
		private val evmKitWrapper: EvmKitWrapper,
		private val bitcoinCore: BitcoinCore,
		private val redeemStorage: RedeemStorage
): ViewModelUiState<RedeemSafe3Module.RedeemSafe3LocalUiState>() {

	private var step = 1

	private var maxLockedCount: Int = -1
	private var existMasterNode = false

	private var showConfirmationDialg = false
	val list = mutableListOf<RedeemSafe3Module.Safe3LocalInfo>()

	private var isRedeeming = AtomicBoolean(false)

	var sendResult by mutableStateOf<SendResult?>(null)
	private var syncing = true
	private var isRedeemSuccess = false
	private var availableAmount: Long? = null
	private var redeemableAmount: BigInteger = BigInteger.ZERO
	private var lockedAmount: Long? = null
	private var redeemableLocked: BigInteger = BigInteger.ZERO

	private val disposables = CompositeDisposable()

	init {
//		isRedeemSuccess = App.preferences.getBoolean(evmKitWrapper.evmKit.receiveAddress.hex, false)
//		if (!isRedeemSuccess) {
			getNeedToRedeemAddress()
//		} else {
//			syncing = false
//		}
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

	override fun createState() : RedeemSafe3Module.RedeemSafe3LocalUiState{
		val canRedeem = list.filter { it.existAvailable || it.existLocked || it.existMasterNode }.isNotEmpty()
		return RedeemSafe3Module.RedeemSafe3LocalUiState(
				step,
				syncing,
				!syncing && canRedeem,
				receiveAddress(),
				convert(),
				isRedeemSuccess,
				showConfirmationDialg,
			availableAmount?.let { App.numberFormatter.formatCoinFull(valueConvert(it.toBigInteger(), 8), "", 8) },
			App.numberFormatter.formatCoinFull(valueConvert(redeemableAmount), "", 8),
			lockedAmount?.let { App.numberFormatter.formatCoinFull(valueConvert(it.toBigInteger(), 8), "", 8) },
			App.numberFormatter.formatCoinFull(valueConvert(redeemableLocked), "", 8)
		)
	}

	private fun convert(): List<RedeemSafe3Module.Safe3LocalItemView> {
		if (list.isEmpty())	emptyList<RedeemSafe3Module.Safe3LocalItemView>()
		return list.map {
			RedeemSafe3Module.Safe3LocalItemView(
					it.address,
					NodeCovertFactory.formatSafe(it.safe3Balance),
					NodeCovertFactory.formatSafe(it.safe3LockBalance),
					if (it.masterNodeLock == null || it.masterNodeLock == BigInteger.ZERO) null else NodeCovertFactory.formatSafe(it.masterNodeLock),
					it.existAvailable,
					it.existLocked,
					it.existMasterNode,
					it.safe3LockNum
			)
		}
	}

	private fun  availableBalance(availableSafe3Info: AvailableSafe3Info?): String {
		return if (availableSafe3Info != null) {
			NodeCovertFactory.formatSafe(availableSafe3Info!!.amount)
		} else {
			NodeCovertFactory.formatSafe(BigInteger.ZERO)
		}
	}

	private fun lockBalance(lockList: List<LockedSafe3Info>?): BigInteger {
		return if (lockList?.isNotEmpty() == true) {
			lockList.sumOf { it.amount }
		} else {
			BigInteger.ZERO
		}
	}

	private fun masterLockBalance(lockList: List<LockedSafe3Info>?): BigInteger {
		return if (lockList?.isNotEmpty() == true) {
			lockList.filter { it.isMN }.sumOf { it.amount }
		} else {
			BigInteger.ZERO
		}
	}


	fun redeem() {
		closeDialog()
		sendResult = SendResult.Sending
		viewModelScope.launch(Dispatchers.IO) {
			val redeemableList = list.filter { it.redeemable }
			val listPrivateKey = redeemableList.filter { it.existAvailable || it.existLocked
			}.map { it.privateKey.toHexString() }
			val masterNodeKey = redeemableList.filter { it.existMasterNode }.map { it.privateKey.toHexString() }
			try {
				// 余额迁移
				safe4.redeemSafe3(receivePrivateKey(), listPrivateKey, receiveAddress())
				if (masterNodeKey.isNotEmpty()) {
					safe4.redeemMasterNode(receivePrivateKey(), masterNodeKey, receiveAddress())
				}
				redeemableList.forEach {
					redeemStorage.save(Redeem(
							it.address,
							it.existAvailable,
							it.existLocked,
							it.existMasterNode,
							true
					))
				}
				sendResult = SendResult.Sent
				isRedeemSuccess = true
				emitState()
				App.preferences.edit().putBoolean(evmKitWrapper.evmKit.receiveAddress.hex, true).commit()
			} catch (e: Exception) {
				sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
				Log.e("Redeem", "redeem error=$e")
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

	private fun loadItems(address: String): Pair<Int, List<LockedSafe3Info>?> {
		val maxLockedCount = safe4.safe3GetLockedNum(address).blockingGet().toInt()
		if (maxLockedCount <= 0) {
			return Pair(maxLockedCount, null)
		}
		val lockList = mutableListOf<LockedSafe3Info>()
		val countPage = (maxLockedCount + itemsPerPage - 1) / itemsPerPage
		var page = 1
		var start = 0
		var count = itemsPerPage
		if (count > maxLockedCount) {
			count = maxLockedCount
		}
		var list = emptyList<LockedSafe3Info>()
		while(page <= countPage) {
			list = safe4.safe3GetLockedInfo(address, start, count).blockingGet()
			lockList.addAll(list)
			start = itemsPerPage * page
			if (start + itemsPerPage > maxLockedCount) {
				count = maxLockedCount - start
			}
			page ++
		}
		return Pair(maxLockedCount, lockList)
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

	fun showConfirmation() {
		if (isRedeeming.get())	return
		showConfirmationDialg = true
		emitState()
	}

	private fun getNeedToRedeemAddress() {
		viewModelScope.launch(Dispatchers.IO) {
			val alreadyRedeem = redeemStorage.allRedeem()
			/*val unspentOutputs = allUtxo()
					.distinctBy { it.transaction.hash.toHexString() }*/
			val spendableUtxo = bitcoinCore.dataProvider.getSpendableUtxo()/*.distinctBy { it.transaction.hash.toHexString() }*/
			availableAmount = spendableUtxo.sumOf { it.output.value }
			val spendableTimeLockUtxo = bitcoinCore.dataProvider.getSpendableTimeLockUtxo()/*.distinctBy { it.transaction.hash.toHexString() }*/
			lockedAmount = spendableTimeLockUtxo.sumOf { it.output.value }
			getRedeemAddressInfo(spendableUtxo, alreadyRedeem, false)
			getRedeemAddressInfo(spendableTimeLockUtxo, alreadyRedeem, true)
			if (list.isEmpty() || (spendableUtxo.isEmpty() && spendableTimeLockUtxo.isEmpty())) {
				isRedeemSuccess = true
			}
			syncing = false
			step = 3
			isRedeeming.set(false)
			emitState()
		}
	}

	private fun getRedeemAddressInfo(unspentOutputs: List<UnspentOutput>, alreadyRedeem: List<Redeem>, isLock: Boolean) {
		val tempUnspentOutputs = unspentOutputs.distinctBy { it.transaction.hash.toHexString() }
		tempUnspentOutputs.forEach {
			val address = bitcoinCore.addressConverter.convert(it.publicKey, ScriptType.P2PKH).stringValue
			val isSuccess = (alreadyRedeem.find { it.address == address }?.success ?: 0) == 1
			if (isSuccess)	return@forEach
			try {
				val existAvailable = safe4.existAvailableNeedToRedeem(address)
				val existLocked = safe4.existLockedNeedToRedeem(address)
				val existMasterNode = safe4.existMasterNodeNeedToRedeem(address)
				if (existAvailable || existLocked || existMasterNode) {
					val safe3Info = getAvailableSafe3Info(address)
					val (lockNum, safe3LockedInfo) = loadItems(address)
					val lockedAmount = lockBalance(safe3LockedInfo)
					val masterNodeAmount = masterLockBalance(safe3LockedInfo)
					if (safe3Info != null) {
						redeemableAmount += safe3Info.amount
					}
					redeemableLocked += lockedAmount
					if ((safe3Info?.amount == null || safe3Info.amount == BigInteger.ZERO) && lockedAmount == BigInteger.ZERO && masterNodeAmount == BigInteger.ZERO)	return@forEach
					bitcoinCore.getPrivateKey(it.publicKey)?.let { privateKey ->
						list.add(
							RedeemSafe3Module.Safe3LocalInfo(
								address,
								safe3Info?.amount ?: BigInteger.ZERO,
								lockedAmount,
								existAvailable,
								existLocked,
								existMasterNode,
								lockNum,
								masterNodeAmount,
								privateKey,
								true
							)
						)
					}
				} else {
					val value = unspentOutputs.filter { it.output.address == address }.sumOf { it.output.value }.toBigInteger()* BigInteger.TEN.pow(10)
					val availableAmount = if (isLock) BigInteger.ZERO else  value
					val lockedAmount = if (isLock) value else BigInteger.ZERO
					bitcoinCore.getPrivateKey(it.publicKey)?.let { privateKey ->
						list.add(
							RedeemSafe3Module.Safe3LocalInfo(
								address,
								availableAmount,
								lockedAmount,
								existAvailable,
								existLocked,
								existMasterNode,
								0,
								null,
								privateKey
							)
						)
					}
					redeemStorage.save(Redeem(
						address,
						existAvailable,
						existLocked,
						existMasterNode,
						true
					))
				}
				emitState()
			} catch (e: Exception) {
				Log.e("Redeem", "redeemCurrentWalletSafe3 error=$e")
			}
		}
	}

	private fun allUtxo(): List<UnspentOutput> {
		val spendableUtxo = bitcoinCore.dataProvider.getSpendableUtxo()
		val spendableTimeLockUtxo = bitcoinCore.dataProvider.getSpendableTimeLockUtxo()
		return spendableUtxo + spendableTimeLockUtxo
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