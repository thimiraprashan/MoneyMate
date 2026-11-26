package com.example.moneymate.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.moneymate.data.Transaction
import com.example.moneymate.data.TransactionRepository
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val sharedPreferences = application.getSharedPreferences("MoneyMatePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currencyCode = MutableLiveData<String>()
    val currencyCode: LiveData<String> = _currencyCode

    private val _budgetNotificationsEnabled = MutableLiveData<Boolean>()
    val budgetNotificationsEnabled: LiveData<Boolean> = _budgetNotificationsEnabled

    private val _dailyRemindersEnabled = MutableLiveData<Boolean>()
    val dailyRemindersEnabled: LiveData<Boolean> = _dailyRemindersEnabled

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _currencyCode.value = sharedPreferences.getString("currency_code", "USD") ?: "USD"
        _budgetNotificationsEnabled.value = sharedPreferences.getBoolean("budget_notifications", true)
        _dailyRemindersEnabled.value = sharedPreferences.getBoolean("daily_reminders", true)
    }

    fun setCurrencyCode(code: String) {
        sharedPreferences.edit().putString("currency_code", code).apply()
        _currencyCode.value = code
    }

    fun setBudgetNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("budget_notifications", enabled).apply()
        _budgetNotificationsEnabled.value = enabled
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("daily_reminders", enabled).apply()
        _dailyRemindersEnabled.value = enabled
    }

    fun exportData(): Boolean {
        return try {
            val transactions = repository.getAllTransactions()
            val json = gson.toJson(transactions)
            
            val file = File(getApplication<Application>().filesDir, "moneymate_backup.json")
            FileOutputStream(file).use { output ->
                output.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importData(): Boolean {
        return try {
            val file = File(getApplication<Application>().filesDir, "moneymate_backup.json")
            if (!file.exists()) return false

            val json = FileReader(file).use { it.readText() }
            val transactions = gson.fromJson<List<Transaction>>(json, List::class.java)
            
            // Clear existing data
            repository.deleteAllTransactions()
            
            // Import new data
            transactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
} 
