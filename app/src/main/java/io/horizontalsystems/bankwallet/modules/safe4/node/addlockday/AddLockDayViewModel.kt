package io.horizontalsystems.bankwallet.modules.safe4.node.addlockday

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.DAILY_BLOCK_PRODUCTION_SPEED
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.valueConvert
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class AddLockDayViewModel(
	val lockIds: List<Long>,
	private val safe4: RpcBlockchainSafe4,
	private val privateKey: String,
) : ViewModelUiState<SafeFourModule.AddLockDayUiState>() {

	private var maxLockDay = 3600
	private var inputDay = 0
	private var selectLockId = -1L
	var error: Int? = null
	var errorMaxDay: Int? = null
	private var showConfirmationDialog = false
	private var recordInfos: List<SafeFourModule.AddLockRecordInfo> = listOf()

	var sendResult by mutableStateOf<SendResult?>(null)

	private val disposables = CompositeDisposable()

	init {
		getRecordInfo()
	}

	override fun createState(): SafeFourModule.AddLockDayUiState {
		return SafeFourModule.AddLockDayUiState(
				maxLockDay,
				this.inputDay >= 360,
				recordInfos,
				error,
				errorMaxDay,
				showConfirmationDialog
		)
	}

	private fun getRecordInfo() {
		viewModelScope.launch(Dispatchers.IO) {
			val info = mutableListOf<SafeFourModule.AddLockRecordInfo>()
			lockIds.forEach { lockId ->
				try {
					val recordInfo = safe4.getRecordByID(lockId)
					val unlockHeight = recordInfo.unlockHeight.toLong() - (safe4.lastBlockHeight
							?: 0)
					// 剩余解锁时间
					val remainDay = unlockHeight / DAILY_BLOCK_PRODUCTION_SPEED
					val lockDay = ((3600 - remainDay.toInt()) / 360) * 360
					val lockedAmount = App.numberFormatter.formatCoinFull(valueConvert(recordInfo.amount), "SAFE", 2)
					info.add(SafeFourModule.AddLockRecordInfo(recordInfo.id.toLong(), remainDay.toInt(), lockDay, lockedAmount, lockDay > 0))
				} catch (e: Exception) {

				}
			}
			recordInfos = info
			emitState()
		}
	}

	fun onEnterDay(day: Int, lockId: Long) {
		try {
			this.selectLockId = lockId
			this.inputDay = day
			if (inputDay > maxLockDay) {
				errorMaxDay = R.string.Safe_Four_Node_Add_Lock_Day_Max
			} else {
				errorMaxDay = null
			}
			if (inputDay < 360) {
				error = R.string.Safe_Four_Node_Add_Lock_Day_Input_Hint
			} else {
				error = null
			}
			emitState()
		} catch (e: Exception) {
			error = R.string.Safe_Four_Node_Add_Lock_Day_Input_Error_Not_Number
		}
	}

	fun showConfirmation() {
		showConfirmationDialog = true
		emitState()
	}

	fun closeDialog() {
		showConfirmationDialog = false
		emitState()
	}

	fun addLockDay() {
		closeDialog()
		if (selectLockId == -1L) return
		sendResult = SendResult.Sending
		viewModelScope.launch(Dispatchers.IO) {
			try {
//				lockIds.forEach { lockId ->
					/*val recordInfo = safe4.getRecordByID(lockId)
					val unlockHeight = recordInfo.unlockHeight.toLong() - (safe4.lastBlockHeight ?: 0)
					// 剩余解锁时间
					val remainDay = unlockHeight / DAILY_BLOCK_PRODUCTION_SPEED
					val lockDay = if (remainDay + inputDay > 3600) {
						((3600 - remainDay.toInt()) / 360) * 360
					} else {
						inputDay
					}*/
					val lockDay = inputDay
					if (lockDay <= 0) {
						sendResult = SendResult.Sent
						emitState()
						return@launch
					}
					safe4.addLockDay(privateKey, selectLockId, lockDay)
							.subscribe({
								sendResult = SendResult.Sent
								emitState()
							}, {
								sendResult = SendResult.Failed(NodeCovertFactory.createCaution(it))
								emitState()
							}).let {
								disposables.add(it)
							}

			} catch (e: Exception) {
				sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
				emitState()
			}
		}
	}

}