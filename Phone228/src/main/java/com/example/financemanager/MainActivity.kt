package com.example.financemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financemanager.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Код для запроса разрешения на уведомления
    private val NOTIFICATION_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ЗАПРОС РАЗРЕШЕНИЯ НА УВЕДОМЛЕНИЯ
        requestNotificationPermission()

        // Обработка "Поделиться" при запуске
        handleSharedIntent(intent)

        // Начальный фрагмент
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
        }

        // Находим BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Обработчик навигации
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

    // НОВЫЙ МЕТОД: Запрос разрешения на уведомления
    private fun requestNotificationPermission() {
        // Только для Android 13 (API 33) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

                // Показываем объяснение, если нужно
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(this,
                        "Приложению нужно разрешение для показа уведомлений",
                        Toast.LENGTH_SHORT).show()
                }

                // Запрашиваем разрешение
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    // Обработка ответа на запрос разрешения
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
            } else {
                // Пользователь отказал
                Toast.makeText(this,
                    "Уведомления отключены. Включите в настройках приложения",
                    Toast.LENGTH_LONG).show()
            }
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
                replaceFragment(TransactionsFragment().apply {
                    arguments = Bundle().apply {
                        putString("shared_text", sharedText)
                    }
                })

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