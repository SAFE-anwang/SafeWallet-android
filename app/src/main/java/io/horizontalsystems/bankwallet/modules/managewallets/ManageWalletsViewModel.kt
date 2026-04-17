package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.core.supportedCoinManager
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.tokenselect.SelectChainTab
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.marketkit.SafeExtend.isSafeFourCustomCoin
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModelUiState<ManageWalletsViewModel.ManageWalletsUiState>() {

    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()

    private var coinItems: List<CoinViewItem<Token>> = listOf()
    private var searchQuery = ""
    private val allTab = SelectChainTab(title = Translator.getString(R.string.Market_All), null)
    private var selectedChainTab: SelectChainTab = allTab
    private var availableBlockchainTypes: List<BlockchainType>? = BlockchainType.supportedCoinManager

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    init {
        viewModelScope.launch {
            service.itemsFlow.collect {
                sync(it)
            }
        }
        updateFilterBlockchains(service.selectedBlockchain)
    }

    override fun createState() = ManageWalletsUiState(
        items = coinItems,
        searchQuery = searchQuery,
        selectedTab = selectedChainTab,
        tabs = getTabs()
    )

    fun onTabSelected(tab: SelectChainTab) {
        selectedChainTab = tab
        sync(service.items)
    }

    private fun getTabs(): List<SelectChainTab> {
        val currentAvailableBlockchainTypes = availableBlockchainTypes
        if (currentAvailableBlockchainTypes.isNullOrEmpty() || currentAvailableBlockchainTypes.size == 1) {
            return emptyList()
        }

        return listOf(allTab) + currentAvailableBlockchainTypes.map { blockchainType ->
            SelectChainTab(
                title = blockchainType.title,
                blockchainType = blockchainType
            )
        }
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        // TODO:: DOGE 过滤
        coinItems = items
            .filter { it.token.blockchainType == selectedChainTab.blockchainType || selectedChainTab.blockchainType == null }
//            .filter { !(it.title == "DOGE" && (it.label == "BIP49" || it.label == "BIP84" || it.label == "BIP86")) }
            .map { viewItem(it) }
        emitState()
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.token,
        imageSource = ImageSource.Remote(
            item.token.coin.imageUrl,
            item.token.iconPlaceholder,
            item.token.coin.alternativeImageUrl
        ),
        title = item.token.coin.code,
        subtitle = item.token.coin.name,
        enabled = item.enabled,
        hasInfo = item.hasInfo,
        label = item.token.badge,
        isSafe4Deploy = item.token.tokenQuery.customCoinUid.isSafeFourCustomCoin()
    )

    fun enable(token: Token) {
        service.enable(token)
    }

    fun disable(token: Token) {
        service.disable(token)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun onEnterFilterBlockchain(filterBlockchain: Filter<Blockchain?>) {
        service.setFilter(filterBlockchain.item)

        updateFilterBlockchains(filterBlockchain.item)
    }

    private fun updateFilterBlockchains(select: Blockchain?) {
        val filterBlockchains = service.blockchains.map {
            Filter(it, it == select)
        }
        filterBlockchainsLiveData.postValue(filterBlockchains)
    }


    data class ManageWalletsUiState(
        val items: List<CoinViewItem<Token>>,
        val searchQuery: String,
        val selectedTab: SelectChainTab,
        val tabs: List<SelectChainTab>,
    )

    data class BirthdayHeightViewItem(
        val blockchainIcon: ImageSource,
        val blockchainName: String,
        val birthdayHeight: String
    )
}
