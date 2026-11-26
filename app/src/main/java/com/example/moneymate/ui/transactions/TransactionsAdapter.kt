package com.example.moneymate.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionType
import com.example.moneymate.databinding.ItemTransactionBinding
import com.example.moneymate.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val currencyManager: CurrencyManager
) : ListAdapter<Transaction, TransactionsAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.transactionTitle.text = transaction.type.toString()
            binding.transactionAmount.text = currencyManager.formatAmount(transaction.amount)
            binding.transactionDate.text = dateFormat.format(transaction.date)
            binding.transactionCategory.text = transaction.category.name
            binding.root.setOnClickListener { onTransactionClick(transaction) }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 
