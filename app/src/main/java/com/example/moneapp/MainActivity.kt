package com.example.moneapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvBalance: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var btnAnalysis: Button
    private lateinit var btnAddIncome: Button
    private lateinit var btnAddExpense: Button
    private lateinit var btnBudget: Button
    private lateinit var btnBackup: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var transactionAdapter: TransactionAdapter

    private val transactions = mutableListOf<Transaction>()
    private var currentBalance = 0.0
    private val sharedPrefs by lazy { getSharedPreferences("FinanceTracker", MODE_PRIVATE) }

    private val backupActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload data if import was successful
            loadData()
            updateBalance()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadData()
        updateBalance()

        btnAddIncome.setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }

        btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        btnAnalysis.setOnClickListener {
            val intent = Intent(this, AnalysisActivity::class.java)
            startActivity(intent)
        }

        btnBudget.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
        }

        // Setup Backup button click listener
        btnBackup.setOnClickListener {
            val intent = Intent(this, BackupActivity::class.java)
            backupActivityLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        updateBalance()
    }

    private fun initializeViews() {
        tvBalance = findViewById(R.id.tvBalance)
        rvTransactions = findViewById(R.id.rvTransactions)
        btnAnalysis = findViewById(R.id.btnAnalysis)
        btnAddIncome = findViewById(R.id.btnAddIncome)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnBudget = findViewById(R.id.btnBudget)
        btnBackup = findViewById(R.id.btnBackup)

        // Initialize Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnBackToMain-> {
                    // Already on home screen
                    true
                }
                R.id.btnAnalysis-> {
                    // For now, we'll just direct to main activity until you create TransactionsActivity
                    // uncomment this when you have the activity:
                     startActivity(Intent(this, AnalysisActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.btnBackup -> {
                    // For now, we'll just direct to main activity until you create ProfileActivity
                    // uncomment this when you have the activity:
                     startActivity(Intent(this, BackupActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Set the home item as selected by default
        bottomNavigation.selectedItemId = R.id.btnBackToMain
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            transactions,
            onEditClick = { transaction ->
                if (transaction.type == "Income") {
                    val intent = Intent(this, AddIncomeActivity::class.java)
                    intent.putExtra("TRANSACTION_ID", transaction.id)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, AddExpenseActivity::class.java)
                    intent.putExtra("TRANSACTION_ID", transaction.id)
                    startActivity(intent)
                }
            },
            onDeleteClick = { transaction -> showDeleteConfirmation(transaction) }
        )
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ -> deleteTransaction(transaction) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        updateBalanceForTransaction(transaction, false)
        transactions.remove(transaction)
        transactionAdapter.notifyDataSetChanged()
        saveData()
    }

    private fun updateBalanceForTransaction(transaction: Transaction, isAdd: Boolean) {
        val amount = if (isAdd) transaction.amount else -transaction.amount
        currentBalance += if (transaction.type == "Income") amount else -amount
        updateBalance()
    }

    private fun updateBalance() {
        tvBalance.text = String.format(Locale.getDefault(), "$%.2f", currentBalance)
        tvBalance.setTextColor(
            when {
                currentBalance > 0 -> getColor(android.R.color.holo_green_dark)
                currentBalance < 0 -> getColor(android.R.color.holo_red_dark)
                else -> getColor(android.R.color.black)
            }
        )
    }

    fun saveData() {
        val editor = sharedPrefs.edit()
        editor.putFloat("balance", currentBalance.toFloat())

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val transactionsJson = transactions.joinToString("|") {
            "${it.id},${it.title},${it.amount},${it.category},${it.type},${dateFormat.format(it.date)}"
        }
        editor.putString("transactions", transactionsJson)
        editor.apply()
    }

    private fun loadData() {
        currentBalance = sharedPrefs.getFloat("balance", 0f).toDouble()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val transactionsJson = sharedPrefs.getString("transactions", "")

        transactions.clear()
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
        transactionAdapter.notifyDataSetChanged()
    }

    companion object {
        fun getNextTransactionId(transactions: List<Transaction>): Int {
            return if (transactions.isEmpty()) 1 else transactions.maxOf { it.id } + 1
        }
    }
}