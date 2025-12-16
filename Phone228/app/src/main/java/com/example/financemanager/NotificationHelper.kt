package com.example.financemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.model.TransactionType

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_TRANSACTIONS = "finance_transactions"
        const val CHANNEL_ID_REMINDERS = "finance_reminders"
        const val CHANNEL_ID_BUDGETS = "finance_budgets"
        const val NOTIFICATION_ID_TRANSACTION = 1
        const val NOTIFICATION_ID_REMINDER = 2
        const val NOTIFICATION_ID_BUDGET = 3
        const val ACTION_QUICK_INCOME = "quick_income"
        const val ACTION_QUICK_EXPENSE = "quick_expense"
    }

    init {
        createNotificationChannels()
    }

    // НОВЫЙ МЕТОД: проверка, включены ли уведомления
    private fun areNotificationsEnabled(): Boolean {
        val prefs = context.getSharedPreferences("app_settings", 0)
        return prefs.getBoolean("notifications", true) // по умолчанию true (включены)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Канал для транзакций (важные)
            val transactionsChannel = NotificationChannel(
                CHANNEL_ID_TRANSACTIONS,
                "Финансовые транзакции",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о добавлении транзакций"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // Канал для напоминаний (обычные)
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Финансовые напоминания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ежедневные напоминания и статистика"
                enableVibration(false)
            }

            // Канал для бюджетов (важные)
            val budgetsChannel = NotificationChannel(
                CHANNEL_ID_BUDGETS,
                "Бюджеты и лимиты",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о достижении лимитов бюджета"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(transactionsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(budgetsChannel)
        }
    }

    fun showTransactionNotification(transaction: Transaction) {
        // ПРОВЕРКА: если уведомления отключены - выходим
        if (!areNotificationsEnabled()) {
            return
        }

        // Интент для открытия приложения
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_fragment", "transactions")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Быстрые действия (кнопки)
        val quickIncomeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_QUICK_INCOME
            putExtra("amount", 1000.0)
        }

        val quickExpenseIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_QUICK_EXPENSE
            putExtra("amount", 500.0)
        }

        val pendingIncomeIntent = PendingIntent.getBroadcast(
            context,
            1,
            quickIncomeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingExpenseIntent = PendingIntent.getBroadcast(
            context,
            2,
            quickExpenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeText = if (transaction.type == TransactionType.INCOME) "Доход" else "Расход"
        val icon = if (transaction.type == TransactionType.INCOME) {
            android.R.drawable.ic_input_add
        } else {
            android.R.drawable.ic_delete
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSACTIONS)
            .setSmallIcon(icon)
            .setContentTitle("💰 Новая транзакция")
            .setContentText("$typeText: ${transaction.amount} ₽")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_input_add,
                "Быстрый доход",
                pendingIncomeIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Быстрый расход",
                pendingExpenseIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${transaction.description}\nСумма: ${transaction.amount} ₽\nТип: $typeText")
            )
            .build()

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_TRANSACTION, notification)
    }

    fun showDailyReminderNotification(totalIncome: Double, totalExpense: Double) {
        // ПРОВЕРКА: если уведомления отключены - выходим
        if (!areNotificationsEnabled()) {
            return
        }

        val balance = totalIncome - totalExpense

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📊 Ежедневная статистика")
            .setContentText("Баланс: ${String.format("%.2f", balance)} ₽")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Доходы: ${String.format("%.2f", totalIncome)} ₽\n" +
                            "Расходы: ${String.format("%.2f", totalExpense)} ₽\n" +
                            "Баланс: ${String.format("%.2f", balance)} ₽")
            )
            .build()

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    fun showBudgetNotification(
        category: String,
        currentSpent: Double,
        limit: Double,
        isExceeded: Boolean
    ) {
        // ПРОВЕРКА: если уведомления отключены - выходим
        if (!areNotificationsEnabled()) {
            return
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_fragment", "budgets")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            4,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percentage = (currentSpent / limit * 100).toInt()

        val title = if (isExceeded) {
            "🚨 Превышен лимит!"
        } else {
            "⚠️ Близко к лимиту"
        }

        val message = if (isExceeded) {
            "'$category': потрачено $currentSpent ₽ при лимите $limit ₽ ($percentage%)"
        } else {
            "'$category': достигнуто $percentage% лимита ($currentSpent/$limit ₽)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGETS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nНажмите для просмотра бюджетов")
            )
            .build()

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BUDGET, notification)
    }
}