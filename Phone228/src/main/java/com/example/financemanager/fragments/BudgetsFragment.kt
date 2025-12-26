package com.example.financemanager.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.financemanager.R
import com.example.financemanager.NotificationHelper
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.data.repository.SimpleFinanceRepository

class BudgetsFragment : Fragment() {

    private lateinit var repository: SimpleFinanceRepository
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_budgets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SimpleFinanceRepository.getInstance(requireContext())
        notificationHelper = NotificationHelper(requireContext())

        // Получаем категории
        val categories = repository.expenseCategories

        // Настройка Spinner для выбора категории
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinner_category)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Установка лимита
        view.findViewById<Button>(R.id.btn_set_budget).setOnClickListener {
            val selectedCategory = spinnerCategory.selectedItem.toString()
            val limitText = view.findViewById<EditText>(R.id.edit_budget_limit).text.toString()

            if (limitText.isNotEmpty()) {
                val limit = limitText.toDoubleOrNull() ?: 0.0

                // Сохраняем бюджет в SharedPreferences
                val prefs = requireContext().getSharedPreferences("budgets", 0)
                prefs.edit().putFloat(selectedCategory, limit.toFloat()).apply()

                Toast.makeText(requireContext(),
                    "Лимит для '$selectedCategory' установлен: $limit ₽",
                    Toast.LENGTH_SHORT).show()

                // Проверяем, не превышен ли лимит
                checkBudgetAndNotify(selectedCategory, limit)
            }
        }

        // Тест уведомления о бюджете
        view.findViewById<Button>(R.id.btn_test_budget_notification).setOnClickListener {
            testBudgetNotification()
        }

        // Показываем текущие бюджеты
        displayCurrentBudgets(view, categories)
    }

    private fun checkBudgetAndNotify(category: String, limit: Double) {
        val transactions = repository.transactions.value ?: emptyList()

        // Считаем расходы по выбранной категории - ИСПРАВЛЕНО
        val categoryExpense = transactions
            .filter {
                it.category == category && it.type == TransactionType.EXPENSE
            }
            .sumOf { it.amount }

        // Проверяем лимиты
        if (categoryExpense >= limit) {
            // Превышен лимит
            notificationHelper.showBudgetNotification(
                category,
                categoryExpense,
                limit,
                true
            )
        } else if (categoryExpense >= limit * 0.8) {
            // Достигнут 80% лимита
            notificationHelper.showBudgetNotification(
                category,
                categoryExpense,
                limit,
                false
            )
        }
    }

    private fun testBudgetNotification() {
        // Тестовое уведомление о бюджете
        notificationHelper.showBudgetNotification(
            "🍔 Еда",
            8000.0,
            10000.0,
            false
        )
    }

    private fun displayCurrentBudgets(view: View, categories: List<String>) {
        val prefs = requireContext().getSharedPreferences("budgets", 0)
        val textBudgets = view.findViewById<TextView>(R.id.text_current_budgets)

        val budgetsText = StringBuilder("📊 Текущие лимиты:\n\n")

        categories.forEach { category ->
            val limit = prefs.getFloat(category, 0f)
            if (limit > 0) {
                val transactions = repository.transactions.value ?: emptyList()
                val spent = transactions
                    .filter {
                        it.category == category && it.type == TransactionType.EXPENSE
                    }
                    .sumOf { it.amount }

                val percentage = if (limit > 0) (spent / limit * 100).toInt() else 0
                budgetsText.append("$category: $spent/$limit ₽ ($percentage%)\n")
            }
        }

        if (budgetsText.toString() == "📊 Текущие лимиты:\n\n") {
            budgetsText.append("Лимиты не установлены")
        }

        textBudgets.text = budgetsText.toString()
    }
}