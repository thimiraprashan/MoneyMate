package com.example.moneymate.ui.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.CategoryBreakdown
import com.example.moneymate.utils.CurrencyManager
import java.util.Locale

class CategoryAdapter(
    private val currencyManager: CurrencyManager
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<CategoryBreakdown>()

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorDot: View = view.findViewById(R.id.categoryColorDot)
        val categoryName: TextView = view.findViewById(R.id.categoryName)
        val categoryMeta: TextView = view.findViewById(R.id.categoryMeta)
        val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        holder.colorDot.backgroundTintList = ColorStateList.valueOf(item.category.colorHex)
        holder.categoryName.text = item.category.displayName
        val txLabel = if (item.transactionCount == 1) "1 transaction" else "${item.transactionCount} transactions"
        holder.categoryMeta.text = String.format(Locale.getDefault(), "%.1f%% • %s", item.percentage, txLabel)
        holder.categoryAmount.text = currencyManager.formatAmount(item.amount)
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryBreakdown>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }
}
