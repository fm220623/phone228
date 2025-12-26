package com.example.financemanager.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.financemanager.R
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.data.repository.SimpleFinanceRepository

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = SimpleFinanceRepository.getInstance(requireContext())
        val transactions = repository.transactions.value ?: emptyList()

        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense

        view.findViewById<TextView>(R.id.text_dashboard).text =
            "📊 Финансовая сводка\n\n" +
                    "Доходы: ${String.format("%.2f", income)} ₽\n" +
                    "Расходы: ${String.format("%.2f", expense)} ₽\n" +
                    "Баланс: ${String.format("%.2f", balance)} ₽\n\n" +
                    "Всего транзакций: ${transactions.size}"
    }
}