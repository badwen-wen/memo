package com.example.memoapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.*

class MemoWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_UPDATE_MEMOS = "com.example.memoapp.UPDATE_MEMOS"
    }
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        
        if (intent?.action == ACTION_UPDATE_MEMOS && context != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, MemoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
    
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // 创建RemoteViews对象
        val views = RemoteViews(context.packageName, R.layout.widget_memo_list)
        
        // 设置标题
        views.setTextViewText(R.id.widget_title, "备忘录")
        
        // 设置添加按钮的点击事件
        val addIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            addIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_widget, pendingIntent)
        
        // 加载备忘录数据并显示前几条
        val memoManager = MemoManager(context)
        val memos = memoManager.loadMemos()
        
        // 显示最近的3个未完成的备忘录
        val recentMemos = memos.filter { !it.isCompleted }.take(3)
        
        var memoText = StringBuilder()
        if (recentMemos.isEmpty()) {
            memoText.append("暂无待办事项")
        } else {
            for ((index, memo) in recentMemos.withIndex()) {
                if (index > 0) memoText.append("\n\n")
                memoText.append("${memo.title}")
                if (memo.content.length > 50) {
                    memoText.append("\n${memo.content.substring(0, 50)}...")
                } else {
                    memoText.append("\n${memo.content}")
                }
            }
        }
        
        views.setTextViewText(R.id.list_view_memos, memoText.toString())
        
        // 更新小部件
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}