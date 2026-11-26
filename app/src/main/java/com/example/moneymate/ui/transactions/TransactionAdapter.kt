package com.example.moneymate.ui.transactions

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionType
import com.example.moneymate.utils.CurrencyManager
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val currencyManager: CurrencyManager
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.transactionTitle)
        private val categoryTextView: TextView = itemView.findViewById(R.id.transactionCategory)
        private val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        private val amountTextView: TextView = itemView.findViewById(R.id.transactionAmount)
        private val typeIcon: ImageView = itemView.findViewById(R.id.transactionTypeIcon)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            titleTextView.text = transaction.description
            categoryTextView.text = transaction.category.displayName
            dateTextView.text = dateFormat.format(transaction.date)

            val formatted = currencyManager.formatAmount(transaction.amount)
            val ctx = itemView.context
            if (transaction.type == TransactionType.INCOME) {
                typeIcon.setImageResource(R.drawable.ic_arrow_down)
                typeIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.income_bg))
                typeIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.income_green))
                amountTextView.text = "+$formatted"
                amountTextView.setTextColor(ContextCompat.getColor(ctx, R.color.income_green))
            } else {
                typeIcon.setImageResource(R.drawable.ic_arrow_up)
                typeIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.expense_bg))
                typeIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.expense_red))
                amountTextView.text = "-$formatted"
                amountTextView.setTextColor(ContextCompat.getColor(ctx, R.color.expense_red))
            }

            editButton.setOnClickListener { onEditClick(transaction) }
            deleteButton.setOnClickListener { onDeleteClick(transaction) }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem == newItem
    }
}
