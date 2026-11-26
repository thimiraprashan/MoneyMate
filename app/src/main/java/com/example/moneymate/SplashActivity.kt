package com.example.moneymate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.moneymate.utils.UserManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_splash)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        findViewById<ImageView>(R.id.logoImage).startAnimation(fadeIn)
        findViewById<TextView>(R.id.appNameText).startAnimation(slideUp)
        findViewById<TextView>(R.id.taglineText).startAnimation(slideUp)

        val userManager = UserManager.getInstance(this)
        Handler(Looper.getMainLooper()).postDelayed({
            val nextIntent = when {
                userManager.getCurrentUser() != null -> Intent(this, MainActivity::class.java)
                userManager.hasCompletedOnboarding() -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, OnboardingActivity::class.java)
            }
            startActivity(nextIntent)
            finish()
        }, 1200)
    }
}
