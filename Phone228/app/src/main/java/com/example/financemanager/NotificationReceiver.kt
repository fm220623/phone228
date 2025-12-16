package com.example.financemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.data.repository.SimpleFinanceRepository

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_QUICK_INCOME -> {
                val amount = intent.getDoubleExtra("amount", 1000.0)
                addQuickTransaction(context, amount, TransactionType.INCOME, "Быстрый доход")
                Toast.makeText(context, "Добавлен быстрый доход: $amount ₽", Toast.LENGTH_SHORT).show()
            }
            NotificationHelper.ACTION_QUICK_EXPENSE -> {
                val amount = intent.getDoubleExtra("amount", 500.0)
                addQuickTransaction(context, amount, TransactionType.EXPENSE, "Быстрый расход")
                Toast.makeText(context, "Добавлен быстрый расход: $amount ₽", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addQuickTransaction(
        context: Context,
        amount: Double,
        type: TransactionType,
        description: String
    ) {
        val repository = SimpleFinanceRepository.getInstance(context)

        // Определяем категорию в зависимости от типа
        val category = if (type == TransactionType.INCOME) "💰 Зарплата" else "⚡ Прочее"
        val categoryId = if (type == TransactionType.INCOME) 10L else 1L

        val transaction = Transaction(
            amount = amount,
            category = category,  // ДОБАВЛЕНО поле category
            categoryId = categoryId,
            type = type,
            description = description
        )
        repository.addTransaction(transaction)

        // Показываем новое уведомление о добавлении
        NotificationHelper(context).showTransactionNotification(transaction)
    }
}