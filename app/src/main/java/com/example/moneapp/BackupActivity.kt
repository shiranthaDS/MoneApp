package com.example.moneapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class BackupActivity : AppCompatActivity() {

    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    private lateinit var btnBack: Button
    private val sharedPrefs by lazy { getSharedPreferences("FinanceTracker", MODE_PRIVATE) }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportDataToUri(it) }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importDataFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnBack = findViewById(R.id.btnBack)

        btnExport.setOnClickListener {
            initiateExport()
        }

        btnImport.setOnClickListener {
            initiateImport()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initiateExport() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "moneapp_backup_$timestamp.json"
        createDocumentLauncher.launch(filename)
    }

    private fun initiateImport() {
        // Show warning dialog before proceeding with import
        AlertDialog.Builder(this)
            .setTitle("Import Data")
            .setMessage("Importing data will replace your current transactions. This cannot be undone. Do you want to continue?")
            .setPositiveButton("Continue") { _, _ ->
                openDocumentLauncher.launch(arrayOf("application/json"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportDataToUri(uri: Uri) {
        try {
            val jsonData = createBackupJson()
            contentResolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
                FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
                    outputStream.write(jsonData.toString(2).toByteArray())
                    Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun importDataFromUri(uri: Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: throw Exception("Could not read file")

            restoreFromJson(jsonString)
            Toast.makeText(this, "Data imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing data: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun createBackupJson(): JSONObject {
        val jsonBackup = JSONObject()
        val currentBalance = sharedPrefs.getFloat("balance", 0f)
        jsonBackup.put("balance", currentBalance)

        val transactionsJson = sharedPrefs.getString("transactions", "") ?: ""
        val transactionsArray = JSONArray()

        if (transactionsJson.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            transactionsJson.split("|").forEach { transactionStr ->
                val parts = transactionStr.split(",")
                if (parts.size == 6) {
                    val transactionJson = JSONObject()
                    transactionJson.put("id", parts[0].toInt())
                    transactionJson.put("title", parts[1])
                    transactionJson.put("amount", parts[2].toDouble())
                    transactionJson.put("category", parts[3])
                    transactionJson.put("type", parts[4])
                    transactionJson.put("date", parts[5])
                    transactionsArray.put(transactionJson)
                }
            }
        }

        jsonBackup.put("transactions", transactionsArray)
        return jsonBackup
    }

    private fun restoreFromJson(jsonString: String) {
        try {
            val jsonBackup = JSONObject(jsonString)
            val editor = sharedPrefs.edit()

            // Restore balance
            val balance = jsonBackup.getDouble("balance").toFloat()
            editor.putFloat("balance", balance)

            // Restore transactions
            val transactionsArray = jsonBackup.getJSONArray("transactions")
            val transactionsList = mutableListOf<String>()

            for (i in 0 until transactionsArray.length()) {
                val transaction = transactionsArray.getJSONObject(i)
                val id = transaction.getInt("id")
                val title = transaction.getString("title")
                val amount = transaction.getDouble("amount")
                val category = transaction.getString("category")
                val type = transaction.getString("type")
                val date = transaction.getString("date")

                transactionsList.add("$id,$title,$amount,$category,$type,$date")
            }

            val transactionsString = transactionsList.joinToString("|")
            editor.putString("transactions", transactionsString)
            editor.apply()

            // Notify that data needs to be reloaded
            setResult(RESULT_OK)
        } catch (e: Exception) {
            throw Exception("Invalid backup file format")
        }
    }
}