package com.example.moneymate.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.moneymate.LoginActivity
import com.example.moneymate.data.Currency
import com.example.moneymate.data.SettingsManager
import com.example.moneymate.databinding.FragmentSettingsBinding
import com.example.moneymate.utils.CurrencyManager
import com.example.moneymate.utils.UserManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager
    private lateinit var currencyManager: CurrencyManager
    private lateinit var userManager: UserManager
    private var suppressInitialCurrencyCallback = true

    private val exportJsonLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { writeTextToUri(it, settingsManager.exportData(), "JSON backup saved") }
        }

    private val exportCsvLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { writeTextToUri(it, settingsManager.exportTransactionsCsv(), "CSV report exported") }
        }

    private val importDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                            val json = inputStream.bufferedReader().use { it.readText() }
                            if (settingsManager.importData(json)) {
                                Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                                setupCurrencySpinner()
                                setupWarningThreshold()
                            } else {
                                Toast.makeText(context, "Failed to import data: Invalid format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("SettingsFragment", "Import failed", e)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext())
        currencyManager = CurrencyManager.getInstance(requireContext())
        userManager = UserManager.getInstance(requireContext())

        setupProfileCard()
        setupCurrencySpinner()
        setupWarningThreshold()
        setupButtons()
    }

    private fun setupProfileCard() {
        val user = userManager.getCurrentUser()
        binding.profileNameText.text = user?.name ?: "Guest User"
        binding.profileEmailText.text = user?.email ?: "Local Profile"

        binding.logoutButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Sign out of your MoneyMate account? Your local transactions and budgets stay saved on this device.")
                .setPositiveButton("Log Out") { _, _ ->
                    userManager.logoutUser()
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupCurrencySpinner() {
        suppressInitialCurrencyCallback = true
        val currencyAdapter = CurrencyAdapter(requireContext())
        binding.currencySpinner.adapter = currencyAdapter

        val currentCurrency = currencyManager.getCurrentCurrency()
        val position = Currency.values().indexOf(currentCurrency)
        if (position != -1) {
            binding.currencySpinner.setSelection(position, false)
        }

        binding.currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (suppressInitialCurrencyCallback) {
                    suppressInitialCurrencyCallback = false
                    return
                }
                val selectedCurrency = Currency.values()[pos]
                if (selectedCurrency != currencyManager.getCurrentCurrency()) {
                    currencyManager.setCurrency(selectedCurrency)
                    settingsManager.currency = selectedCurrency.code
                    Toast.makeText(
                        requireContext(),
                        "Currency changed to ${selectedCurrency.displayName} (${selectedCurrency.code})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupWarningThreshold() {
        binding.warningThresholdInput.setText(settingsManager.budgetWarningThreshold.toString())
    }

    private fun setupButtons() {
        binding.exportCsvButton.setOnClickListener {
            exportCsvLauncher.launch("moneymate_transactions.csv")
        }
        binding.exportButton.setOnClickListener {
            exportJsonLauncher.launch("moneymate_backup.json")
        }
        binding.importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            importDataLauncher.launch(intent)
        }
        binding.loadDemoButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Load Sample Demo Data")
                .setMessage("This will populate 6 months of realistic sample transactions and monthly budgets so you can explore all charts and features.")
                .setPositiveButton("Load Demo Data") { _, _ ->
                    settingsManager.loadSampleDemoData()
                    Toast.makeText(requireContext(), "Sample demo data loaded!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.clearDataButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Financial Data")
                .setMessage("Are you sure you want to delete all transactions and budgets? This cannot be undone.")
                .setPositiveButton("Clear All") { _, _ ->
                    settingsManager.clearAllFinancialData()
                    Toast.makeText(requireContext(), "All transactions and budgets cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun writeTextToUri(uri: Uri, content: String, successMessage: String) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        val threshold = binding.warningThresholdInput.text.toString().toIntOrNull()
        if (threshold != null) {
            settingsManager.budgetWarningThreshold = threshold
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
