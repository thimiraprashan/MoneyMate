package com.example.moneymate.ui.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moneymate.R
import com.example.moneymate.data.BudgetManager
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionCategory
import com.example.moneymate.data.TransactionManager
import com.example.moneymate.data.TransactionType
import com.example.moneymate.utils.CurrencyManager
import com.example.moneymate.utils.NotificationHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : Fragment() {

    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var descriptionInputLayout: TextInputLayout
    private lateinit var typeInputLayout: TextInputLayout
    private lateinit var categoryInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var typeInput: AutoCompleteTextView
    private lateinit var categoryInput: AutoCompleteTextView
    private lateinit var dateInput: TextInputEditText
    private lateinit var formHeading: TextView

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private lateinit var transactionManager: TransactionManager
    private lateinit var budgetManager: BudgetManager
    private lateinit var notificationHelper: NotificationHelper
    private var transactionId: Long? = null
    private var selectedCategory: TransactionCategory? = null
    private var selectedType: TransactionType = TransactionType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        transactionManager = TransactionManager(requireContext())
        budgetManager = BudgetManager(requireContext())
        notificationHelper = NotificationHelper(requireContext())
        transactionId = arguments?.getLong("transactionId", -1L)?.takeIf { it != -1L }
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        amountInputLayout = view.findViewById(R.id.amountInputLayout)
        descriptionInputLayout = view.findViewById(R.id.descriptionInputLayout)
        typeInputLayout = view.findViewById(R.id.typeInputLayout)
        categoryInputLayout = view.findViewById(R.id.categoryInputLayout)
        amountInput = view.findViewById(R.id.amountInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        typeInput = view.findViewById(R.id.typeInput)
        categoryInput = view.findViewById(R.id.categoryInput)
        dateInput = view.findViewById(R.id.dateInput)
        formHeading = view.findViewById(R.id.formHeading)
        val saveButton = view.findViewById<View>(R.id.saveButton)

        val currency = CurrencyManager.getInstance(requireContext()).getCurrentCurrency()
        amountInputLayout.prefixText = "${currency.symbol} "

        val types = TransactionType.values().map { it.name }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        typeInput.setAdapter(typeAdapter)
        typeInput.setOnClickListener { typeInput.showDropDown() }
        typeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = TransactionType.values()[position]
            updateCategoriesForType(selectedType, resetIfMismatch = true)
        }

        val existing = transactionId?.let { transactionManager.getTransactionById(it) }
        if (existing != null) {
            formHeading.text = "Edit Transaction"
            selectedType = existing.type
            selectedCategory = existing.category
            amountInput.setText(existing.amount.toString())
            descriptionInput.setText(existing.description)
            typeInput.setText(existing.type.name, false)
            updateCategoriesForType(existing.type, resetIfMismatch = false)
            categoryInput.setText(existing.category.displayName, false)
            calendar.time = existing.date
            dateInput.setText(dateFormat.format(existing.date))
        } else {
            val defaultTypeArg = arguments?.getString("defaultType")
            selectedType = defaultTypeArg?.let {
                runCatching { TransactionType.valueOf(it) }.getOrNull()
            } ?: TransactionType.EXPENSE
            typeInput.setText(selectedType.name, false)
            updateCategoriesForType(selectedType, resetIfMismatch = true)
            dateInput.setText(dateFormat.format(calendar.time))
        }

        dateInput.setOnClickListener { showDatePicker() }
        saveButton.setOnClickListener { saveTransaction() }
    }

    private fun updateCategoriesForType(type: TransactionType, resetIfMismatch: Boolean) {
        val categories = TransactionCategory.forType(type)
        val names = categories.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        categoryInput.setAdapter(adapter)
        categoryInput.setOnClickListener { categoryInput.showDropDown() }
        categoryInput.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            categoryInputLayout.error = null
        }

        if (resetIfMismatch && (selectedCategory == null || selectedCategory?.type != type)) {
            selectedCategory = categories.firstOrNull()
            categoryInput.setText(selectedCategory?.displayName.orEmpty(), false)
        }
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                dateInput.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun saveTransaction() {
        amountInputLayout.error = null
        descriptionInputLayout.error = null
        typeInputLayout.error = null
        categoryInputLayout.error = null

        val amount = amountInput.text.toString().toDoubleOrNull()
        val description = descriptionInput.text.toString().trim()
        val typeText = typeInput.text.toString().trim()
        val categoryText = categoryInput.text.toString().trim()

        var valid = true
        if (amount == null || amount <= 0.0) {
            amountInputLayout.error = "Enter an amount greater than 0"
            valid = false
        }
        if (description.isBlank()) {
            descriptionInputLayout.error = "Description is required"
            valid = false
        }
        val parsedType = runCatching { TransactionType.valueOf(typeText) }.getOrNull()
        if (parsedType == null) {
            typeInputLayout.error = "Select a type"
            valid = false
        }
        val parsedCategory = selectedCategory ?: TransactionCategory.fromInput(categoryText)
        if (parsedCategory == null) {
            categoryInputLayout.error = "Select a category"
            valid = false
        }
        if (!valid || amount == null || parsedType == null || parsedCategory == null) return

        val transaction = Transaction(
            id = transactionId ?: 0,
            amount = amount,
            description = description,
            date = calendar.time,
            type = parsedType,
            category = parsedCategory
        )

        try {
            transactionManager.saveTransaction(transaction)
            if (parsedType == TransactionType.EXPENSE) {
                budgetManager.evaluateAlertForCategory(parsedCategory, calendar.time)?.let { health ->
                    notificationHelper.showBudgetThresholdNotification(
                        parsedCategory,
                        health.progressPercentage
                    )
                }
            }
            Toast.makeText(requireContext(), "Transaction saved", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message ?: "Error saving transaction", Toast.LENGTH_SHORT).show()
        }
    }
}
