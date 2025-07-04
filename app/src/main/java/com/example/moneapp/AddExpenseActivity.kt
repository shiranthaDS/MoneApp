package com.example.moneapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnDatePicker: Button
    private lateinit var btnSaveExpense: Button
    private lateinit var btnCancel: Button

    private var selectedDate = Calendar.getInstance()
    private var editingTransactionId: Int? = null
    private val sharedPrefs by lazy { getSharedPreferences("FinanceTracker", MODE_PRIVATE) }
    private val transactions = mutableListOf<Transaction>()
    private var currentBalance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        initializeViews()
        setupSpinner()
        setupDatePicker()
        loadData()

        // Check if we're editing an existing transaction
        editingTransactionId = intent.getIntExtra("TRANSACTION_ID", -1)
        if (editingTransactionId != -1) {
            loadTransactionData()
        }

        btnSaveExpense.setOnClickListener {
            saveExpense()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        spCategory = findViewById(R.id.spCategory)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupSpinner() {
        val expenseCategories = arrayOf("Food", "Transportation", "Entertainment", "Housing",
            "Utilities", "Shopping", "Health", "Education", "Other")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, expenseCategories)
        spCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        updateDateButtonText()
        btnDatePicker.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateButtonText()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtonText() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        btnDatePicker.text = dateFormat.format(selectedDate.time)
    }

    private fun loadTransactionData() {
        val transactionId = editingTransactionId ?: return
        val transaction = transactions.find { it.id == transactionId } ?: return

        if (transaction.type != "Expense") {
            Toast.makeText(this, "Cannot edit an income as expense", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etTitle.setText(transaction.title)
        etAmount.setText(transaction.amount.toString())
        selectedDate.time = transaction.date
        updateDateButtonText()

        // Find position of category in spinner
        val adapter = spCategory.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(transaction.category)
        if (position >= 0) {
            spCategory.setSelection(position)
        }

        btnSaveExpense.text = "Update Expense"
    }

    private fun saveExpense() {
        val title = etTitle.text.toString().trim()
        val amountText = etAmount.text.toString().trim()
        val category = spCategory.selectedItem.toString()

        if (title.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val isEditing = editingTransactionId != -1

        if (isEditing) {
            // Update existing transaction
            val transaction = transactions.find { it.id == editingTransactionId }
            if (transaction != null) {
                // Revert old transaction effect on balance
                updateBalanceForTransaction(transaction, false)

                // Update transaction
                transaction.title = title
                transaction.amount = amount
                transaction.category = category
                transaction.date = selectedDate.time

                // Apply new transaction effect on balance
                updateBalanceForTransaction(transaction, true)
            }
        } else {
            // Create new transaction
            val transaction = Transaction(
                id = MainActivity.getNextTransactionId(transactions),
                title = title,
                amount = amount,
                category = category,
                type = "Expense",
                date = selectedDate.time
            )

            transactions.add(transaction)
            updateBalanceForTransaction(transaction, true)
        }

        saveData()
        Toast.makeText(this, if (isEditing) "Expense updated" else "Expense added", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateBalanceForTransaction(transaction: Transaction, isAdd: Boolean) {
        val amount = if (isAdd) transaction.amount else -transaction.amount
        currentBalance += if (transaction.type == "Income") amount else -amount
    }

    private fun loadData() {
        currentBalance = sharedPrefs.getFloat("balance", 0f).toDouble()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val transactionsJson = sharedPrefs.getString("transactions", "")

        if (!transactionsJson.isNullOrEmpty()) {
            transactionsJson.split("|").forEach { transactionStr ->
                val parts = transactionStr.split(",")
                if (parts.size == 6) {
                    val date = try {
                        dateFormat.parse(parts[5]) ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    transactions.add(
                        Transaction(
                            id = parts[0].toInt(),
                            title = parts[1],
                            amount = parts[2].toDouble(),
                            category = parts[3],
                            type = parts[4],
                            date = date
                        )
                    )
                }
            }
        }
    }

    private fun saveData() {
        val editor = sharedPrefs.edit()
        editor.putFloat("balance", currentBalance.toFloat())

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val transactionsJson = transactions.joinToString("|") {
            "${it.id},${it.title},${it.amount},${it.category},${it.type},${dateFormat.format(it.date)}"
        }
        editor.putString("transactions", transactionsJson)
        editor.apply()
    }
}