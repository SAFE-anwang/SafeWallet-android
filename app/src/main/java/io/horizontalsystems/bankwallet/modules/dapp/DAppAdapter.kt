package io.horizontalsystems.bankwallet.modules.dapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.databinding.ViewDappItemBinding
import java.util.HashMap


class DAppAdapter(
    val viewModel: DAppViewModel,
    private val listener: Listener
):  RecyclerView.Adapter<DAppAdapter.DAppViewHolder>() {

    var items = HashMap<String, List<DAppItem>>()

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DAppAdapter.DAppViewHolder {
        return DAppViewHolder(
            ViewDappItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            viewModel,
            listener
        )
    }

    override fun onBindViewHolder(holder: DAppAdapter.DAppViewHolder, position: Int) {
        items.keys.forEachIndexed { index, s ->
            if (index == position) {
                holder.bind(s, items[s]!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class DAppViewHolder(private val binding: ViewDappItemBinding,
                         private val viewModel: DAppViewModel,
                         private val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String, dApps: List<DAppItem>) {
            val subAdapter = DAppItemAdapter(dApps, listener)
            val layoutManager = LinearLayoutManager(binding.recyclerView.context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.setHasFixedSize(true)
//            binding.recyclerView.layoutManager = StaggeredGridLayoutManager(3, LinearLayoutManager.HORIZONTAL)
            binding.recyclerView.adapter = subAdapter
            binding.txtTitle.text = if (title == "Recommend") {
                binding.recyclerView.context.getString(R.string.DApp_Recommended)
            } else {
                title
            }
            binding.txtAll.setOnClickListener {
                viewModel.tempListApp = dApps
                listener.onAllDApp(title)
            }
        }
    }


    interface Listener {
        fun onAllDApp(type: String)
        fun onClick(dappItem: DAppItem)
    }


}
