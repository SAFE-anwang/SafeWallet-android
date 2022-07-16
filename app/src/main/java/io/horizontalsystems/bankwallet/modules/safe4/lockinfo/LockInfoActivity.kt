package io.horizontalsystems.bankwallet.modules.safe4.lockinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.adapters.SafeAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator.getString
import io.horizontalsystems.bankwallet.databinding.ActivityLockInfoBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderLockItemBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import java.math.BigDecimal

class LockInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityLockInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(null)

        binding = ActivityLockInfoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top)

        val wallet: Wallet = intent.getParcelableExtra(WALLET) ?: run { finish(); return }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val balanceData =  balanceAdapterRepository.balanceData(wallet)
        Log.i("safe4", "---balanceData: ${balanceData.locked}")

        val adapter = App.adapterManager.getAdapterForWallet(wallet) as SafeAdapter
        val lockUxto = adapter.kit.getConfirmedUnlockedUnspentOutputProvider().getLockUxto()
        lockUxto.forEach {
            val lockMonth = (BigDecimal(it.output.unlockedHeight!!) - BigDecimal(it.block!!.height)) / BigDecimal(86400)
            Log.i("safe4", "---lockUxto: $lockMonth ${it.output.value} ${it.output.unlockedHeight}")
        }
        setToolbar()

        val totalAmount = balanceData.locked.movePointLeft(8).stripTrailingZeros()
        binding.totalAmountText.text = getString(R.string.Safe4_Lock_Total_Amount, totalAmount)
        syncItems(lockUxto)
    }

    private fun setToolbar() {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(R.string.Safe4_Lock_Info),
                    navigationIcon = {
                        Image(painter = painterResource(id = R.drawable.logo_safe_24),
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                finish()
                            }
                        )
                    )
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_to_bottom)
    }

    companion object {
        const val WALLET = "wallet_key"
    }

    private fun syncItems(items: List<UnspentOutput>) {
        val viewItems = items.map { item ->
            val lockMonth = (BigDecimal(item.output.unlockedHeight!!) - BigDecimal(item.block!!.height)) / BigDecimal(86400)
            val lockAmount = BigDecimal(item.output.value).movePointLeft(8).stripTrailingZeros()
            ViewItem(
                lockAmount.toPlainString(),
                item.output.address.toString(),
                lockMonth.toPlainString()
            )
        }
        binding.rvItems.adapter = Adapter(viewItems)
    }

    data class ViewItem(
        val lockAmount: String,
        val address: String,
        val month: String
    )

    class Adapter(
        private val items: List<ViewItem>
    ) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ViewHolderLockItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }
    }

    class ViewHolder(
        private val binding: ViewHolderLockItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var item: ViewItem? = null

        fun bind(item: ViewItem) {
            this.item = item
            binding.lockAmountText.text = getString(R.string.Safe4_Lock_Amount, item.lockAmount)
            binding.addressText.text = item.address
            binding.monthText.text = getString(R.string.Safe4_Lock_Month, item.month)
        }
    }

}
