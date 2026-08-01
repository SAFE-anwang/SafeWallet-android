package io.horizontalsystems.bankwallet.modules.dapp.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.dapp.DAppItem
import io.horizontalsystems.bankwallet.modules.dapp.FilterDAppType
import io.horizontalsystems.bankwallet.modules.safe4.dapp.Safe4DAppService
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.io.BufferedReader
import java.io.InputStreamReader

class DAppService(
    val service: DAppApiService,
    private val safe4DAppService: Safe4DAppService? = null
): Clearable {

    companion object {
        private const val TAG = "DAppService"
    }

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
                mergeChainDApps()
                setFilterType(filterDAppType)
            }, {
                // Fallback to default data, then try chain DApps
                allDAppList.clear()
                allDAppList.addAll(getDefaultRecommends())
                mergeChainDApps()
                dAppItemsObservable.onNext(DataState.Success(allDAppList))
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

    private fun mergeChainDApps() {
        val safe4Service = safe4DAppService ?: return
        try {
            val chainDApps = safe4Service.fetchAllChainDApps()
            // Use name+url as dedup key to avoid duplicates with API data
            val existingKeys = allDAppList.map { "${it.name}|${it.dlink}" }.toSet()
            val chainItems = chainDApps.mapNotNull { info ->
                val key = "${info.name}|${info.runUrl}"
                if (key in existingKeys) return@mapNotNull null
                DAppItem(
                    type = "SAFE",
                    subType = "SAFE DApp",
                    name = info.name ?: "",
                    desc = info.description ?: "",
                    descEN = info.description ?: "",
                    icon = info.gitUrl ?: "",
                    dlink = info.runUrl ?: "",
                    md5Code = null,
                    keywords = info.keyword,
                    chainId = info.id.toString()
                )
            }
            if (chainItems.isNotEmpty()) {
                allDAppList.addAll(chainItems)
                Log.d(TAG, "Merged ${chainItems.size} chain DApps into list")
                cacheChainDAppLogos(chainItems)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch chain DApps", e)
        }
    }

    /**
     * Background fetch and cache logos for Safe4 chain DApps from contract.
     * After caching completes, republishes the list so UI picks up cached paths.
     */
    private fun cacheChainDAppLogos(chainItems: List<DAppItem>) {
        val safe4Service = safe4DAppService ?: return
        Thread {
            chainItems.forEach { item ->
                item.chainId?.let { id ->
                    try {
                        safe4Service.fetchAndCacheLogo(id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cache logo for DApp $id", e)
                    }
                }
            }
            // Republish list so UI picks up cached logo paths
            setFilterType(filterDAppType)
        }.start()
    }

    /**
     * Get the cached logo file path for a Safe4 chain DApp.
     */
    fun getChainDAppLogoPath(chainId: String): String? {
        return safe4DAppService?.getCachedLogoPath(chainId)
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