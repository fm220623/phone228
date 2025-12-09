package com.example.financemanager.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.data.repository.SimpleFinanceRepository
import com.example.financemanager.ui.transactions.TransactionsAdapter
import kotlin.random.Random

class TransactionsFragment : Fragment() {

    private lateinit var repository: SimpleFinanceRepository
    private lateinit var adapter: TransactionsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация репозитория
        repository = SimpleFinanceRepository(requireContext())

        // Настройка RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionsAdapter(repository.transactions.value ?: emptyList()) { transaction ->
            showDeleteDialog(transaction)
        }
        recyclerView.adapter = adapter

        // Наблюдаем за данными
        repository.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.updateData(transactions)
            updateStatistics(view, transactions)
        }

        // Кнопка добавления
        view.findViewById<Button>(R.id.btn_add_transaction).setOnClickListener {
            showAddTransactionDialog()
        }

        // Обновляем статистику при старте
        updateStatistics(view, repository.transactions.value ?: emptyList())
    }

    private fun showAddTransactionDialog() {
        // Простой диалог без Spinner
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_simple_transaction, null)

        AlertDialog.Builder(requireContext())
            .setTitle("Новая транзакция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amountText = dialogView.findViewById<EditText>(R.id.edit_amount).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.edit_description).text.toString()
                val isIncome = dialogView.findViewById<RadioButton>(R.id.radio_income).isChecked

                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val transaction = Transaction(
                        amount = amount,
                        categoryId = if (isIncome) 4 else 1, // Простые категории
                        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                        description = description.ifEmpty { if (isIncome) "Доход" else "Расход" }
                    )
                    repository.addTransaction(transaction)
                    updateAdapter()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить транзакцию?")
            .setMessage("${transaction.description} - ${transaction.amount} ₽")
            .setPositiveButton("Удалить") { _, _ ->
                repository.deleteTransaction(transaction)
                updateAdapter()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateStatistics(view: View, transactions: List<Transaction>) {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense

        val textStats = view.findViewById<TextView>(R.id.text_statistics)
        textStats.text = String.format(
            "Доходы: %.2f ₽\nРасходы: %.2f ₽\nБаланс: %.2f ₽",
            income, expense, balance
        )
    }

    private fun updateAdapter() {
        // Простое обновление — создаём новый адаптер
        val newAdapter = TransactionsAdapter(repository.transactions.value ?: emptyList()) { transaction ->
            showDeleteDialog(transaction)
        }
        recyclerView.adapter = newAdapter
    }
}