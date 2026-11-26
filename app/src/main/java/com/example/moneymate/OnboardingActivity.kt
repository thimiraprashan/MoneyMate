package com.example.moneymate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.moneymate.utils.UserManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: MaterialButton
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        userManager = UserManager.getInstance(this)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        nextButton = findViewById(R.id.nextButton)
        skipButton = findViewById(R.id.skipButton)

        val onboardingItems = listOf(
            OnboardingItem(
                R.drawable.onboarding_1,
                "Track Every Dollar",
                "Log daily income and expenses in seconds with smart categories and multi-currency support."
            ),
            OnboardingItem(
                R.drawable.onboarding_2,
                "Smart Budget Guardrails",
                "Set weekly, monthly, or yearly limits and receive timely alerts before you overspend."
            ),
            OnboardingItem(
                R.drawable.ic_analytics,
                "Visual Cash Flow Insights",
                "Explore 6-month income vs. expense trends, savings rate, and exportable CSV/JSON reports."
            )
        )

        viewPager.adapter = OnboardingAdapter(onboardingItems)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }

        skipButton.setOnClickListener {
            completeOnboarding()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                nextButton.text = if (position == onboardingItems.size - 1) "Get Started" else "Next"
            }
        })
    }

    private fun completeOnboarding() {
        userManager.setOnboardingCompleted(true)
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }
}

data class OnboardingItem(
    val imageRes: Int,
    val title: String,
    val description: String
)

class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.imageRes)
        holder.titleText.text = item.title
        holder.descriptionText.text = item.description
    }

    override fun getItemCount() = items.size

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }
}
