package com.example.moneymate

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView

class RegistrationActivity : AppCompatActivity() {

    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText

    private lateinit var registerButton: MaterialButton
    private lateinit var loginText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        nameLayout = findViewById(R.id.nameLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)

        registerButton = findViewById(R.id.registerButton)
        loginText = findViewById(R.id.loginText)
    }

    private fun setupListeners() {
        // Add text change listeners for real-time validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        nameInput.addTextChangedListener(textWatcher)
        emailInput.addTextChangedListener(textWatcher)
        passwordInput.addTextChangedListener(textWatcher)
        confirmPasswordInput.addTextChangedListener(textWatcher)

        registerButton.setOnClickListener {
            if (validateInputs()) {
                // TODO: Implement actual registration logic here
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        loginText.setOnClickListener {
            // TODO: Navigate to login screen
            Toast.makeText(this, "Login screen coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (nameInput.text.isNullOrBlank()) {
            nameLayout.error = "Name is required"
            isValid = false
        } else {
            nameLayout.error = null
        }

        // Validate email
        val email = emailInput.text.toString()
        if (email.isBlank()) {
            emailLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            isValid = false
        } else {
            emailLayout.error = null
        }

        // Validate password
        val password = passwordInput.text.toString()
        if (password.isBlank()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordLayout.error = null
        }

        // Validate confirm password
        val confirmPassword = confirmPasswordInput.text.toString()
        if (confirmPassword.isBlank()) {
            confirmPasswordLayout.error = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordLayout.error = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordLayout.error = null
        }

        return isValid
    }
} 