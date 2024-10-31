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
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.storage.RedeemStorage
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.createCaution
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinAddressService
import io.horizontalsystems.bankwallet.net.SafeNetWork
import io.horizontalsystems.bankwallet.net.VpnConnectService
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

class GetSafe3TestCoinViewModel(
		val wallet: Wallet?,
		val defaultAddress: String? = null
): ViewModelUiState<RedeemSafe3Module.GetSafe3TestCoinUiState>() {

	var getSafe3TestCoinService: Safe3TestCoinService? = null
	var sendResult by mutableStateOf<SendResult?>(null)

	var canEnable = false
	var getStatus = false
	var getResponse: GetResult? = null
	val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	private val disposables = CompositeDisposable()

	init {
		canEnable = defaultAddress?.isNotEmpty() ?: false
		emitState()
	}

	private fun initService() {
		if (getSafe3TestCoinService == null) {
			getSafe3TestCoinService = Safe3TestCoinService()
		}
	}

	override fun createState() : RedeemSafe3Module.GetSafe3TestCoinUiState{
		return RedeemSafe3Module.GetSafe3TestCoinUiState(
				canEnable,
				defaultAddress ?: "",
				getStatus,
				getResponse?.message,
				getResponse?.data?.amount,
				getResponse?.data?.transactionHash,
				getResponse?.data?.dateTimestamp?.let {
					dataFormat.format(Date(it.toLong()))
				},
				getResponse?.data?.from,
				getResponse?.data?.nonce,
		)
	}

	fun enterAddress(address: String) {
		canEnable = address.isNotEmpty()
		getResponse = null
		emitState()
	}

	fun getTestCoin(address: String) {
		initService()
		if (getSafe3TestCoinService == null) {
			sendResult = SendResult.Failed(createCaution(Throwable("领取失败")))
			return
		}
		sendResult = SendResult.Sending
		try {
			getSafe3TestCoinService?.getTestCoin(address)
					?.subscribeOn(Schedulers.io())
					?.subscribe({
						getResponse = it
						getStatus = it.status
						if (getStatus) {
							sendResult = SendResult.Sent
							canEnable = false
						} else {
							sendResult = SendResult.Failed(createCaution(Throwable(it.message)))
						}
						emitState()
					},
							{
								sendResult = SendResult.Failed(createCaution(it))
							}).let {
					}
		} catch (e: Exception) {
			Log.e("GetTestCoin", "error=$e")
			sendResult = SendResult.Failed(createCaution(e))
		}
	}

	override fun onCleared() {
		super.onCleared()
		disposables.clear()

		sendResult = SendResult.Sent
	}

	data class GetResult(
			val status: Boolean,
			val message: String? = null,
			val data: Data? = null
	)

	data class Data(
			val amount: String,
			val transactionHash: String,
			val dateTimestamp: String,
			val from: String,
			val nonce: String,
	)
}