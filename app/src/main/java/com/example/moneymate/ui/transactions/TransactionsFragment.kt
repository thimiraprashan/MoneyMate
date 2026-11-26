package com.example.moneymate.ui.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymate.R
import com.example.moneymate.data.FinanceAnalytics
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionManager
import com.example.moneymate.data.TransactionSort
import com.example.moneymate.data.TransactionType
import com.example.moneymate.databinding.FragmentTransactionsBinding
import com.example.moneymate.utils.CurrencyManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionManager: TransactionManager
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var currencyManager: CurrencyManager

    private var currentQuery: String = ""
    private var currentTypeFilter: TransactionType? = null
    private var currentSort: TransactionSort = TransactionSort.NEWEST_FIRST

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionManager = TransactionManager(requireContext())
        currencyManager = CurrencyManager.getInstance(requireContext())
        setupRecyclerView()
        setupFilters()
        setupFab()
        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onEditClick = { transaction ->
                try {
                    findNavController().navigate(
                        R.id.action_transactionsFragment_to_addTransactionFragment,
                        Bundle().apply {
                            putLong("transactionId", transaction.id)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("TransactionsFragment", "Error navigating to edit transaction", e)
                }
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmation(transaction)
            },
            currencyManager = currencyManager
        )
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun setupFilters() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString().orEmpty()
                loadTransactions()
            }
        })

        binding.typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentTypeFilter = when {
                checkedIds.contains(R.id.chipIncome) -> TransactionType.INCOME
                checkedIds.contains(R.id.chipExpense) -> TransactionType.EXPENSE
                else -> null
            }
            loadTransactions()
        }

        binding.sortButton.setOnClickListener {
            currentSort = when (currentSort) {
                TransactionSort.NEWEST_FIRST -> TransactionSort.HIGHEST_AMOUNT
                TransactionSort.HIGHEST_AMOUNT -> TransactionSort.LOWEST_AMOUNT
                TransactionSort.LOWEST_AMOUNT -> TransactionSort.OLDEST_FIRST
                TransactionSort.OLDEST_FIRST -> TransactionSort.NEWEST_FIRST
            }
            binding.sortButton.text = when (currentSort) {
                TransactionSort.NEWEST_FIRST -> "Newest"
                TransactionSort.OLDEST_FIRST -> "Oldest"
                TransactionSort.HIGHEST_AMOUNT -> "Highest"
                TransactionSort.LOWEST_AMOUNT -> "Lowest"
            }
            loadTransactions()
        }
    }

    private fun loadTransactions() {
        val all = transactionManager.getTransactions()
        val filtered = FinanceAnalytics.filterTransactions(
            transactions = all,
            query = currentQuery,
            typeFilter = currentTypeFilter,
            sort = currentSort
        )
        transactionAdapter.submitList(filtered)

        val totalNet = filtered.sumOf {
            if (it.type == TransactionType.INCOME) it.amount else -it.amount
        }
        val countLabel = if (filtered.size == 1) "1 transaction" else "${filtered.size} transactions"
        binding.summaryStrip.text = "$countLabel • Net: ${currencyManager.formatAmount(totalNet)}"

        binding.emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.transactionsRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupFab() {
        binding.addTransactionButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_transactionsFragment_to_addTransactionFragment)
            } catch (e: Exception) {
                Log.e("TransactionsFragment", "Error navigating to add transaction", e)
            }
        }
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Delete \"${transaction.description}\" (${currencyManager.formatAmount(transaction.amount)})?")
            .setPositiveButton("Delete") { _, _ ->
                transactionManager.deleteTransaction(transaction)
                loadTransactions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
