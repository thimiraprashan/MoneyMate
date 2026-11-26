package com.example.moneymate.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionRepository
import java.util.Calendar

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH)
    private val currentYear = calendar.get(Calendar.YEAR)

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        _transactions.value = repository.getTransactionsByMonth(currentMonth, currentYear)
            .sortedByDescending { it.date }
    }

    fun addTransaction(transaction: Transaction) {
        repository.addTransaction(transaction)
        loadTransactions()
    }

    fun updateTransaction(transaction: Transaction) {
        repository.updateTransaction(transaction)
        loadTransactions()
    }

    fun deleteTransaction(transactionId: Long) {
        repository.deleteTransaction(transactionId)
        loadTransactions()
    }

    fun refreshTransactions() {
        loadTransactions()
    }
} 
