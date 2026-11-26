package com.example.moneymate

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moneymate.data.SettingsManager
import com.example.moneymate.data.TransactionManager
import com.example.moneymate.databinding.ActivityLoginBinding
import com.example.moneymate.utils.UserManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager.getInstance(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInputs(email, password)) {
                val user = userManager.loginUser(email, password)
                if (user != null) {
                    Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    binding.tilPassword.error = "Invalid email or password"
                }
            }
        }

        binding.btnDemoLogin.setOnClickListener {
            val demoUser = userManager.loginDemoUser()
            if (TransactionManager(this).getTransactions().isEmpty()) {
                SettingsManager(this).loadSampleDemoData()
            }
            Toast.makeText(this, "Signed in as ${demoUser.name} (Demo)", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        binding.tvForgotPassword.setOnClickListener {
            showResetPasswordDialog()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun showResetPasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val emailInput = EditText(this).apply {
            hint = "Registered Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.etEmail.text?.toString().orEmpty())
        }
        val newPasswordInput = EditText(this).apply {
            hint = "New Password (min 6 chars)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(emailInput)
        container.addView(newPasswordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your registered email and a new password.")
            .setView(container)
            .setPositiveButton("Reset") { _, _ ->
                val email = emailInput.text.toString().trim()
                val newPass = newPasswordInput.text.toString()
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPass.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (userManager.resetPassword(email, newPass)) {
                    Toast.makeText(this, "Password updated! You can now sign in.", Toast.LENGTH_SHORT).show()
                    binding.etEmail.setText(email)
                    binding.etPassword.setText("")
                } else {
                    Toast.makeText(this, "No account found for that email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }
}
