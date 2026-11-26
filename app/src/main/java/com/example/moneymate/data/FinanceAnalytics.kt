package com.example.moneymate.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class TransactionSort {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

enum class BudgetHealthStatus {
    SAFE,
    WARNING,
    EXCEEDED
}

data class CategoryBreakdown(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val balance: Double,
    val savingsRatePercent: Int,
    val transactionCount: Int,
    val expenseBreakdown: List<CategoryBreakdown>,
    val incomeBreakdown: List<CategoryBreakdown>
)

data class MonthlyCashFlow(
    val label: String,
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

data class BudgetHealth(
    val budget: Budget,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressPercentage: Int,
    val status: BudgetHealthStatus
)

data class FinancialInsights(
    val topExpenseCategory: CategoryBreakdown?,
    val averageDailyExpense: Double,
    val expenseChangePercent: Int?,
    val savingsRatePercent: Int
)

/**
 * Pure Kotlin financial calculation, filtering, CSV export, and sample-data engine.
 * Designed to be completely unit-testable without an Android Context.
 */
object FinanceAnalytics {

    fun monthBounds(year: Int, month: Int): Pair<Date, Date> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.time to endCal.time
    }

    fun periodBounds(period: BudgetPeriod, referenceDate: Date = Date()): Pair<Date, Date> {
        val cal = Calendar.getInstance().apply { time = referenceDate }
        val start = Calendar.getInstance().apply { time = referenceDate }
        val end = Calendar.getInstance().apply { time = referenceDate }

        when (period) {
            BudgetPeriod.DAILY -> {
                setStartOfDay(start)
                setEndOfDay(end)
            }
            BudgetPeriod.WEEKLY -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                setStartOfDay(start)
                end.time = start.time
                end.add(Calendar.DAY_OF_YEAR, 6)
                setEndOfDay(end)
            }
            BudgetPeriod.MONTHLY -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay(start)
                end.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                setEndOfDay(end)
            }
            BudgetPeriod.YEARLY -> {
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay(start)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
                setEndOfDay(end)
            }
        }
        return start.time to end.time
    }

    private fun setStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun setEndOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
    }

    fun computeMonthlySummary(
        transactions: List<Transaction>,
        year: Int,
        month: Int
    ): MonthlySummary {
        val (start, end) = monthBounds(year, month)
        val monthTx = transactions.filter { it.date in start..end }

        val totalIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0) {
            (((totalIncome - totalExpenses) / totalIncome) * 100.0).roundToInt().coerceIn(-100, 100)
        } else {
            0
        }

        return MonthlySummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            balance = balance,
            savingsRatePercent = savingsRate,
            transactionCount = monthTx.size,
            expenseBreakdown = computeBreakdown(monthTx.filter { it.type == TransactionType.EXPENSE }, totalExpenses),
            incomeBreakdown = computeBreakdown(monthTx.filter { it.type == TransactionType.INCOME }, totalIncome)
        )
    }

    private fun computeBreakdown(
        transactions: List<Transaction>,
        total: Double
    ): List<CategoryBreakdown> {
        if (transactions.isEmpty() || total <= 0.0) return emptyList()
        return transactions
            .groupBy { it.category }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                CategoryBreakdown(
                    category = category,
                    amount = sum,
                    percentage = ((sum / total) * 100.0).toFloat(),
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.amount }
    }

    fun computeRecentCashFlow(
        transactions: List<Transaction>,
        monthsCount: Int = 6,
        referenceDate: Date = Date()
    ): List<MonthlyCashFlow> {
        val labelFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val result = mutableListOf<MonthlyCashFlow>()

        for (offset in (monthsCount - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                time = referenceDate
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -offset)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val (start, end) = monthBounds(year, month)
            val monthTx = transactions.filter { it.date in start..end }
            val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            result.add(
                MonthlyCashFlow(
                    label = labelFormat.format(cal.time),
                    month = month,
                    year = year,
                    income = income,
                    expense = expense
                )
            )
        }
        return result
    }

    fun computeInsights(
        transactions: List<Transaction>,
        year: Int,
        month: Int,
        referenceNow: Date = Date()
    ): FinancialInsights {
        val currentSummary = computeMonthlySummary(transactions, year, month)

        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val prevSummary = computeMonthlySummary(
            transactions,
            prevCal.get(Calendar.YEAR),
            prevCal.get(Calendar.MONTH)
        )

        val nowCal = Calendar.getInstance().apply { time = referenceNow }
        val isCurrentMonth = nowCal.get(Calendar.YEAR) == year && nowCal.get(Calendar.MONTH) == month
        val daysElapsed = if (isCurrentMonth) {
            nowCal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        val avgDailyExpense = currentSummary.totalExpenses / daysElapsed

        val deltaPercent = if (prevSummary.totalExpenses > 0.0) {
            (((currentSummary.totalExpenses - prevSummary.totalExpenses) / prevSummary.totalExpenses) * 100.0).roundToInt()
        } else {
            null
        }

        return FinancialInsights(
            topExpenseCategory = currentSummary.expenseBreakdown.firstOrNull(),
            averageDailyExpense = avgDailyExpense,
            expenseChangePercent = deltaPercent,
            savingsRatePercent = currentSummary.savingsRatePercent
        )
    }

    fun filterTransactions(
        transactions: List<Transaction>,
        query: String = "",
        typeFilter: TransactionType? = null,
        categoryFilter: TransactionCategory? = null,
        sort: TransactionSort = TransactionSort.NEWEST_FIRST
    ): List<Transaction> {
        val trimmed = query.trim().lowercase(Locale.getDefault())
        val filtered = transactions.filter { tx ->
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = categoryFilter == null || tx.category == categoryFilter
            val matchesQuery = trimmed.isEmpty() ||
                tx.description.lowercase(Locale.getDefault()).contains(trimmed) ||
                tx.category.displayName.lowercase(Locale.getDefault()).contains(trimmed) ||
                tx.category.name.lowercase(Locale.getDefault()).contains(trimmed)
            matchesType && matchesCategory && matchesQuery
        }

        return when (sort) {
            TransactionSort.NEWEST_FIRST -> filtered.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.id })
            TransactionSort.OLDEST_FIRST -> filtered.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id })
            TransactionSort.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            TransactionSort.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
    }

    fun evaluateBudgetHealth(
        budget: Budget,
        transactions: List<Transaction>,
        referenceDate: Date = Date()
    ): BudgetHealth {
        val (start, end) = periodBounds(budget.effectivePeriod(), referenceDate)
        val spent = transactions
            .filter { it.category == budget.category && it.type == TransactionType.EXPENSE }
            .filter { it.date in start..end }
            .sumOf { it.amount }

        val progress = if (budget.amount > 0.0) {
            ((spent / budget.amount) * 100.0).toInt()
        } else {
            0
        }
        val remaining = budget.amount - spent
        val status = when {
            progress >= 100 -> BudgetHealthStatus.EXCEEDED
            progress >= budget.warningThreshold -> BudgetHealthStatus.WARNING
            else -> BudgetHealthStatus.SAFE
        }

        return BudgetHealth(
            budget = budget,
            spentAmount = spent,
            remainingAmount = remaining,
            progressPercentage = progress,
            status = status
        )
    }

    fun exportTransactionsToCsv(
        transactions: List<Transaction>,
        currencyCode: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("ID,Date,Type,Category,Description,Amount,Currency")
        transactions
            .sortedByDescending { it.date }
            .forEach { tx ->
                val escapedDesc = "\"${tx.description.replace("\"", "\"\"")}\""
                val formattedAmount = String.format(Locale.US, "%.2f", tx.amount)
                sb.appendLine("${tx.id},${dateFormat.format(tx.date)},${tx.type.name},${tx.category.displayName},$escapedDesc,$formattedAmount,$currencyCode")
            }
        return sb.toString()
    }

    fun generateSampleTransactions(referenceDate: Date = Date()): List<Transaction> {
        val list = mutableListOf<Transaction>()
        var idCounter = 1L

        fun daysAgo(days: Int): Date {
            return Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }

        fun monthsAgoDay(monthsAgo: Int, dayOfMonth: Int): Date {
            return Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -monthsAgo)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDay))
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }

        // Current month income & expenses
        list += Transaction(idCounter++, 4200.0, "Monthly Software Salary", TransactionCategory.SALARY, TransactionType.INCOME, monthsAgoDay(0, 1))
        list += Transaction(idCounter++, 350.0, "Dividend Payout", TransactionCategory.INVESTMENT, TransactionType.INCOME, daysAgo(4))
        list += Transaction(idCounter++, 1250.0, "Apartment Rent", TransactionCategory.HOUSING, TransactionType.EXPENSE, monthsAgoDay(0, 2))
        list += Transaction(idCounter++, 145.50, "Weekly Grocery Run", TransactionCategory.FOOD, TransactionType.EXPENSE, daysAgo(1))
        list += Transaction(idCounter++, 62.80, "Coffee & Bistro Lunch", TransactionCategory.FOOD, TransactionType.EXPENSE, daysAgo(3))
        list += Transaction(idCounter++, 94.00, "Electricity & Fiber Internet", TransactionCategory.UTILITIES, TransactionType.EXPENSE, daysAgo(5))
        list += Transaction(idCounter++, 48.20, "Metro Pass & Rideshare", TransactionCategory.TRANSPORTATION, TransactionType.EXPENSE, daysAgo(2))
        list += Transaction(idCounter++, 110.00, "Wireless Headphones", TransactionCategory.SHOPPING, TransactionType.EXPENSE, daysAgo(6))
        list += Transaction(idCounter++, 55.00, "Gym Membership & Pharmacy", TransactionCategory.HEALTH, TransactionType.EXPENSE, daysAgo(7))
        list += Transaction(idCounter++, 39.99, "Movie Night & Streaming", TransactionCategory.ENTERTAINMENT, TransactionType.EXPENSE, daysAgo(4))

        // Previous months for rich 6-month cash flow analytics
        val pastMonths = listOf(1, 2, 3, 4, 5)
        for (m in pastMonths) {
            list += Transaction(idCounter++, 4100.0 + (m % 2) * 100.0, "Monthly Salary", TransactionCategory.SALARY, TransactionType.INCOME, monthsAgoDay(m, 1))
            if (m == 2) {
                list += Transaction(idCounter++, 600.0, "Quarterly Performance Bonus", TransactionCategory.BONUS, TransactionType.INCOME, monthsAgoDay(m, 15))
            }
            list += Transaction(idCounter++, 1250.0, "Apartment Rent", TransactionCategory.HOUSING, TransactionType.EXPENSE, monthsAgoDay(m, 2))
            list += Transaction(idCounter++, 320.0 + m * 15.0, "Groceries & Dining", TransactionCategory.FOOD, TransactionType.EXPENSE, monthsAgoDay(m, 10))
            list += Transaction(idCounter++, 115.0 + m * 8.0, "Utilities & Internet", TransactionCategory.UTILITIES, TransactionType.EXPENSE, monthsAgoDay(m, 12))
            list += Transaction(idCounter++, 95.0, "Transit & Fuel", TransactionCategory.TRANSPORTATION, TransactionType.EXPENSE, monthsAgoDay(m, 16))
            list += Transaction(idCounter++, 140.0 + (m % 3) * 45.0, "Clothing & Gadgets", TransactionCategory.SHOPPING, TransactionType.EXPENSE, monthsAgoDay(m, 20))
            if (m == 3) {
                list += Transaction(idCounter++, 280.0, "Weekend Train Trip", TransactionCategory.TRAVEL, TransactionType.EXPENSE, monthsAgoDay(m, 22))
            }
        }

        return list
    }

    fun generateSampleBudgets(referenceDate: Date = Date()): List<Budget> {
        val (start, end) = periodBounds(BudgetPeriod.MONTHLY, referenceDate)
        return listOf(
            Budget(1L, TransactionCategory.FOOD, 400.0, start, end, 80, BudgetPeriod.MONTHLY),
            Budget(2L, TransactionCategory.HOUSING, 1300.0, start, end, 90, BudgetPeriod.MONTHLY),
            Budget(3L, TransactionCategory.SHOPPING, 120.0, start, end, 75, BudgetPeriod.MONTHLY),
            Budget(4L, TransactionCategory.TRANSPORTATION, 150.0, start, end, 80, BudgetPeriod.MONTHLY)
        )
    }
}
