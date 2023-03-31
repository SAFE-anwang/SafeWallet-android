package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessorImpl
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    initial: Address? = null,
    tokenQuery: TokenQuery,
    coinCode: String,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onStateChange: ((DataState<Address>?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null
) {
    val viewModel = viewModel<AddressViewModel>(
            factory = AddressInputModule.FactoryToken(tokenQuery, coinCode),
            key = "address_view_model_${tokenQuery.id}"
    )

    HSAddressInput(
        modifier = modifier,
        viewModel = viewModel,
        initial = initial,
        error = error,
        textPreprocessor = textPreprocessor,
        onStateChange = onStateChange,
        onValueChange = onValueChange
    )
}

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel,
    initial: Address? = null,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    onStateChange: ((DataState<Address>?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null
) {

    val scope = rememberCoroutineScope()
    var addressState by remember { mutableStateOf<DataState<Address>?>(initial?.let { DataState.Success(it) }) }
    var parseAddressJob by remember { mutableStateOf<Job?>(null)}
    var isFocused by remember { mutableStateOf(false)}

    val addressStateMergedWithError = if (addressState is DataState.Success && error != null) {
        DataState.Error(error)
    } else {
        addressState
    }

    val inputState = when {
        isFocused -> getFocusedState(addressStateMergedWithError)
        else -> addressStateMergedWithError
    }

    if (initial != null) {
        Log.i("safe4",  "initial 1: ${addressState?.dataOrNull}")
        onValueChange?.invoke(addressState?.dataOrNull)
        onStateChange?.invoke(addressState)
    }

    FormsInput(
        modifier = modifier,
        initial = initial?.title,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        state = inputState,
        qrScannerEnabled = true,
        textPreprocessor = textPreprocessor,
        onChangeFocus = {
            isFocused = it
        }
    ) {
//        Log.i("safe4",  "it: $it")
        parseAddressJob?.cancel()
        parseAddressJob = scope.launch {
            addressState = DataState.Loading
//            Log.i("safe4",  "initial 2: ${addressState?.dataOrNull}")
            val state = try {
                DataState.Success(viewModel.parseAddress(it))
            } catch (e: AddressValidationException.Blank) {
                null
            } catch (e: AddressValidationException) {
                DataState.Error(e)
            }

            ensureActive()
            addressState = state
//            Log.i("safe4",  "initial 3: ${addressState?.dataOrNull}")
            onValueChange?.invoke(addressState?.dataOrNull)
            onStateChange?.invoke(addressState)
        }
    }
}

private fun getFocusedState(state: DataState<Address>?): DataState<Address>? {
    return if (state is DataState.Error && state.error !is AddressValidationException.Invalid) {
        null
    } else {
        state
    }
}
