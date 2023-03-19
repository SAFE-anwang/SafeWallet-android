package io.horizontalsystems.bankwallet.modules.dapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ViewDappHistoryItemBinding


class DAppHistoryAdapter(
    var items: List<String>,
    val listener: HistoryClickListener
):  RecyclerView.Adapter<DAppHistoryAdapter.DAppHistoryItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DAppHistoryAdapter.DAppHistoryItemViewHolder {
        return DAppHistoryItemViewHolder(
            ViewDappHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DAppHistoryItemViewHolder, position: Int) {
        holder.bind(items.get(position))
        holder.itemView.setOnClickListener {
            listener?.onClick(items[position])
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun updateData(data: List<String>) {
        items = data
        notifyDataSetChanged()
    }

    class DAppHistoryItemViewHolder(private val binding: ViewDappHistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.txtAppName.text = item
        }
    }

    interface HistoryClickListener {
        fun onClick(url: String)
    }
}
