package com.example.moneapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AnalysisActivity : AppCompatActivity() {

    private lateinit var spMonthSelector: Spinner
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var rvCategorySummary: RecyclerView
    private lateinit var btnBackToMain: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var incomeCategorySummaryAdapter: CategorySummaryAdapter
    private lateinit var expenseCategorySummaryAdapter: CategorySummaryAdapter

    private var currentMonthIndex = 0
    private val transactions = mutableListOf<Transaction>()
    private val incomeCategories = mutableListOf<CategorySummary>()
    private val expenseCategories = mutableListOf<CategorySummary>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_activity)

        initializeViews()
        setupMonthSelector()
        setupTabLayout()
        setupRecyclerView()
        setupBottomNavigation()

        loadTransactions()

        val calendar = Calendar.getInstance()
        currentMonthIndex = calendar.get(Calendar.MONTH)
        spMonthSelector.setSelection(currentMonthIndex)

        processTransactionsForMonth(currentMonthIndex)

        btnBackToMain.setOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        spMonthSelector = findViewById(R.id.spMonthSelector)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tabLayout = findViewById(R.id.tabLayout)
        rvCategorySummary = findViewById(R.id.rvCategorySummary)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btnBackToMain -> {
                    // Navigate back to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.btnAnalysis -> {
                    // Already on Analysis screen
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.btnBackup -> {
                    val intent = Intent(this, BackupActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the analysis item as selected by default
        bottomNavigation.selectedItemId = R.id.btnAnalysis
    }

    private fun setupMonthSelector() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMonthSelector.adapter = adapter

        spMonthSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentMonthIndex = position
                processTransactionsForMonth(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Income"))
        tabLayout.addTab(tabLayout.newTab().setText("Expenses"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                rvCategorySummary.adapter = if (tab?.position == 0)
                    incomeCategorySummaryAdapter
                else
                    expenseCategorySummaryAdapter
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        rvCategorySummary.layoutManager = LinearLayoutManager(this)
        incomeCategorySummaryAdapter = CategorySummaryAdapter(incomeCategories, true)
        expenseCategorySummaryAdapter = CategorySummaryAdapter(expenseCategories, false)
        rvCategorySummary.adapter = incomeCategorySummaryAdapter
    }

    private fun loadTransactions() {
        val sharedPrefs = getSharedPreferences("FinanceTracker", MODE_PRIVATE)
        val transactionsJson = sharedPrefs.getString("transactions", "")

        if (!transactionsJson.isNullOrEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            transactions.clear()
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

    private fun processTransactionsForMonth(monthIndex: Int) {
        incomeCategories.clear()
        expenseCategories.clear()
        var totalIncome = 0.0
        var totalExpense = 0.0
        val calendar = Calendar.getInstance()

        transactions.forEach { transaction ->
            calendar.time = transaction.date
            if (calendar.get(Calendar.MONTH) == monthIndex) {
                if (transaction.type == "Income") {
                    totalIncome += transaction.amount
                    updateCategorySummary(incomeCategories, transaction.category, transaction.amount)
                } else {
                    totalExpense += transaction.amount
                    updateCategorySummary(expenseCategories, transaction.category, transaction.amount)
                }
            }
        }

        tvTotalIncome.text = String.format(Locale.getDefault(), "$%.2f", totalIncome)
        tvTotalExpense.text = String.format(Locale.getDefault(), "$%.2f", totalExpense)

        incomeCategories.sortByDescending { it.amount }
        expenseCategories.sortByDescending { it.amount }

        incomeCategorySummaryAdapter.updateCategories(incomeCategories)
        expenseCategorySummaryAdapter.updateCategories(expenseCategories)
    }

    private fun updateCategorySummary(categories: MutableList<CategorySummary>, category: String, amount: Double) {
        val existingCategory = categories.find { it.category == category }
        if (existingCategory != null) {
            existingCategory.amount += amount
        } else {
            categories.add(CategorySummary(category, amount))
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure the bottom navigation shows the correct selected item
        bottomNavigation.selectedItemId = R.id.btnAnalysis
    }
}