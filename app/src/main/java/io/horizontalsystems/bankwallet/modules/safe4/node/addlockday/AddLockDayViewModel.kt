package io.horizontalsystems.bankwallet.modules.safe4.node.addlockday

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AddLockDayViewModel(
	val lockIds: List<Int>,
	private val safe4: RpcBlockchainSafe4,
	private val privateKey: String,
) : ViewModelUiState<SafeFourModule.AddLockDayUiState>() {

	var inputDay = 360
	var error: Int? = null
	private var showConfirmationDialog = false

	var sendResult by mutableStateOf<SendResult?>(null)

	private val disposables = CompositeDisposable()

	override fun createState(): SafeFourModule.AddLockDayUiState {
		return SafeFourModule.AddLockDayUiState(
				this.inputDay >= 360,
				error,
				showConfirmationDialog
		)
	}

	fun onEnterDay(day: Int) {
		try {
			this.inputDay = day
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
					safe4.addLockDay(privateKey, lockId, inputDay)
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