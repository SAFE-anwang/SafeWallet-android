package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ItemSelectWalletTypeBinding


class SelectWalletTypeAdapter(
    val items: List<WalletType>,
    val clickCallback: (WalletType) -> Unit
):  RecyclerView.Adapter<SelectWalletTypeAdapter.WalletViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectWalletTypeAdapter.WalletViewHolder {
        return WalletViewHolder(
            ItemSelectWalletTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SelectWalletTypeAdapter.WalletViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            clickCallback.invoke(items[position])
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class WalletViewHolder(private val binding: ItemSelectWalletTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WalletType) {
            binding.ivName.setText(item.name)
            binding.ivWallet.setImageResource(item.icon)
        }
    }
}
