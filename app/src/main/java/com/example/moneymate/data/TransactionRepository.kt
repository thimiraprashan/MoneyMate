package com.example.moneymate.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class TransactionRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "MoneyMatePrefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_BUDGET = "monthly_budget"
    }

    fun addTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun updateTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        val transactions = getAllTransactions().toMutableList()
        transactions.removeIf { it.id == transactionId }
        saveTransactions(transactions)
    }

    fun deleteAllTransactions() {
        saveTransactions(emptyList())
    }

    fun getAllTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getTransactionsByMonth(month: Int, year: Int): List<Transaction> {
        return getAllTransactions().filter {
            val cal = java.util.Calendar.getInstance()
            cal.time = it.date
            cal.get(java.util.Calendar.MONTH) == month && 
            cal.get(java.util.Calendar.YEAR) == year
        }
    }

    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_BUDGET, budget.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_BUDGET, 0f).toDouble()
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    fun getTotalExpenses(month: Int, year: Int): Double {
        return getTransactionsByMonth(month, year)
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    fun getTotalIncome(month: Int, year: Int): Double {
        return getTransactionsByMonth(month, year)
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    fun getCategoryExpenses(month: Int, year: Int): Map<TransactionCategory, Double> {
        return getTransactionsByMonth(month, year)
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }
} 
