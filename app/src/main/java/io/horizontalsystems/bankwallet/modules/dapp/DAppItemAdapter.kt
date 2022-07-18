package io.horizontalsystems.bankwallet.modules.dapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.load
import coil.transform.CircleCropTransformation
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.ViewDappItemBinding
import io.horizontalsystems.bankwallet.databinding.ViewDappSubItemBinding
import java.util.HashMap


class DAppItemAdapter(
    var items: List<DAppItem>,
    val listener: DAppAdapter.Listener
):  RecyclerView.Adapter<DAppItemAdapter.DAppItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DAppItemAdapter.DAppItemViewHolder {
        return DAppItemViewHolder(
            ViewDappSubItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DAppItemAdapter.DAppItemViewHolder, position: Int) {
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

    fun updateData(data: List<DAppItem>) {
        items = data
        notifyDataSetChanged()
    }

    class DAppItemViewHolder(private val binding: ViewDappSubItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DAppItem) {
            binding.txtAppName.text = item.name
            binding.txtDescription.text = if (App.languageManager.currentLanguageName.contains("zh"))
                item.desc else item.descEN
            binding.appIconView.load(item.icon) {
                placeholder(R.drawable.ic_image_photo)
                transformations(CircleCropTransformation())
            }
        }
    }
}
