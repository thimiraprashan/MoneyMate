package com.example.moneymate.ui.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneymate.R
import com.example.moneymate.data.FinanceAnalytics
import com.example.moneymate.data.TransactionManager
import com.example.moneymate.data.TransactionType
import com.example.moneymate.utils.CurrencyManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.chip.ChipGroup
import java.util.Calendar
import kotlin.math.abs

class CategoryAnalysisFragment : Fragment() {

    private lateinit var avgDailySpendText: TextView
    private lateinit var topCategoryText: TextView
    private lateinit var momTrendText: TextView
    private lateinit var emptyBreakdownText: TextView
    private lateinit var cashFlowBarChart: BarChart
    private lateinit var categoryChart: PieChart
    private lateinit var categoryList: RecyclerView
    private lateinit var breakdownChipGroup: ChipGroup

    private lateinit var transactionManager: TransactionManager
    private lateinit var currencyManager: CurrencyManager
    private lateinit var adapter: CategoryAdapter

    private var selectedType: TransactionType = TransactionType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionManager = TransactionManager(requireContext())
        currencyManager = CurrencyManager.getInstance(requireContext())

        avgDailySpendText = view.findViewById(R.id.avgDailySpendText)
        topCategoryText = view.findViewById(R.id.topCategoryText)
        momTrendText = view.findViewById(R.id.momTrendText)
        emptyBreakdownText = view.findViewById(R.id.emptyBreakdownText)
        cashFlowBarChart = view.findViewById(R.id.cashFlowBarChart)
        categoryChart = view.findViewById(R.id.categoryChart)
        categoryList = view.findViewById(R.id.categoryList)
        breakdownChipGroup = view.findViewById(R.id.breakdownChipGroup)

        setupPieChart()
        setupBarChart()
        setupRecyclerView()
        setupChips()
        updateData()
    }

    override fun onResume() {
        super.onResume()
        updateData()
    }

    private fun setupChips() {
        breakdownChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = if (checkedIds.contains(R.id.chipBreakdownIncome)) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
            updateBreakdownOnly()
        }
    }

    private fun setupBarChart() {
        cashFlowBarChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.setCenterAxisLabels(true)
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            legend.isEnabled = true
        }
    }

    private fun setupPieChart() {
        categoryChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(80)
            setHoleRadius(58f)
            setTransparentCircleRadius(62f)
            setDrawCenterText(true)
            setCenterTextSize(13f)
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = false
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(currencyManager)
        categoryList.layoutManager = LinearLayoutManager(requireContext())
        categoryList.adapter = adapter
    }

    private fun updateData() {
        val transactions = transactionManager.getTransactions()
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        // Insights
        val insights = FinanceAnalytics.computeInsights(transactions, year, month)
        avgDailySpendText.text = currencyManager.formatAmount(insights.averageDailyExpense)
        topCategoryText.text = insights.topExpenseCategory?.category?.displayName ?: "None"
        momTrendText.text = when (val delta = insights.expenseChangePercent) {
            null -> "Record expenses across multiple months to unlock month-over-month comparisons."
            0 -> "Your spending is flat compared to last month (Savings Rate: ${insights.savingsRatePercent}%)."
            in 1..Int.MAX_VALUE -> "Spending is up $delta% vs. last month. Savings rate: ${insights.savingsRatePercent}%."
            else -> "Great job! Spending is down ${abs(delta)}% vs. last month. Savings rate: ${insights.savingsRatePercent}%."
        }

        // 6-Month Cash Flow BarChart
        val cashFlow = FinanceAnalytics.computeRecentCashFlow(transactions, monthsCount = 6)
        val incomeEntries = cashFlow.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.income.toFloat())
        }
        val expenseEntries = cashFlow.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.expense.toFloat())
        }

        val incomeSet = BarDataSet(incomeEntries, "Income").apply {
            color = ContextCompat.getColor(requireContext(), R.color.income_green)
            setDrawValues(false)
        }
        val expenseSet = BarDataSet(expenseEntries, "Expenses").apply {
            color = ContextCompat.getColor(requireContext(), R.color.expense_red)
            setDrawValues(false)
        }

        val groupSpace = 0.24f
        val barSpace = 0.04f
        val barWidth = 0.34f
        val barData = BarData(incomeSet, expenseSet).apply {
            this.barWidth = barWidth
        }
        cashFlowBarChart.data = barData
        cashFlowBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(cashFlow.map { it.label })
        cashFlowBarChart.xAxis.axisMinimum = 0f
        cashFlowBarChart.xAxis.axisMaximum = cashFlow.size.toFloat()
        cashFlowBarChart.groupBars(0f, groupSpace, barSpace)
        cashFlowBarChart.animateY(600)
        cashFlowBarChart.invalidate()

        updateBreakdownOnly()
    }

    private fun updateBreakdownOnly() {
        val transactions = transactionManager.getTransactions()
        val now = Calendar.getInstance()
        val summary = FinanceAnalytics.computeMonthlySummary(
            transactions,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH)
        )

        val breakdown = if (selectedType == TransactionType.INCOME) {
            summary.incomeBreakdown
        } else {
            summary.expenseBreakdown
        }
        val total = if (selectedType == TransactionType.INCOME) {
            summary.totalIncome
        } else {
            summary.totalExpenses
        }

        if (breakdown.isEmpty()) {
            categoryChart.visibility = View.GONE
            emptyBreakdownText.visibility = View.VISIBLE
            adapter.submitList(emptyList())
        } else {
            categoryChart.visibility = View.VISIBLE
            emptyBreakdownText.visibility = View.GONE

            val entries = breakdown.map { PieEntry(it.amount.toFloat(), it.category.displayName) }
            val dataSet = PieDataSet(entries, selectedType.name).apply {
                colors = breakdown.map { it.category.colorHex }
                sliceSpace = 2f
                valueTextSize = 11f
                valueTextColor = Color.WHITE
                valueFormatter = PercentFormatter(categoryChart)
            }
            val label = if (selectedType == TransactionType.INCOME) "Income" else "Expenses"
            categoryChart.centerText = "$label\n${currencyManager.formatAmount(total)}"
            categoryChart.data = PieData(dataSet)
            categoryChart.animateY(500)
            categoryChart.invalidate()

            adapter.submitList(breakdown)
        }
    }
}
