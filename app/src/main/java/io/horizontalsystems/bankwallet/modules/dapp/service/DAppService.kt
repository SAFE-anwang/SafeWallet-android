package io.horizontalsystems.bankwallet.modules.dapp.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.dapp.DAppItem
import io.horizontalsystems.bankwallet.modules.dapp.FilterDAppType
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.io.BufferedReader
import java.io.InputStreamReader

class DAppService(
    val service: DAppApiService
): Clearable {

    val allDAppList: ArrayList<DAppItem> = ArrayList()
    val dAppItemsObservable: BehaviorSubject<DataState<List<DAppItem>>> =
        BehaviorSubject.create()
    val recommendsItemsObservable: BehaviorSubject<DataState<List<DAppItem>>> =
        BehaviorSubject.create()
    val searchItemsObservable: BehaviorSubject<DataState<List<DAppItem>>> =
        BehaviorSubject.create()

    private var dAppDataDisposable: Disposable? = null
    private var recommendsDisposable: Disposable? = null

    private var filterDAppType = FilterDAppType.ALL

    init {
        syncData()
    }

    private fun syncData() {
        dAppDataDisposable?.dispose()
        service.getAllList()
            .doOnSubscribe { dAppItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                allDAppList.clear()
                allDAppList.addAll(it)
                setFilterType(filterDAppType)
            }, {
                dAppItemsObservable.onNext(DataState.Success(getDefaultRecommends()))
            })
            .let {
                dAppDataDisposable = it
            }
        /*service.getRecommends()
            .subscribeIO({
                recommendsItemsObservable.onNext(DataState.Success(it))
            }, {
                // 使用默认数据
                recommendsItemsObservable.onNext(DataState.Success(getDefaultRecommends()))
//                recommendsItemsObservable.onNext(DataState.Error(it))
            })
            .let {
                recommendsDisposable = it
            }*/
    }

    fun getDefaultRecommends(): List<DAppItem> {
        try {
            val gson = Gson()
            val inputStream = App.instance.assets.open("dapp_default_list")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = bufferedReader.readText()
            val listType = object : TypeToken<List<DAppItem>>() {}.type
            return gson.fromJson<List<DAppItem>>(jsonString, listType)
        } catch (e: Exception) {
        }
        return listOf()
    }

    private fun getFilterString(): String {
        return when(filterDAppType) {
            FilterDAppType.ETH -> "ETH"
            FilterDAppType.BSC -> "BSC"
            FilterDAppType.SAFE -> "SAFE"
            FilterDAppType.ALL -> "ALL"
        }
    }

    fun setFilterType(f: FilterDAppType) {
        filterDAppType = f
        val typeString = getFilterString()
        dAppItemsObservable.onNext(DataState.Success(allDAppList.filter {
            if (typeString == "ALL")
                true
            else
                it.type == typeString
        }))
    }

    override fun clear() {
        dAppDataDisposable?.dispose()
    }

    fun search(name: String) {
        service.getListByName(name)
            .doOnSubscribe { searchItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                searchItemsObservable.onNext(DataState.Success(it))
            }, {
                searchItemsObservable.onNext(DataState.Error(it))
            })
            .let {

            }
    }

    fun refresh() {
        syncData()
    }
}