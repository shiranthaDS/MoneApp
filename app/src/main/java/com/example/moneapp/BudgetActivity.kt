package com.example.moneapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var etBudgetAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSetBudget: Button
    private lateinit var rvBudgets: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvTotalBudget: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvStatus: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private val budgets = mutableListOf<Budget>()
    private lateinit var budgetAdapter: BudgetAdapter
    private val sharedPrefs by lazy { getSharedPreferences("FinanceTracker", MODE_PRIVATE) }

    // Notification manager
    private lateinit var notificationManager: BudgetNotificationManager

    // Budget notification states to avoid repeated notifications
    private val notificationStates = mutableMapOf<String, Boolean>()

    // Default categories - you can expand this list
    private val categories = listOf(
        "Food", "Transportation", "Entertainment", "Housing",
        "Utilities", "Shopping", "Health", "Education", "Other"
    )

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Budget alerts require notification permission", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Initialize notification manager
        notificationManager = BudgetNotificationManager(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        initializeViews()
        setupSpinner()
        setupRecyclerView()
        setupBottomNavigation()
        loadBudgets()
        loadNotificationStates()
        updateMonthYearDisplay()
        calculateAndUpdateProgress()

        btnSetBudget.setOnClickListener {
            saveBudget()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale dialog
                    AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("Budget alerts require notification permission to inform you when you're approaching or exceeding your budget limits.")
                        .setPositiveButton("Request Permission") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Maybe Later", null)
                        .show()
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun initializeViews() {
        etBudgetAmount = findViewById(R.id.etBudgetAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        rvBudgets = findViewById(R.id.rvBudgets)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvTotalBudget = findViewById(R.id.tvTotalBudget)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        progressBar = findViewById(R.id.progressBar)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvStatus = findViewById(R.id.tvStatus)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnBackToMain -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.btnAnalysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    // Already on Budget screen
                    true
                }
                R.id.btnBackup -> {
                    startActivity(Intent(this, BackupActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Set the budget item as selected by default
        bottomNavigation.selectedItemId = R.id.nav_budget
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(
            budgets,
            onEditClick = { budget -> showEditBudgetDialog(budget) },
            onDeleteClick = { budget -> showDeleteConfirmation(budget) }
        )
        rvBudgets.layoutManager = LinearLayoutManager(this)
        rvBudgets.adapter = budgetAdapter
    }

    private fun saveBudget() {
        val amountText = etBudgetAmount.text.toString()
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = spinnerCategory.selectedItem.toString()
        val currentDate = Calendar.getInstance()
        val month = currentDate.get(Calendar.MONTH)
        val year = currentDate.get(Calendar.YEAR)

        // Check if budget for this category already exists for current month
        val existingBudgetIndex = budgets.indexOfFirst {
            it.category == category && it.month == month && it.year == year
        }

        if (existingBudgetIndex != -1) {
            // Update existing budget
            budgets[existingBudgetIndex].amount = amount
            // Reset notification state for this category
            resetNotificationState(budgets[existingBudgetIndex])
            Toast.makeText(this, "Budget updated for $category", Toast.LENGTH_SHORT).show()
        } else {
            // Create new budget
            val newBudget = Budget(
                category = category,
                amount = amount,
                month = month,
                year = year
            )
            budgets.add(newBudget)
            // Initialize notification state
            initNotificationState(newBudget)
            Toast.makeText(this, "Budget set for $category", Toast.LENGTH_SHORT).show()
        }

        etBudgetAmount.setText("")
        saveBudgetsToPrefs()
        saveNotificationStates()
        budgetAdapter.notifyDataSetChanged()
        calculateAndUpdateProgress()
    }

    private fun showEditBudgetDialog(budget: Budget) {
        val editText = EditText(this)
        editText.setText(budget.amount.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Budget for ${budget.category}")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newAmount = editText.text.toString().toDoubleOrNull()
                if (newAmount != null && newAmount > 0) {
                    budget.amount = newAmount
                    // Reset notification state when budget amount changes
                    resetNotificationState(budget)
                    saveBudgetsToPrefs()
                    saveNotificationStates()
                    budgetAdapter.notifyDataSetChanged()
                    calculateAndUpdateProgress()
                    Toast.makeText(this, "Budget updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(budget: Budget) {
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete the budget for ${budget.category}?")
            .setPositiveButton("Delete") { _, _ ->
                budgets.remove(budget)
                // Remove notification state
                removeNotificationState(budget)
                saveBudgetsToPrefs()
                saveNotificationStates()
                budgetAdapter.notifyDataSetChanged()
                calculateAndUpdateProgress()
                Toast.makeText(this, "Budget deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBudgetsToPrefs() {
        val editor = sharedPrefs.edit()
        val budgetsJson = budgets.joinToString("|") {
            "${it.category},${it.amount},${it.month},${it.year},${it.spent}"
        }
        editor.putString("budgets", budgetsJson)
        editor.apply()
    }

    private fun loadBudgets() {
        val budgetsJson = sharedPrefs.getString("budgets", "")
        budgets.clear()

        if (!budgetsJson.isNullOrEmpty()) {
            budgetsJson.split("|").forEach { budgetStr ->
                val parts = budgetStr.split(",")
                if (parts.size >= 4) {
                    budgets.add(
                        Budget(
                            category = parts[0],
                            amount = parts[1].toDouble(),
                            month = parts[2].toInt(),
                            year = parts[3].toInt(),
                            spent = if (parts.size >= 5) parts[4].toDouble() else 0.0
                        )
                    )
                }
            }
        }

        // Filter budgets for current month and year
        val currentDate = Calendar.getInstance()
        val currentMonth = currentDate.get(Calendar.MONTH)
        val currentYear = currentDate.get(Calendar.YEAR)

        val filteredBudgets = budgets.filter {
            it.month == currentMonth && it.year == currentYear
        }.toMutableList()

        budgets.clear()
        budgets.addAll(filteredBudgets)

        // Initialize notification states for all budgets
        budgets.forEach { initNotificationState(it) }

        budgetAdapter.notifyDataSetChanged()
    }

    // Notification state management
    private fun getNotificationStateKey(budget: Budget): String {
        return "${budget.category}_${budget.month}_${budget.year}"
    }

    private fun initNotificationState(budget: Budget) {
        val key = getNotificationStateKey(budget)
        if (!notificationStates.containsKey(key)) {
            notificationStates[key] = false // Not notified yet
        }
    }

    private fun resetNotificationState(budget: Budget) {
        val key = getNotificationStateKey(budget)
        notificationStates[key] = false // Reset notification state
    }

    private fun removeNotificationState(budget: Budget) {
        val key = getNotificationStateKey(budget)
        notificationStates.remove(key)
    }

    private fun markAsNotified(budget: Budget) {
        val key = getNotificationStateKey(budget)
        notificationStates[key] = true
    }

    private fun hasBeenNotified(budget: Budget): Boolean {
        val key = getNotificationStateKey(budget)
        return notificationStates[key] ?: false
    }

    private fun saveNotificationStates() {
        val editor = sharedPrefs.edit()
        val states = notificationStates.entries.joinToString("|") {
            "${it.key},${it.value}"
        }
        editor.putString("notification_states", states)
        editor.apply()
    }

    private fun loadNotificationStates() {
        val states = sharedPrefs.getString("notification_states", "")
        notificationStates.clear()

        if (!states.isNullOrEmpty()) {
            states.split("|").forEach { stateStr ->
                val parts = stateStr.split(",")
                if (parts.size == 2) {
                    notificationStates[parts[0]] = parts[1].toBoolean()
                }
            }
        }
    }

    private fun updateMonthYearDisplay() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = dateFormat.format(calendar.time)
    }

    private fun calculateAndUpdateProgress() {
        val currentDate = Calendar.getInstance()
        val currentMonth = currentDate.get(Calendar.MONTH)
        val currentYear = currentDate.get(Calendar.YEAR)

        // Calculate total budget for current month
        val totalBudget = budgets.sumOf { it.amount }

        // Load transactions
        val transactions = loadTransactions()

        // Filter expenses for current month and calculate total spent
        val monthlyExpenses = transactions.filter {
            val calendar = Calendar.getInstance()
            calendar.time = it.date
            it.type == "Expense" &&
                    calendar.get(Calendar.MONTH) == currentMonth &&
                    calendar.get(Calendar.YEAR) == currentYear
        }

        val totalSpent = monthlyExpenses.sumOf { it.amount }

        // Calculate spending by category
        val categorySpending = mutableMapOf<String, Double>()
        monthlyExpenses.forEach {
            categorySpending[it.category] = (categorySpending[it.category] ?: 0.0) + it.amount
        }

        // Update category spending in budgets and check for notifications
        budgets.forEach { budget ->
            val previousSpent = budget.spent
            budget.spent = categorySpending[budget.category] ?: 0.0

            // Check if we should show notifications for this budget
            checkBudgetNotifications(budget, previousSpent)
        }

        // Update UI
        tvTotalBudget.text = String.format(Locale.getDefault(), "$%.2f", totalBudget)
        tvTotalSpent.text = String.format(Locale.getDefault(), "$%.2f", totalSpent)

        // Update progress
        val progress = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
        progressBar.progress = progress.coerceAtMost(100)
        tvProgressPercent.text = "$progress%"

        // Update status
        when {
            totalBudget == 0.0 -> {
                tvStatus.text = "No budget set"
                tvStatus.setTextColor(Color.GRAY)
            }
            progress >= 100 -> {
                tvStatus.text = "BUDGET EXCEEDED!"
                tvStatus.setTextColor(Color.RED)
            }
            progress >= 80 -> {
                tvStatus.text = "WARNING: Near budget limit"
                tvStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
            }
            else -> {
                tvStatus.text = "On track"
                tvStatus.setTextColor(Color.GREEN)
            }
        }

        // Check for overall budget notification
        if (totalBudget > 0) {
            notificationManager.showOverallBudgetNotification(progress, totalSpent, totalBudget)
        }

        budgetAdapter.notifyDataSetChanged()
        saveBudgetsToPrefs()
        saveNotificationStates()
    }

    private fun checkBudgetNotifications(budget: Budget, previousSpent: Double) {
        if (budget.amount <= 0) return

        // Don't send notifications if already notified for this budget
        if (hasBeenNotified(budget)) return

        // Check if we've crossed thresholds that should trigger notifications
        if (budget.isExceeded) {
            notificationManager.showExceededBudgetNotification(budget)
            markAsNotified(budget)
        }
        // Check if we've crossed the 80% threshold with this update
        else if (budget.isNearLimit &&
            previousSpent < (budget.amount * 0.8) &&
            budget.spent >= (budget.amount * 0.8)) {
            notificationManager.showApproachingBudgetNotification(budget)
            markAsNotified(budget)
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
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
        return transactions
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        loadBudgets()
        calculateAndUpdateProgress()
        // Make sure the bottom navigation shows the correct selected item
        bottomNavigation.selectedItemId = R.id.nav_budget
    }
}