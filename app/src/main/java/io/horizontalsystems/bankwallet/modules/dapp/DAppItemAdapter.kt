package io.horizontalsystems.bankwallet.modules.dapp

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ErrorResult
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
            binding.txtDescription.text = if (App.languageManager.currentLanguageName.contains("中文"))
                item.desc else item.descEN
            buildKeywordChips(binding, item.keywords)
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
                placeholder(R.drawable.ic_placeholder)
                transformations(CircleCropTransformation())
                listener(
                    object : ImageRequest.Listener {
                        override fun onError(request: ImageRequest, throwable: ErrorResult) {
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

private fun buildKeywordChips(binding: ViewDappSubItemBinding, keywords: String?) {
    val container = binding.keywordsContainer
    container.removeAllViews()
    if (keywords.isNullOrBlank()) return

    val context = binding.root.context
    val density = context.resources.displayMetrics.density
    val chipBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 10f * density
        setColor(ContextCompat.getColor(context, R.color.lawrence))
        setStroke(1, ContextCompat.getColor(context, R.color.leah))
    }

    keywords.split("|").forEach { keyword ->
        if (keyword.isBlank()) return@forEach
        val chip = TextView(context).apply {
            text = keyword.trim()
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.grey))
            background = chipBg
            gravity = Gravity.CENTER
            setPadding(10, 3, 10, 3)
        }
        container.addView(chip)
        // Add spacing between chips
        val spacer = android.view.View(context)
        spacer.layoutParams = android.view.ViewGroup.MarginLayoutParams(
            (6 * density).toInt(), 0
        )
        container.addView(spacer)
    }
    // Remove trailing spacer
    if (container.childCount > 0 && container.getChildAt(container.childCount - 1) is android.view.View
        && (container.getChildAt(container.childCount - 1) as? android.view.View)?.let { it !is TextView } == true
    ) {
        container.removeViewAt(container.childCount - 1)
    }
}
