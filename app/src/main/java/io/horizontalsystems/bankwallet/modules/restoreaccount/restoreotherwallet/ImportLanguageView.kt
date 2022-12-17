package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreotherwallet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ItemSelectLanguageBinding
import io.horizontalsystems.bankwallet.databinding.ViewImportLanguageBinding

class ImportLanguageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewImportLanguageBinding.inflate(LayoutInflater.from(context), this)

    private var onSelectLanguageCallback: ((language: LanguageType) -> Unit)? = null

    init {
        val adapter = SelectLanguageAdapter(getLanguageList()) {
            onSelectLanguageCallback?.invoke(it)
        }
        val gridLayoutManager = GridLayoutManager(context,2)
        binding.rvLanguage.layoutManager = gridLayoutManager
        binding.rvLanguage.adapter = adapter
    }

    fun onLanguageChange(callback: ((language: LanguageType) -> Unit)?) {
        onSelectLanguageCallback = callback
    }

    private fun getLanguageList(): List<LanguageType> {
        return mutableListOf(
            LanguageType.English,
            LanguageType.Chinese/*,
            LanguageType.TraditionalChinese,
            LanguageType.Japan,
            LanguageType.Spanish,
            LanguageType.Korean,
            LanguageType.French,
            LanguageType.Italian*/
        )
    }


    class SelectLanguageAdapter(
        val items: List<LanguageType>,
        val clickCallback: (LanguageType) -> Unit
    ):  RecyclerView.Adapter<LanguageViewHolder>() {

        private var currentLanguage: LanguageType = LanguageType.English

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            return LanguageViewHolder(
                ItemSelectLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val lanuage = items[position]
            holder.bind(lanuage, currentLanguage == lanuage)
            holder.itemView.setOnClickListener {
                currentLanguage = lanuage
                clickCallback.invoke(items[position])
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }


    }

    class LanguageViewHolder(private val binding: ItemSelectLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageType, isSelect: Boolean) {
            binding.ivName.setText(item.showName)
            binding.ivSelect.isSelected = isSelect
        }
    }
}