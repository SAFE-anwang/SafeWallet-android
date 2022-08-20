package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import java.io.BufferedReader
import java.io.InputStreamReader

class SelectWalletViewModel: ViewModel() {

    val walletItemsLiveData = MutableLiveData<List<WalletInfo>>()
    val walletList: WalletListInfo? = initWalletList()

    var filterText: String = ""

    init {
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


    private fun update() {
        val list = mutableListOf<WalletInfo>()
        if (filterText.isBlank()) {
            walletList?.hardware?.let {
                list.addAll(it)
            }
            walletList?.software?.let {
                list.addAll(it)
            }
            walletList?.both?.let {
                list.addAll(it)
            }
        } else {
            walletList?.hardware?.filter {
                it.name.contains(filterText)
            }?.let { list.addAll(it) }
            walletList?.software?.filter {
                it.name.contains(filterText)
            }?.let { list.addAll(it) }
            walletList?.both?.filter {
                it.name.contains(filterText)
            }?.let { list.addAll(it) }
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