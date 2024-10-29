package io.horizontalsystems.bankwallet.modules.safe4.node.addlockday

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.DAILY_BLOCK_PRODUCTION_SPEED
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
	val lockIds: List<Int>,
	private val safe4: RpcBlockchainSafe4,
	private val privateKey: String,
) : ViewModelUiState<SafeFourModule.AddLockDayUiState>() {

	var maxLockDay = 3600
	var inputDay = 360
	var error: Int? = null
	var errorMaxDay: Int? = null
	private var showConfirmationDialog = false

	var sendResult by mutableStateOf<SendResult?>(null)

	private val disposables = CompositeDisposable()

	init {
		getRecordInfo()
	}

	override fun createState(): SafeFourModule.AddLockDayUiState {
		return SafeFourModule.AddLockDayUiState(
				maxLockDay,
				this.inputDay >= 360 && this.inputDay <= maxLockDay,
				error,
				errorMaxDay,
				showConfirmationDialog
		)
	}

	private fun getRecordInfo() {
		viewModelScope.launch(Dispatchers.IO) {
			var minLockDay = 3600
			lockIds.forEach { lockId ->
				try {
					val recordInfo = safe4.getRecordByID(lockId)
					val unlockHeight = recordInfo.unlockHeight.toLong() - (safe4.lastBlockHeight
							?: 0)
					// 剩余解锁时间
					val remainDay = unlockHeight / DAILY_BLOCK_PRODUCTION_SPEED
					val lockDay = ((3600 - remainDay.toInt()) / 360) * 360
					minLockDay = min(minLockDay, lockDay)
				} catch (e: Exception) {

				}
			}
			maxLockDay = minLockDay
			emitState()
		}
	}

	fun onEnterDay(day: Int) {
		try {
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
		sendResult = SendResult.Sending
		viewModelScope.launch(Dispatchers.IO) {
			try {
				lockIds.forEach { lockId ->
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
						return@forEach
					}
					safe4.addLockDay(privateKey, lockId, lockDay)
							.subscribe({
								sendResult = SendResult.Sent
								emitState()
							}, {
								sendResult = SendResult.Failed(NodeCovertFactory.createCaution(it))
								emitState()
							}).let {
								disposables.add(it)
							}

				}

			} catch (e: Exception) {
				sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
				emitState()
			}
		}
	}

}