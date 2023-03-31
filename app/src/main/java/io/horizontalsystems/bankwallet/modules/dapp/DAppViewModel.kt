package io.horizontalsystems.bankwallet.modules.dapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.dapp.service.DAppService
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DAppViewModel(
    private val service: DAppService
): ViewModel() {

    val dAppList = MutableLiveData<Map<String, List<DAppItem>>>()
    val recommendsAppList = MutableLiveData<Map<String, List<DAppItem>>>()
    val viewState = MutableLiveData<ViewState>(ViewState.Loading)
    val filterTypesLiveData = MutableLiveData<List<Filter<FilterDAppType>>>()
    val syncingLiveData = MutableLiveData<Boolean>()



    private val disposables = CompositeDisposable()

    lateinit var tempListApp: List<DAppItem>

    init {
        groupDAppData(service.getDefaultRecommends())
        service.dAppItemsObservable
            .subscribeIO { dAppItemDataState ->
                dAppItemDataState.viewState?.let {
                    viewState.postValue(it)
                }

                dAppItemDataState.dataOrNull?.let {
                    groupDAppData(it)
                }
            }
            .let {
                disposables.add(it)
            }
        /*service.recommendsItemsObservable
            .subscribeIO { dAppItemDataState ->
                dAppItemDataState.viewState?.let {
                    viewState.postValue(it)
                }

                dAppItemDataState.dataOrNull?.let {
                    val map = HashMap<String, List<DAppItem>>()
                    map["Recommend"] = it
                    recommendsAppList.postValue(map)
                }
            }
            .let {
                disposables.add(it)
            }*/
        filterTypesLiveData.postValue(listOf(
            Filter(FilterDAppType.ALL, true),
            Filter(FilterDAppType.ETH, false),
            Filter(FilterDAppType.BSC, false),
            Filter(FilterDAppType.SAFE, false),
        ))
    }

    private fun groupDAppData(datas: List<DAppItem>) {
        datas.forEach {
            it.iconPlaceholder = when(it.name.lowercase()) {
                "uniswap" -> {
                    R.drawable.ic_uniswap
                }
                "sushi" -> {
                    R.drawable.sushi
                }
                "safeswap" -> {
                    R.drawable.safe
                }
                else -> null
            }
        }
        val items = datas.groupBy {
            it.subType
        }
        dAppList.postValue(items)
    }

    fun setFilterDAppType(filterType: FilterDAppType) {
        val filterTypes = filterTypesLiveData.value?.map {
            Filter(it.item, it.item == filterType)
        }
        filterTypes?.let {
            filterTypesLiveData.postValue(it)
        }

        service.setFilterType(filterType)
    }

    override fun onCleared() {
        service.clear()
    }

    fun getListForType(subType: String) {
        return
    }

    fun onErrorClick() {
        refresh()
    }

    fun refresh() {
        service.refresh()

        viewModelScope.launch {
            syncingLiveData.postValue(true)
            delay(1000)
            syncingLiveData.postValue(false)
        }
    }
}

data class DAppItem(
    val type: String,
    val subType: String,
    val name: String,
    val desc: String?,
    val descEN: String?,
    val icon: String,
    val dlink: String,
    val md5Code: String?,
    var iconPlaceholder: Int? = null
) {

}

enum class FilterDAppType {
    ALL, ETH, BSC, SAFE;

    val title: Int
        get() = when (this) {
            ETH -> R.string.DApp_ETH
            BSC -> R.string.DApp_EOS
            SAFE -> R.string.DApp_SAFE
            ALL -> R.string.DApp_ALL
        }

    val type: String
        get() = when (this) {
            ETH -> "ETH"
            BSC -> "EOS"
            SAFE -> "SAFE"
            else -> "ALL"
        }
}

data class Filter<T>(val item: T, val selected: Boolean)