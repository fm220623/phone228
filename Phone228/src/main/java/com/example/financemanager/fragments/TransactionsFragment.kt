package com.example.financemanager.fragments

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.widget.ImageButton
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financemanager.R
import com.example.financemanager.NotificationHelper
import com.example.financemanager.data.model.Transaction
import com.example.financemanager.data.model.TransactionType
import com.example.financemanager.data.repository.SimpleFinanceRepository
import com.example.financemanager.ui.transactions.TransactionsAdapter
import com.example.financemanager.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var repository: SimpleFinanceRepository
    private lateinit var adapter: TransactionsAdapter
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var recyclerView: RecyclerView // Объявляем переменную
    private var selectedPhotoUriString: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            ImageUtils.showToast(requireContext(), "Для загрузки фото нужны разрешения")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SimpleFinanceRepository.getInstance(requireContext())
        notificationHelper = NotificationHelper(requireContext())

        // Настройка RecyclerView - ИНИЦИАЛИЗИРУЕМ переменную
        recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Создаем адаптер
        adapter = TransactionsAdapter(
            repository.transactions.value ?: emptyList(),
            { transaction -> showDeleteDialog(transaction) },
            { transaction -> showTransactionDetails(transaction) }
        )
        recyclerView.adapter = adapter

        // Наблюдаем за данными
        repository.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.updateData(transactions)
            updateStatistics(view, transactions)
            checkBudgetsAfterTransaction(transactions)
        }

        // Кнопка добавления
        view.findViewById<Button>(R.id.btn_add_transaction).setOnClickListener {
            showAddTransactionDialog()
        }

        // ОБРАБОТКА "ПОДЕЛИТЬСЯ"
        arguments?.getString("shared_text")?.let { sharedText ->
            showSharedTextDialog(sharedText)
        }

        updateStatistics(view, repository.transactions.value ?: emptyList())
    }

    private fun showTransactionDetails(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_transaction_details, null)

        // Заполняем данные
        val textType = dialogView.findViewById<TextView>(R.id.text_type)
        val textCategory = dialogView.findViewById<TextView>(R.id.text_category)
        val textAmount = dialogView.findViewById<TextView>(R.id.text_amount)
        val textDate = dialogView.findViewById<TextView>(R.id.text_date)
        val textDescription = dialogView.findViewById<TextView>(R.id.text_description)
        val textPhotoTitle = dialogView.findViewById<TextView>(R.id.text_photo_title)
        val imagePhoto = dialogView.findViewById<ImageView>(R.id.image_photo)
        val textNoPhoto = dialogView.findViewById<TextView>(R.id.text_no_photo)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

        // Тип
        val typeText = if (transaction.type == TransactionType.INCOME) "Доход" else "Расход"
        val typeColor = if (transaction.type == TransactionType.INCOME) "#4CAF50" else "#F44336"
        textType.text = "Тип: $typeText"
        textType.setTextColor(Color.parseColor(typeColor))

        // Категория
        textCategory.text = "Категория: ${transaction.category}"

        // Сумма
        textAmount.text = "Сумма: ${transaction.amount} ₽"
        textAmount.setTextColor(Color.parseColor(typeColor))

        // Дата
        val date = Date(transaction.date)
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        textDate.text = "Дата: ${formatter.format(date)}"

        // Описание (без категории)
        val description = transaction.description.substringAfter(":").trim()
        textDescription.text = if (description.isNotEmpty()) description else "Нет описания"

        // Фото (только реальные фото из галереи)
        val hasPhoto = transaction.photoUri != null && transaction.photoUri!!.isNotEmpty()
        if (hasPhoto) {
            // Реальное фото из галереи
            val bitmap = ImageUtils.loadBitmapFromUri(requireContext(), transaction.photoUri)
            if (bitmap != null) {
                textPhotoTitle.visibility = View.VISIBLE
                imagePhoto.visibility = View.VISIBLE
                textNoPhoto.visibility = View.GONE
                imagePhoto.setImageBitmap(bitmap)
                textPhotoTitle.text = "Прикрепленное фото (нажмите для увеличения):"

                // ДОБАВЛЯЕМ КЛИК НА ФОТО
                imagePhoto.setOnClickListener {
                    showFullscreenPhoto(transaction.photoUri)
                }
            } else {
                textPhotoTitle.visibility = View.GONE
                imagePhoto.visibility = View.GONE
                textNoPhoto.visibility = View.VISIBLE
                textNoPhoto.text = "Не удалось загрузить фото"
            }
        } else {
            textPhotoTitle.visibility = View.GONE
            imagePhoto.visibility = View.GONE
            textNoPhoto.visibility = View.VISIBLE
            textNoPhoto.text = "Фото не прикреплено"
        }

        // Диалог
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_simple_transaction, null)

        // Настройка Spinner для категорий
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radio_group_type)
        val photoStatus = dialogView.findViewById<TextView>(R.id.text_photo_status)
        val choosePhotoBtn = dialogView.findViewById<Button>(R.id.btn_choose_photo)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.image_photo_preview)

        // Функция обновления статуса фото
        fun updatePhotoStatus() {
            if (selectedPhotoUriString != null) {
                photoStatus.text = "✓ Фото готово"
                photoStatus.setTextColor(Color.GREEN)

                // Показываем превью для реальных фото из галереи
                val bitmap = ImageUtils.loadBitmapFromUri(requireContext(), selectedPhotoUriString)
                bitmap?.let {
                    imagePreview.setImageBitmap(it)
                    imagePreview.visibility = View.VISIBLE
                }
            } else {
                photoStatus.text = "Нет фото"
                photoStatus.setTextColor(Color.GRAY)
                imagePreview.visibility = View.GONE
            }
        }

        // Обновляем статус при открытии
        updatePhotoStatus()

        // Обновляем категории при изменении типа транзакции
        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            updateCategoriesSpinner(spinnerCategory, checkedId == R.id.radio_income)
        }

        // Инициализация категорий
        updateCategoriesSpinner(spinnerCategory, false)

        // Кнопка для выбора фото из галереи (ЕДИНСТВЕННАЯ)
        choosePhotoBtn.setOnClickListener {
            checkPermissionsAndPickPhoto()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Новая транзакция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amountText = dialogView.findViewById<EditText>(R.id.edit_amount).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.edit_description).text.toString()
                val isIncome = dialogView.findViewById<RadioButton>(R.id.radio_income).isChecked
                val selectedCategory = spinnerCategory.selectedItem.toString()

                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull() ?: 0.0

                    val categoryId = getCategoryId(selectedCategory, isIncome)

                    val transaction = Transaction(
                        amount = amount,
                        category = selectedCategory,
                        categoryId = categoryId,
                        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                        description = if (description.isNotEmpty()) "$selectedCategory: $description"
                        else selectedCategory,
                        photoUri = selectedPhotoUriString
                    )

                    // ТОЛЬКО ОДИН РАЗ добавляем транзакцию - в репозиторий
                    repository.addTransaction(transaction)

                    // Прокручиваем к началу списка (новая транзакция будет наверху)
                    recyclerView.smoothScrollToPosition(0)

                    // Уведомление
                    notificationHelper.showTransactionNotification(transaction)

                    // Сообщение
                    ImageUtils.showToast(requireContext(), "Транзакция добавлена")

                    // Сбрасываем фото
                    selectedPhotoUriString = null
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                // При отмене сохраняем фото для следующей транзакции
            }
            .create()

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedPhotoUriString = uri.toString()
                ImageUtils.showToast(requireContext(), "Фото выбрано из галереи")

                // Обновляем статус фото в открытом диалоге
                val dialogView = requireView().findViewById<TextView>(R.id.text_photo_status)
                dialogView?.text = "✓ Фото готово"
                dialogView?.setTextColor(Color.GREEN)
            }
        }
    }

    private fun checkPermissionsAndPickPhoto() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            pickPhotoFromGallery()
        }
    }

    private fun pickPhotoFromGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        startActivityForResult(pickPhotoIntent, PICK_IMAGE_REQUEST)
    }

    private fun updateCategoriesSpinner(spinner: Spinner, isIncome: Boolean) {
        val categories = if (isIncome) {
            repository.incomeCategories
        } else {
            repository.expenseCategories
        }

        // ИСПРАВЛЕНИЕ: используем правильный ресурс для выпадающего списка
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun getCategoryId(categoryName: String, isIncome: Boolean): Long {
        return when {
            categoryName.contains("🍔") || categoryName.contains("Еда") -> 1L
            categoryName.contains("🚗") || categoryName.contains("Транспорт") -> 2L
            categoryName.contains("🏠") || categoryName.contains("Жилье") -> 3L
            categoryName.contains("🛍️") || categoryName.contains("Покупки") -> 4L
            categoryName.contains("🏥") || categoryName.contains("Здоровье") -> 5L
            categoryName.contains("🎉") || categoryName.contains("Развлечения") -> 6L
            categoryName.contains("💰") || categoryName.contains("Зарплата") -> 10L
            categoryName.contains("💼") && categoryName.contains("Фриланс") -> 11L
            categoryName.contains("📈") || categoryName.contains("Инвестиции") -> 12L
            categoryName.contains("🎁") || categoryName.contains("Подарок") -> 13L
            else -> if (isIncome) 15L else 9L
        }
    }

    private fun checkBudgetsAfterTransaction(transactions: List<Transaction>) {
        try {
            // Получаем установленные бюджеты
            val prefs = requireContext().getSharedPreferences("budgets", 0)
            val allEntries = prefs.all

            // Для каждого бюджета проверяем расходы
            allEntries.forEach { (categoryKey, limitObj) ->
                val limit = (limitObj as? Float)?.toDouble() ?: 0.0

                if (limit > 0) {
                    // Убираем emoji из ключа для сравнения
                    val cleanCategoryKey = categoryKey
                        .replace("🍔", "").replace("🚗", "").replace("🏠", "").replace("🛍️", "")
                        .replace("🏥", "").replace("🎉", "").replace("📚", "").replace("💼", "")
                        .replace("💰", "").replace("📈", "").replace("🎁", "").replace("💸", "")
                        .replace("💡", "").replace("⚡", "").trim()

                    // Считаем расходы по этой категории
                    val categoryExpense = transactions
                        .filter { transaction ->
                            try {
                                val isExpense = transaction.type == TransactionType.EXPENSE
                                val cleanTransactionCategory = transaction.category
                                    .replace("🍔", "").replace("🚗", "").replace("🏠", "").replace("🛍️", "")
                                    .replace("🏥", "").replace("🎉", "").replace("📚", "").replace("💼", "")
                                    .replace("💰", "").replace("📈", "").replace("🎁", "").replace("💸", "")
                                    .replace("💡", "").replace("⚡", "").trim()

                                isExpense && cleanTransactionCategory.contains(cleanCategoryKey, ignoreCase = true)
                            } catch (e: Exception) {
                                false // Если ошибка - пропускаем транзакцию
                            }
                        }
                        .sumOf { it.amount }

                    // Проверяем лимиты
                    if (categoryExpense >= limit) {
                        // Превышен лимит
                        notificationHelper.showBudgetNotification(
                            categoryKey,
                            categoryExpense,
                            limit,
                            true
                        )
                    } else if (categoryExpense >= limit * 0.8) {
                        // Достигнут 80% лимита
                        notificationHelper.showBudgetNotification(
                            categoryKey,
                            categoryExpense,
                            limit,
                            false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Логируем ошибку, но не крашим приложение
            println("ERROR in checkBudgets: ${e.message}")
        }
    }

    private fun showSharedTextDialog(sharedText: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Создать транзакцию из текста?")
            .setMessage("Текст: $sharedText")
            .setPositiveButton("Создать") { _, _ ->
                val regex = "\\d+(\\.\\d+)?".toRegex()
                val amounts = regex.findAll(sharedText)
                    .map { it.value.toDoubleOrNull() }
                    .filterNotNull()
                    .toList()

                if (amounts.isNotEmpty()) {
                    val amount = amounts.first()
                    val transaction = Transaction(
                        amount = amount,
                        category = "⚡ Прочее",
                        categoryId = 1,
                        type = TransactionType.EXPENSE,
                        description = "⚡ Прочее: $sharedText"
                    )

                    // ТОЛЬКО добавляем в репозиторий
                    repository.addTransaction(transaction)

                    // Прокручиваем к началу
                    recyclerView.smoothScrollToPosition(0)

                    // ПОКАЗАТЬ УВЕДОМЛЕНИЕ
                    notificationHelper.showTransactionNotification(transaction)
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
                ImageUtils.showToast(requireContext(), "Транзакция удалена")
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

    // НОВЫЙ МЕТОД: Показ фото на весь экран (упрощенный)
    private fun showFullscreenPhoto(uriString: String?) {
        try {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_fullscreen_photo, null)

            val fullscreenImage = dialogView.findViewById<ImageView>(R.id.image_fullscreen)
            val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_fullscreen)

            // Загружаем изображение из галереи
            if (uriString != null) {
                val bitmap = ImageUtils.loadBitmapFromUri(requireContext(), uriString)
                if (bitmap != null) {
                    fullscreenImage.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(requireContext(), "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Создаем диалог
            val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(dialogView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

            // Кнопка закрытия
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Клик по самому изображению тоже закрывает
            fullscreenImage.setOnClickListener {
                dialog.dismiss()
            }

            // Закрытие по кнопке назад
            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dialog.dismiss()
                    return@setOnKeyListener true
                }
                false
            }

            dialog.show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка открытия фото: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}