package com.example.moneymate.ui.analysis

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.CategoryBreakdown
import com.example.moneymate.utils.CurrencyManager
import java.util.Locale

class CategoryAdapter(
    private val currencyManager: CurrencyManager
) : ListAdapter<CategoryBreakdown, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), currencyManager)
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorDot: View = itemView.findViewById(R.id.categoryColorDot)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val categoryMeta: TextView = itemView.findViewById(R.id.categoryMeta)
        private val categoryAmount: TextView = itemView.findViewById(R.id.categoryAmount)

        fun bind(item: CategoryBreakdown, currencyManager: CurrencyManager) {
            colorDot.backgroundTintList = ColorStateList.valueOf(item.category.colorHex)
            categoryName.text = item.category.displayName
            val txLabel = if (item.transactionCount == 1) "1 entry" else "${item.transactionCount} entries"
            categoryMeta.text = String.format(Locale.getDefault(), "%.1f%% • %s", item.percentage, txLabel)
            categoryAmount.text = currencyManager.formatAmount(item.amount)
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryBreakdown>() {
    override fun areItemsTheSame(oldItem: CategoryBreakdown, newItem: CategoryBreakdown): Boolean =
        oldItem.category == newItem.category

    override fun areContentsTheSame(oldItem: CategoryBreakdown, newItem: CategoryBreakdown): Boolean =
        oldItem == newItem
}
