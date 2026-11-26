package com.example.moneymate.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymate.R
import com.example.moneymate.data.BudgetHealthStatus
import com.example.moneymate.data.BudgetManager
import com.example.moneymate.data.FinanceAnalytics
import com.example.moneymate.data.TransactionManager
import com.example.moneymate.data.TransactionType
import com.example.moneymate.databinding.FragmentDashboardBinding
import com.example.moneymate.utils.CurrencyManager
import com.example.moneymate.utils.UserManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionManager: TransactionManager
    private lateinit var budgetManager: BudgetManager
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var currencyManager: CurrencyManager
    private lateinit var userManager: UserManager

    private val selectedMonthCal: Calendar = Calendar.getInstance()
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionManager = TransactionManager(requireContext())
        budgetManager = BudgetManager(requireContext())
        currencyManager = CurrencyManager.getInstance(requireContext())
        userManager = UserManager.getInstance(requireContext())

        setupGreeting()
        setupCategoryList()
        setupPieChart()
        setupActions()
        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun setupGreeting() {
        val user = userManager.getCurrentUser()
        binding.greetingText.text = if (user != null) "Hi, ${user.name}" else "Welcome back"
    }

    private fun setupActions() {
        binding.prevMonthButton.setOnClickListener {
            selectedMonthCal.add(Calendar.MONTH, -1)
            updateDashboard()
        }
        binding.nextMonthButton.setOnClickListener {
            selectedMonthCal.add(Calendar.MONTH, 1)
            updateDashboard()
        }
        binding.quickAddExpenseBtn.setOnClickListener {
            val bundle = Bundle().apply { putString("defaultType", TransactionType.EXPENSE.name) }
            findNavController().navigate(R.id.action_dashboardFragment_to_addTransactionFragment, bundle)
        }
        binding.quickAddIncomeBtn.setOnClickListener {
            val bundle = Bundle().apply { putString("defaultType", TransactionType.INCOME.name) }
            findNavController().navigate(R.id.action_dashboardFragment_to_addTransactionFragment, bundle)
        }
        binding.quickAnalyticsBtn.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_analyticsFragment)
        }
        binding.budgetHealthCard.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_budgetFragment)
        }
    }

    private fun setupPieChart() {
        binding.categoryPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(80)
            setHoleRadius(60f)
            setTransparentCircleRadius(64f)
            setDrawCenterText(true)
            setCenterTextSize(13f)
            setCenterTextColor(requireContext().getColor(R.color.text_primary))
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.isEnabled = false
        }
    }

    private fun setupCategoryList() {
        categoryAdapter = CategoryAdapter(currencyManager)
        binding.categoryList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    private fun updateDashboard() {
        val year = selectedMonthCal.get(Calendar.YEAR)
        val month = selectedMonthCal.get(Calendar.MONTH)
        binding.monthLabelText.text = monthFormat.format(selectedMonthCal.time)

        val allTransactions = transactionManager.getTransactions()
        val summary = FinanceAnalytics.computeMonthlySummary(allTransactions, year, month)

        binding.balanceValue.text = currencyManager.formatAmount(summary.balance)
        binding.totalIncomeValue.text = currencyManager.formatAmount(summary.totalIncome)
        binding.totalExpensesValue.text = currencyManager.formatAmount(summary.totalExpenses)
        binding.savingsRateBadge.text = "Savings: ${summary.savingsRatePercent}%"
        binding.transactionCountText.text =
            if (summary.transactionCount == 1) "1 transaction this month"
            else "${summary.transactionCount} transactions this month"

        // Budget health snapshot
        val budgetHealth = budgetManager.getAllBudgetHealth()
        if (budgetHealth.isEmpty()) {
            binding.budgetHealthSummaryText.text = "No budgets set yet. Tap to configure limits."
        } else {
            val exceeded = budgetHealth.count { it.status == BudgetHealthStatus.EXCEEDED }
            val warning = budgetHealth.count { it.status == BudgetHealthStatus.WARNING }
            val safe = budgetHealth.count { it.status == BudgetHealthStatus.SAFE }
            binding.budgetHealthSummaryText.text = buildString {
                append("${budgetHealth.size} active: ")
                append("$safe on track")
                if (warning > 0) append(", $warning near limit")
                if (exceeded > 0) append(", $exceeded exceeded")
            }
        }

        // Expense breakdown chart
        if (summary.expenseBreakdown.isEmpty()) {
            binding.categoryPieChart.visibility = View.GONE
            binding.emptyChartText.visibility = View.VISIBLE
            categoryAdapter.updateCategories(emptyList())
        } else {
            binding.categoryPieChart.visibility = View.VISIBLE
            binding.emptyChartText.visibility = View.GONE

            val entries = summary.expenseBreakdown.map {
                PieEntry(it.amount.toFloat(), it.category.displayName)
            }
            val colors = summary.expenseBreakdown.map { it.category.colorHex }
            val dataSet = PieDataSet(entries, "Expenses").apply {
                this.colors = colors
                sliceSpace = 2f
                valueTextSize = 11f
                valueTextColor = Color.WHITE
                valueFormatter = PercentFormatter(binding.categoryPieChart)
            }
            binding.categoryPieChart.centerText = "Spent\n${currencyManager.formatAmount(summary.totalExpenses)}"
            binding.categoryPieChart.data = PieData(dataSet)
            binding.categoryPieChart.animateY(600)
            binding.categoryPieChart.invalidate()

            categoryAdapter.updateCategories(summary.expenseBreakdown)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
