package com.example.moneymate.ui.budget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneymate.R
import com.example.moneymate.data.Budget
import com.example.moneymate.data.BudgetManager
import com.example.moneymate.databinding.FragmentBudgetBinding
import com.example.moneymate.utils.CurrencyManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var budgetManager: BudgetManager
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var currencyManager: CurrencyManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        budgetManager = BudgetManager(requireContext())
        currencyManager = CurrencyManager.getInstance(requireContext())

        setupRecyclerView()
        setupAddButton()
        loadBudgets()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        loadBudgets()
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(
            healthList = emptyList(),
            onEditClick = { budget -> navigateToEditBudget(budget) },
            onDeleteClick = { budget -> showDeleteConfirmation(budget) },
            currencyManager = currencyManager
        )

        binding.budgetRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = budgetAdapter
        }
    }

    private fun setupAddButton() {
        binding.addBudgetButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_budgetFragment_to_addBudgetFragment)
            } catch (e: Exception) {
                Log.e("BudgetFragment", "Error navigating to add budget", e)
                Toast.makeText(requireContext(), "Error opening add budget screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBudgets() {
        val healthList = budgetManager.getAllBudgetHealth()
        budgetAdapter.updateBudgets(healthList)

        val totalBudgeted = healthList.sumOf { it.budget.amount }
        val totalSpent = healthList.sumOf { it.spentAmount }
        binding.budgetTotalsText.text =
            "${currencyManager.formatAmount(totalSpent)} spent of ${currencyManager.formatAmount(totalBudgeted)}"

        binding.emptyBudgetText.visibility = if (healthList.isEmpty()) View.VISIBLE else View.GONE
        binding.budgetRecyclerView.visibility = if (healthList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun navigateToEditBudget(budget: Budget) {
        try {
            val action = BudgetFragmentDirections.actionBudgetFragmentToAddBudgetFragment(budget.id)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("BudgetFragment", "Error navigating to edit budget", e)
            Toast.makeText(requireContext(), "Error opening edit budget screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(budget: Budget) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Budget")
            .setMessage("Remove the ${budget.category.displayName} budget?")
            .setPositiveButton("Delete") { _, _ ->
                budgetManager.deleteBudget(budget)
                loadBudgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
