package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import java.io.BufferedReader
import java.io.InputStreamReader

class SelectWalletViewModel: ViewModel() {

    val walletItemsLiveData = MutableLiveData<List<WalletInfo>>()
    val filterTypesLiveData = MutableLiveData<List<Filter<FilterWalletType>>>()
    val walletList: WalletListInfo? = initWalletList()

    var filterText: String = ""
    var filterType: FilterWalletType = FilterWalletType.Hardware

    init {
        setFilterList()
        update()
    }

    private fun initWalletList(): WalletListInfo? {
        try {
            val gson = Gson()
            val inputStream = App.instance.assets.open("wallet.json")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = bufferedReader.readText()
            val listType = object : TypeToken<WalletListInfo>() {}.type
            return gson.fromJson<WalletListInfo>(jsonString, listType)
        } catch (e: Exception) {
        }
        return null
    }

    private fun setFilterList() {
        val filterTypes = FilterWalletType.values().toList().map {
            Filter(it, it == filterType)
        }
        filterTypesLiveData.postValue(filterTypes)
    }

    private fun update() {
        val list = mutableListOf<WalletInfo>()
        if (filterText.isBlank()) {
            when(filterType) {
                FilterWalletType.Hardware -> walletList?.hardware?.let {
                    list.addAll(it)
                }
                FilterWalletType.Software -> walletList?.software?.let {
                    list.addAll(it)
                }
                FilterWalletType.Both -> walletList?.both?.let {
                    list.addAll(it)
                }
            }
        } else {
            when(filterType) {
                FilterWalletType.Hardware -> walletList?.hardware?.filter {
                        it.name.contains(filterText)
                    }?.let { list.addAll(it) }

                FilterWalletType.Software -> walletList?.software?.filter {
                        it.name.contains(filterText)
                    }?.let { list.addAll(it) }

                FilterWalletType.Both -> walletList?.both?.filter {
                    it.name.contains(filterText)
                }?.let { list.addAll(it) }
            }
        }
        walletItemsLiveData.postValue(list)
    }

    fun updateFilter(name: String) {
        filterText = name
        update()
    }

    fun getWalletForName(name: String): WalletInfo? {
        walletList?.hardware?.forEach {
            if (it.name.contentEquals(name)) {
                return it
            }
        }
        walletList?.software?.forEach {
            if (it.name.contentEquals(name)) {
                return it
            }
        }
        walletList?.both?.forEach {
            if (it.name.contentEquals(name)) {
                return it
            }
        }
        return null
    }

    fun setFilterWalletType(filterType: FilterWalletType) {
        this.filterType = filterType
        setFilterList()
        update()
    }
}

data class WalletListInfo(
    val hardware: List<WalletInfo>,
    val software: List<WalletInfo>,
    val both: List<WalletInfo>,
)

data class WalletInfo(
    val name: String,
    val wallet: Int,
    val bip32path: List<String>,
    val custompath: Boolean,
    val needpassword: Int
)

enum class FilterWalletType {
    Hardware, Software, Both;

    val title: Int
        get() = when (this) {
            Hardware -> R.string.Restore_Import_Wallet_Type_Hardware
            Software -> R.string.Restore_Import_Wallet_Type_Software
            Both -> R.string.Restore_Import_Wallet_Type_Both
        }
}