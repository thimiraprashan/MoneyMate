package com.example.moneymate.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

data class BudgetProgress(
    val category: TransactionCategory,
    val budgetAmount: Double,
    val spentAmount: Double,
    val progressPercentage: Int
)

class BudgetManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val transactionManager = TransactionManager(context)

    fun saveBudget(budget: Budget) {
        val budgets = getBudgets().toMutableList()
        if (budget.id > 0) {
            val index = budgets.indexOfFirst { it.id == budget.id }
            if (index != -1) {
                budgets[index] = budget
            } else {
                budgets.add(budget)
            }
        } else {
            val newId = (budgets.maxOfOrNull { it.id } ?: 0) + 1
            budgets.add(budget.copy(id = newId))
        }
        saveBudgets(budgets)
    }

    fun getBudgets(): List<Budget> {
        val json = sharedPreferences.getString("budgets", "[]")
        val type = object : TypeToken<List<Budget>>() {}.type
        return gson.fromJson<List<Budget>>(json, type) ?: emptyList()
    }

    fun getBudgetById(id: Long): Budget? {
        return getBudgets().find { it.id == id }
    }

    fun getBudgetForCategory(category: TransactionCategory): Budget? {
        return getBudgets().find { it.category == category }
    }

    fun deleteBudget(budget: Budget) {
        val budgets = getBudgets().toMutableList()
        budgets.removeAll { it.id == budget.id }
        saveBudgets(budgets)
    }

    fun clearAllBudgets() {
        saveBudgets(emptyList())
    }

    fun getAllBudgetHealth(referenceDate: Date = Date()): List<BudgetHealth> {
        val transactions = transactionManager.getTransactions()
        return getBudgets().map { budget ->
            FinanceAnalytics.evaluateBudgetHealth(budget, transactions, referenceDate)
        }
    }

    fun evaluateAlertForCategory(category: TransactionCategory, referenceDate: Date = Date()): BudgetHealth? {
        val budget = getBudgetForCategory(category) ?: return null
        val health = FinanceAnalytics.evaluateBudgetHealth(
            budget,
            transactionManager.getTransactions(),
            referenceDate
        )
        return if (health.status != BudgetHealthStatus.SAFE) health else null
    }

    fun getBudgetProgress(category: TransactionCategory, startDate: Date, endDate: Date): BudgetProgress {
        val budget = getBudgetForCategory(category) ?: return BudgetProgress(category, 0.0, 0.0, 0)
        val spent = transactionManager.getTransactions()
            .filter { it.category == category && it.type == TransactionType.EXPENSE }
            .filter { it.date in startDate..endDate }
            .sumOf { it.amount }
        val progress = if (budget.amount > 0) ((spent / budget.amount) * 100).toInt() else 0
        return BudgetProgress(category, budget.amount, spent, progress)
    }

    fun saveBudgets(budgets: List<Budget>) {
        val json = gson.toJson(budgets)
        sharedPreferences.edit().putString("budgets", json).apply()
    }
}
