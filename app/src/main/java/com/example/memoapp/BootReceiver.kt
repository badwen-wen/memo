package com.example.memoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // 重启后重新安排所有待处理的提醒
            scheduleAllReminders(context)
        }
    }
    
    private fun scheduleAllReminders(context: Context) {
        val memoManager = MemoManager(context)
        val memos = memoManager.loadMemos()
        
        for (memo in memos) {
            if (memo.reminderTime != null && memo.reminderTime > System.currentTimeMillis()) {
                // 计算剩余时间
                val delayInMillis = memo.reminderTime - System.currentTimeMillis()
                
                if (delayInMillis > 0) {
                    // 使用WorkManager安排提醒
                    val workRequest = OneTimeWorkRequest.Builder(MemoReminderWorker::class.java)
                        .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                        .build()
                    
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "reminder_${memo.id}",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                }
            }
        }
    }
}