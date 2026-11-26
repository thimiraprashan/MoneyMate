package com.example.moneymate.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date

class TransactionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("transactions", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTransaction(transaction: Transaction) {
        require(transaction.amount > 0) { "Transaction amount must be greater than 0." }
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
        require(!transaction.date.after(endOfToday)) { "Transaction date cannot be in the future." }

        val transactions = getTransactions().toMutableList()
        if (transaction.id > 0) {
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                transactions[index] = transaction
            } else {
                transactions.add(transaction)
            }
        } else {
            val newId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
            transactions.add(transaction.copy(id = newId))
        }
        saveTransactions(transactions)
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("transactions", "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson<List<Transaction>>(json, type) ?: emptyList()
    }

    fun getTransactionById(id: Long): Transaction? {
        return getTransactions().find { it.id == id }
    }

    fun deleteTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.removeAll { it.id == transaction.id }
        saveTransactions(transactions)
    }

    fun clearAllTransactions() {
        saveTransactions(emptyList())
    }

    fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double {
        return getTransactions()
            .filter { it.type == type && it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun getTotalAmountByCategoryAndDateRange(category: TransactionCategory, startDate: Date, endDate: Date): Double {
        return getTransactions()
            .filter { it.category == category && it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()
    }
}
