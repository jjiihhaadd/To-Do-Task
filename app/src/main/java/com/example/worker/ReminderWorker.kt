package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task Reminder"
        showNotification(taskTitle)
        return Result.success()
    }

    private fun showNotification(taskTitle: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Task Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskTitle.hashCode(), notification)
    }

    companion object {
        const val KEY_TASK_TITLE = "KEY_TASK_TITLE"
        private const val CHANNEL_ID = "task_reminders_channel"

        fun scheduleReminder(context: Context, taskTitle: String, delayMinutes: Long) {
            val data = androidx.work.Data.Builder()
                .putString(KEY_TASK_TITLE, taskTitle)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun scheduleReminderAtTime(context: Context, taskTitle: String, targetTimeMillis: Long) {
            val delayMillis = targetTimeMillis - System.currentTimeMillis()
            if (delayMillis <= 0) return

            val data = androidx.work.Data.Builder()
                .putString(KEY_TASK_TITLE, taskTitle)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
