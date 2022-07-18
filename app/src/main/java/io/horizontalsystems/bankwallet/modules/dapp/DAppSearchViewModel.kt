package io.horizontalsystems.bankwallet.modules.dapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.dapp.service.DAppService
import io.reactivex.disposables.CompositeDisposable

class DAppSearchViewModel(
    private val service: DAppService
): ViewModel() {

    val searchResultList = MutableLiveData<List<DAppItem>>()
    val viewState = MutableLiveData<ViewState>(ViewState.Loading)
    val syncingLiveData = MutableLiveData<Boolean>()

    private val disposables = CompositeDisposable()

    init {
        service.searchItemsObservable
            .subscribeIO { dAppItemDataState ->
                dAppItemDataState.viewState?.let {
                    viewState.postValue(it)
                }

                dAppItemDataState.dataOrNull?.let {
                    searchResultList.postValue(it)
                }
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        service.clear()
    }

    fun search(name: String) {
        if (name.isNullOrBlank())   return
        service.search(name)
    }

    fun reload() {

    }
}