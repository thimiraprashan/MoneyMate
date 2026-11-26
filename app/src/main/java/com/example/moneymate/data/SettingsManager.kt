package com.example.moneymate.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.moneymate.utils.CurrencyManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class SettingsManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val tag = "SettingsManager"

    var currency: String
        get() = CurrencyManager.getInstance(context).getCurrentCurrency().code
        set(value) {
            sharedPreferences.edit().putString("currency", value).apply()
            Currency.fromCode(value)?.let {
                CurrencyManager.getInstance(context).setCurrency(it)
            }
        }

    var budgetWarningThreshold: Int
        get() = sharedPreferences.getInt("budget_warning_threshold", 80)
        set(value) = sharedPreferences.edit().putInt("budget_warning_threshold", value.coerceIn(1, 100)).apply()

    fun exportData(): String {
        val transactions = TransactionManager(context).getTransactions()
        val budgets = BudgetManager(context).getBudgets()
        val settings = mapOf(
            "currency" to currency,
            "budget_warning_threshold" to budgetWarningThreshold
        )
        val data = mapOf(
            "transactions" to transactions,
            "budgets" to budgets,
            "settings" to settings
        )
        return gson.toJson(data)
    }

    fun exportTransactionsCsv(): String {
        val transactions = TransactionManager(context).getTransactions()
        return FinanceAnalytics.exportTransactionsToCsv(transactions, currency)
    }

    fun importData(json: String): Boolean {
        return try {
            val data = gson.fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)
                ?: return false

            if (!data.containsKey("transactions") || !data.containsKey("budgets") || !data.containsKey("settings")) {
                Log.e(tag, "Invalid data format: missing required sections")
                return false
            }

            val transactions = gson.fromJson<List<Transaction>>(
                gson.toJson(data["transactions"]),
                object : TypeToken<List<Transaction>>() {}.type
            ) ?: emptyList()
            TransactionManager(context).saveTransactions(transactions)

            val budgets = gson.fromJson<List<Budget>>(
                gson.toJson(data["budgets"]),
                object : TypeToken<List<Budget>>() {}.type
            ) ?: emptyList()
            BudgetManager(context).saveBudgets(budgets)

            val settings = gson.fromJson<Map<String, Any>>(
                gson.toJson(data["settings"]),
                object : TypeToken<Map<String, Any>>() {}.type
            ) ?: emptyMap()

            settings["currency"]?.toString()?.let { currency = it }
            settings["budget_warning_threshold"]?.let { value ->
                val threshold = when (value) {
                    is Number -> value.toInt()
                    else -> value.toString().toDoubleOrNull()?.toInt() ?: 80
                }
                budgetWarningThreshold = threshold
            }
            true
        } catch (e: JsonSyntaxException) {
            Log.e(tag, "Invalid JSON format", e)
            false
        } catch (e: Exception) {
            Log.e(tag, "Error importing data", e)
            false
        }
    }

    fun loadSampleDemoData() {
        TransactionManager(context).saveTransactions(FinanceAnalytics.generateSampleTransactions())
        BudgetManager(context).saveBudgets(FinanceAnalytics.generateSampleBudgets())
    }

    fun clearAllFinancialData() {
        TransactionManager(context).clearAllTransactions()
        BudgetManager(context).clearAllBudgets()
    }
}
