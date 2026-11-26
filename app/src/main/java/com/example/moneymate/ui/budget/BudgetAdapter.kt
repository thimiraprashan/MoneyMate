package com.example.moneymate.ui.budget

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.Budget
import com.example.moneymate.data.BudgetHealth
import com.example.moneymate.data.BudgetHealthStatus
import com.example.moneymate.utils.CurrencyManager
import kotlin.math.abs

class BudgetAdapter(
    private var healthList: List<BudgetHealth> = emptyList(),
    private val onEditClick: (Budget) -> Unit,
    private val onDeleteClick: (Budget) -> Unit,
    private val currencyManager: CurrencyManager
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.categoryName)
        val periodText: TextView = view.findViewById(R.id.periodText)
        val budgetAmount: TextView = view.findViewById(R.id.budgetAmount)
        val spentAmount: TextView = view.findViewById(R.id.spentAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val remainingText: TextView = view.findViewById(R.id.remainingText)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val item = healthList[position]
        val budget = item.budget
        val ctx = holder.itemView.context

        holder.categoryName.text = budget.category.displayName
        holder.periodText.text = "${budget.effectivePeriod().name} • Alert at ${budget.warningThreshold}%"
        holder.spentAmount.text = "Spent: ${currencyManager.formatAmount(item.spentAmount)}"
        holder.budgetAmount.text = "Limit: ${currencyManager.formatAmount(budget.amount)}"
        holder.progressBar.progress = item.progressPercentage.coerceIn(0, 100)
        holder.progressText.text = "${item.progressPercentage}%"

        val statusColor = when (item.status) {
            BudgetHealthStatus.EXCEEDED -> ContextCompat.getColor(ctx, R.color.expense_red)
            BudgetHealthStatus.WARNING -> ContextCompat.getColor(ctx, R.color.warning_amber)
            BudgetHealthStatus.SAFE -> ContextCompat.getColor(ctx, R.color.primary)
        }
        holder.progressBar.progressTintList = ColorStateList.valueOf(statusColor)
        holder.progressText.setTextColor(statusColor)

        holder.remainingText.text = if (item.remainingAmount >= 0) {
            "${currencyManager.formatAmount(item.remainingAmount)} remaining"
        } else {
            "Over budget by ${currencyManager.formatAmount(abs(item.remainingAmount))}"
        }
        holder.remainingText.setTextColor(
            if (item.remainingAmount < 0) ContextCompat.getColor(ctx, R.color.expense_red)
            else ContextCompat.getColor(ctx, R.color.text_secondary)
        )

        holder.editButton.setOnClickListener { onEditClick(budget) }
        holder.deleteButton.setOnClickListener { onDeleteClick(budget) }
    }

    override fun getItemCount() = healthList.size

    fun updateBudgets(newHealthList: List<BudgetHealth>) {
        healthList = newHealthList
        notifyDataSetChanged()
    }
}
