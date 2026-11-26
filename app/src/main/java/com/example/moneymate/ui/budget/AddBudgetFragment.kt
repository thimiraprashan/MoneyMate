package com.example.moneymate.ui.budget

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moneymate.data.Budget
import com.example.moneymate.data.BudgetManager
import com.example.moneymate.data.BudgetPeriod
import com.example.moneymate.data.FinanceAnalytics
import com.example.moneymate.data.SettingsManager
import com.example.moneymate.data.TransactionCategory
import com.example.moneymate.databinding.FragmentAddBudgetBinding
import com.example.moneymate.utils.CurrencyManager

class AddBudgetFragment : Fragment() {

    private var _binding: FragmentAddBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var budgetManager: BudgetManager
    private lateinit var settingsManager: SettingsManager
    private var budgetId: Long? = null
    private var selectedCategory: TransactionCategory? = null
    private var selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        budgetManager = BudgetManager(requireContext())
        settingsManager = SettingsManager(requireContext())
        budgetId = arguments?.getLong("budgetId", -1L)?.takeIf { it != -1L }

        val currency = CurrencyManager.getInstance(requireContext()).getCurrentCurrency()
        binding.amountLayout.prefixText = "${currency.symbol} "

        setupCategoryDropdown()
        setupPeriodDropdown()
        loadExistingBudget()
        setupSaveButton()
    }

    private fun setupCategoryDropdown() {
        val categories = TransactionCategory.expenseCategories()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories.map { it.displayName }
        )
        binding.categoryInput.setAdapter(adapter)
        binding.categoryInput.setOnClickListener { binding.categoryInput.showDropDown() }
        binding.categoryInput.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            binding.categoryLayout.error = null
        }
    }

    private fun setupPeriodDropdown() {
        val periods = listOf(BudgetPeriod.WEEKLY, BudgetPeriod.MONTHLY, BudgetPeriod.YEARLY)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            periods.map { it.name }
        )
        binding.periodInput.setAdapter(adapter)
        binding.periodInput.setText(selectedPeriod.name, false)
        binding.periodInput.setOnClickListener { binding.periodInput.showDropDown() }
        binding.periodInput.setOnItemClickListener { _, _, position, _ ->
            selectedPeriod = periods[position]
        }
    }

    private fun loadExistingBudget() {
        val existing = budgetId?.let { budgetManager.getBudgetById(it) }
        if (existing != null) {
            binding.budgetFormHeading.text = "Edit Budget"
            selectedCategory = existing.category
            selectedPeriod = existing.effectivePeriod()
            binding.amountInput.setText(existing.amount.toString())
            binding.categoryInput.setText(existing.category.displayName, false)
            binding.periodInput.setText(selectedPeriod.name, false)
            binding.warningThresholdInput.setText(existing.warningThreshold.toString())
        } else {
            binding.warningThresholdInput.setText(settingsManager.budgetWarningThreshold.toString())
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveBudget()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.amountLayout.error = null
        binding.categoryLayout.error = null
        binding.warningThresholdLayout.error = null

        val amount = binding.amountInput.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            binding.amountLayout.error = "Enter a valid budget limit greater than 0"
            isValid = false
        }

        val catText = binding.categoryInput.text.toString()
        if (selectedCategory == null && TransactionCategory.fromInput(catText) == null) {
            binding.categoryLayout.error = "Select a category"
            isValid = false
        }

        val threshold = binding.warningThresholdInput.text.toString().toIntOrNull()
        if (threshold == null || threshold !in 1..100) {
            binding.warningThresholdLayout.error = "Threshold must be between 1 and 100"
            isValid = false
        }

        return isValid
    }

    private fun saveBudget() {
        try {
            val amount = binding.amountInput.text.toString().toDouble()
            val category = selectedCategory
                ?: TransactionCategory.fromInput(binding.categoryInput.text.toString())
                ?: return
            val warningThreshold = binding.warningThresholdInput.text.toString().toInt()
            val (start, end) = FinanceAnalytics.periodBounds(selectedPeriod)

            val budget = Budget(
                id = budgetId ?: 0L,
                category = category,
                amount = amount,
                startDate = start,
                endDate = end,
                warningThreshold = warningThreshold,
                period = selectedPeriod
            )

            budgetManager.saveBudget(budget)
            Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } catch (e: Exception) {
            Log.e("AddBudgetFragment", "Error saving budget", e)
            Toast.makeText(requireContext(), "Error saving budget: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
