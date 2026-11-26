package com.example.moneymate

import com.example.moneymate.data.Budget
import com.example.moneymate.data.BudgetHealthStatus
import com.example.moneymate.data.BudgetPeriod
import com.example.moneymate.data.FinanceAnalytics
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionCategory
import com.example.moneymate.data.TransactionSort
import com.example.moneymate.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class FinanceAnalyticsTest {

    private fun dateOf(year: Int, month: Int, day: Int): Date {
        return Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    @Test
    fun computeMonthlySummary_calculatesTotalsBalanceSavingsAndBreakdown() {
        val transactions = listOf(
            Transaction(1L, 3000.0, "Salary", TransactionCategory.SALARY, TransactionType.INCOME, dateOf(2026, Calendar.NOVEMBER, 1)),
            Transaction(2L, 600.0, "Groceries", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 5)),
            Transaction(3L, 300.0, "Rent", TransactionCategory.HOUSING, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 8)),
            Transaction(4L, 500.0, "Old Expense", TransactionCategory.TRAVEL, TransactionType.EXPENSE, dateOf(2026, Calendar.OCTOBER, 20))
        )

        val summary = FinanceAnalytics.computeMonthlySummary(transactions, 2026, Calendar.NOVEMBER)

        assertEquals(3000.0, summary.totalIncome, 0.001)
        assertEquals(900.0, summary.totalExpenses, 0.001)
        assertEquals(2100.0, summary.balance, 0.001)
        assertEquals(70, summary.savingsRatePercent)
        assertEquals(3, summary.transactionCount)
        assertEquals(2, summary.expenseBreakdown.size)
        assertEquals(TransactionCategory.FOOD, summary.expenseBreakdown[0].category)
    }

    @Test
    fun filterTransactions_filtersByQueryTypeAndSortsCorrectly() {
        val transactions = listOf(
            Transaction(1L, 120.0, "Coffee Shop", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 2)),
            Transaction(2L, 2500.0, "Monthly Salary", TransactionCategory.SALARY, TransactionType.INCOME, dateOf(2026, Calendar.NOVEMBER, 1)),
            Transaction(3L, 450.0, "Supermarket Food", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 10))
        )

        val foodExpenses = FinanceAnalytics.filterTransactions(
            transactions = transactions,
            query = "food",
            typeFilter = TransactionType.EXPENSE,
            sort = TransactionSort.HIGHEST_AMOUNT
        )

        assertEquals(2, foodExpenses.size)
        assertEquals(450.0, foodExpenses[0].amount, 0.001)
        assertEquals(120.0, foodExpenses[1].amount, 0.001)
    }

    @Test
    fun evaluateBudgetHealth_detectsSafeWarningAndExceeded() {
        val refDate = dateOf(2026, Calendar.NOVEMBER, 15)
        val (start, end) = FinanceAnalytics.periodBounds(BudgetPeriod.MONTHLY, refDate)

        val foodBudget = Budget(1L, TransactionCategory.FOOD, 200.0, start, end, 80, BudgetPeriod.MONTHLY)
        val shoppingBudget = Budget(2L, TransactionCategory.SHOPPING, 100.0, start, end, 75, BudgetPeriod.MONTHLY)
        val travelBudget = Budget(3L, TransactionCategory.TRAVEL, 500.0, start, end, 80, BudgetPeriod.MONTHLY)

        val transactions = listOf(
            Transaction(1L, 170.0, "Dinner", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 10)),
            Transaction(2L, 130.0, "Shoes", TransactionCategory.SHOPPING, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 12)),
            Transaction(3L, 100.0, "Train", TransactionCategory.TRAVEL, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 14))
        )

        val foodHealth = FinanceAnalytics.evaluateBudgetHealth(foodBudget, transactions, refDate)
        val shoppingHealth = FinanceAnalytics.evaluateBudgetHealth(shoppingBudget, transactions, refDate)
        val travelHealth = FinanceAnalytics.evaluateBudgetHealth(travelBudget, transactions, refDate)

        assertEquals(BudgetHealthStatus.WARNING, foodHealth.status)
        assertEquals(85, foodHealth.progressPercentage)
        assertEquals(30.0, foodHealth.remainingAmount, 0.001)

        assertEquals(BudgetHealthStatus.EXCEEDED, shoppingHealth.status)
        assertEquals(130, shoppingHealth.progressPercentage)
        assertEquals(-30.0, shoppingHealth.remainingAmount, 0.001)

        assertEquals(BudgetHealthStatus.SAFE, travelHealth.status)
        assertEquals(20, travelHealth.progressPercentage)
    }

    @Test
    fun computeInsightsAndCashFlow_computesMoMDeltaAndSeries() {
        val transactions = listOf(
            Transaction(1L, 1000.0, "Oct Food", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.OCTOBER, 10)),
            Transaction(2L, 500.0, "Nov Food", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 5)),
            Transaction(3L, 2000.0, "Nov Salary", TransactionCategory.SALARY, TransactionType.INCOME, dateOf(2026, Calendar.NOVEMBER, 1))
        )

        val insights = FinanceAnalytics.computeInsights(
            transactions = transactions,
            year = 2026,
            month = Calendar.NOVEMBER,
            referenceNow = dateOf(2026, Calendar.NOVEMBER, 10)
        )
        assertNotNull(insights.topExpenseCategory)
        assertEquals(TransactionCategory.FOOD, insights.topExpenseCategory?.category)
        assertEquals(50.0, insights.averageDailyExpense, 0.001)
        assertEquals(-50, insights.expenseChangePercent)

        val cashFlow = FinanceAnalytics.computeRecentCashFlow(transactions, monthsCount = 6, referenceDate = dateOf(2026, Calendar.NOVEMBER, 10))
        assertEquals(6, cashFlow.size)
        assertEquals(2000.0, cashFlow.last().income, 0.001)
        assertEquals(500.0, cashFlow.last().expense, 0.001)
    }

    @Test
    fun exportTransactionsToCsv_escapesQuotesAndIncludesHeader() {
        val transactions = listOf(
            Transaction(1L, 45.5, "Lunch, \"Special\"", TransactionCategory.FOOD, TransactionType.EXPENSE, dateOf(2026, Calendar.NOVEMBER, 5))
        )

        val csv = FinanceAnalytics.exportTransactionsToCsv(transactions, "USD")
        val lines = csv.trim().lines()

        assertEquals(2, lines.size)
        assertEquals("ID,Date,Type,Category,Description,Amount,Currency", lines[0])
        assertTrue(lines[1].contains("\"Lunch, \"\"Special\"\"\""))
        assertTrue(lines[1].endsWith(",45.50,USD"))
    }

    @Test
    fun generateSampleData_producesValidTransactionsAndBudgets() {
        val sampleTx = FinanceAnalytics.generateSampleTransactions()
        val sampleBudgets = FinanceAnalytics.generateSampleBudgets()

        assertTrue(sampleTx.size >= 20)
        assertTrue(sampleTx.all { it.amount > 0 })
        assertEquals(4, sampleBudgets.size)
    }
}
