package com.example.moneymate.data

import java.util.Date

data class Budget(
    val id: Long = 0,
    val category: TransactionCategory,
    val amount: Double,
    val startDate: Date,
    val endDate: Date,
    val warningThreshold: Int = 80,
    val period: BudgetPeriod? = BudgetPeriod.MONTHLY
) {
    fun effectivePeriod(): BudgetPeriod = period ?: BudgetPeriod.MONTHLY
}
