package com.example.financemanager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.financemanager.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ОБРАБОТКА "ПОДЕЛИТЬСЯ" ПРИ ЗАПУСКЕ
        handleSharedIntent(intent)

        // Начальный фрагмент
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        // Находим BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Обработчик навигации - ИСПРАВЛЕНО (используем новый метод)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_budgets -> BudgetsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> return@setOnItemSelectedListener false
            }
            replaceFragment(fragment)
            true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSharedIntent(it) }
    }

    private fun handleSharedIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                // Переходим во фрагмент транзакций и передаем текст
                replaceFragment(TransactionsFragment().apply {
                    arguments = Bundle().apply {
                        putString("shared_text", sharedText)
                    }
                })

                // Выделяем вкладку транзакций
                findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .selectedItemId = R.id.nav_transactions
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}