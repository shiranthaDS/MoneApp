package com.example.moneapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.graphics.Color

class BudgetNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "budget_alerts_channel"
        private const val NOTIFICATION_GROUP = "budget_alerts_group"

        // Notification IDs
        const val APPROACHING_BUDGET_NOTIFICATION_ID = 101
        const val EXCEEDED_BUDGET_NOTIFICATION_ID = 102
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Alerts"
            val descriptionText = "Notifications for budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showApproachingBudgetNotification(budget: Budget) {
        val intent = Intent(context, BudgetActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_warning)
            .setContentTitle("Budget Alert")
            .setContentText("You're approaching your ${budget.category} budget limit (${budget.percentSpent}%)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've spent $${String.format("%.2f", budget.spent)} of your $${String.format("%.2f", budget.amount)} ${budget.category} budget."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(Color.parseColor("#FFA500"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(APPROACHING_BUDGET_NOTIFICATION_ID + budget.category.hashCode(), notification)
            } catch (e: SecurityException) {
                // Handle case where notification permission is not granted
            }
        }
    }

    fun showExceededBudgetNotification(budget: Budget) {
        val intent = Intent(context, BudgetActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_alert)
            .setContentTitle("Budget Exceeded!")
            .setContentText("You've exceeded your ${budget.category} budget")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've spent $${String.format("%.2f", budget.spent)} of your $${String.format("%.2f", budget.amount)} ${budget.category} budget (${budget.percentSpent}%)."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.RED)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(EXCEEDED_BUDGET_NOTIFICATION_ID + budget.category.hashCode(), notification)
            } catch (e: SecurityException) {
                // Handle case where notification permission is not granted
            }
        }
    }

    fun showOverallBudgetNotification(percentSpent: Int, totalSpent: Double, totalBudget: Double) {
        val intent = Intent(context, BudgetActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val color: Int

        when {
            percentSpent >= 100 -> {
                title = "Overall Budget Exceeded!"
                color = Color.RED
            }
            percentSpent >= 80 -> {
                title = "Approaching Overall Budget Limit"
                color = Color.parseColor("#FFA500")
            }
            else -> return // Don't show notification if below 80%
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (percentSpent >= 100) R.drawable.ic_budget_alert else R.drawable.ic_budget_warning)
            .setContentTitle(title)
            .setContentText("You've spent $${String.format("%.2f", totalSpent)} of your $${String.format("%.2f", totalBudget)} total budget")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've spent $${String.format("%.2f", totalSpent)} of your $${String.format("%.2f", totalBudget)} total budget (${percentSpent}%)."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(color)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(if (percentSpent >= 100) EXCEEDED_BUDGET_NOTIFICATION_ID else APPROACHING_BUDGET_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Handle case where notification permission is not granted
            }
        }
    }
}