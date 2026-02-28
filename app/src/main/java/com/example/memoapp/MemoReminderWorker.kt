package com.example.memoapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class MemoReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "memo_reminder_channel"
        const val EXTRA_MEMO_TITLE = "extra_memo_title"
        const val EXTRA_MEMO_CONTENT = "extra_memo_content"
        const val EXTRA_MEMO_ID = "extra_memo_id"
    }
    
    override fun doWork(): Result {
        try {
            // 获取传递的参数
            val title = inputData.getString(EXTRA_MEMO_TITLE) ?: "备忘录提醒"
            val content = inputData.getString(EXTRA_MEMO_CONTENT) ?: ""
            val memoId = inputData.getString(EXTRA_MEMO_ID) ?: ""
            
            createNotificationChannel(applicationContext)
            showNotification(applicationContext, title, content, memoId)
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "备忘录提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "备忘录应用的提醒通知"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(context: Context, title: String, content: String, memoId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add) // 使用添加图标作为通知图标
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "立即查看", pendingIntent) // 添加操作按钮
        
        with(NotificationManagerCompat.from(context)) {
            notify(memoId.hashCode(), builder.build())
        }
    }
}