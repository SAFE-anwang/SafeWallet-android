package io.horizontalsystems.bankwallet.modules.dapp

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.ViewDappSubItemBinding


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
            Log.e("connectWallet", "languageName: ${App.languageManager.currentLanguageName}")
            binding.txtDescription.text = if (App.languageManager.currentLanguageName.contains("中文"))
                item.desc else item.descEN
            /*val request = ImageRequest.Builder(binding.appIconView.context)
                .data(item.icon)
                .crossfade(true)
                .target(binding.appIconView)
                .addHeader("Content-Type", "image/png")
                .listener(object : ImageRequest.Listener {
                    override fun onError(request: ImageRequest, throwable: Throwable) {
                        super.onError(request, throwable)
                        Log.e("DAppApiService", "error: $throwable")
                    }
                })
                .build()
            binding.appIconView.context.imageLoader.enqueue(request)*/
            binding.appIconView.load(item.icon) {
                placeholder(R.drawable.ic_image_photo)
                transformations(CircleCropTransformation())
                listener(
                    object : ImageRequest.Listener {
                        override fun onError(request: ImageRequest, throwable: Throwable) {
                            super.onError(request, throwable)
                            Log.e("DAppApiService", "error: $throwable")
                            val resId = when(item.name.lowercase()) {
                                "uniswap" -> {
                                    R.drawable.ic_uniswap
                                }
                                "sushi" -> {
                                    R.drawable.sushi
                                }
                                "safeswap" -> {
                                    R.drawable.safe
                                }
                                else -> 0
                            }
                            if (resId != 0) {
                                binding.appIconView.load(resId) {
                                    transformations(CircleCropTransformation())
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
