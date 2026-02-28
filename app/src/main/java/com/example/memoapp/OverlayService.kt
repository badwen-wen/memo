package com.example.memoapp

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class OverlayService : Service() {
    
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    
    companion object {
        const val EXTRA_MEMO_TITLE = "extra_memo_title"
        const val EXTRA_MEMO_CONTENT = "extra_memo_content"
        const val EXTRA_REPEAT_COUNT = "extra_repeat_count"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlayView(
            intent?.getStringExtra(EXTRA_MEMO_TITLE) ?: "重要提醒",
            intent?.getStringExtra(EXTRA_MEMO_CONTENT) ?: "",
            intent?.getIntExtra(EXTRA_REPEAT_COUNT, 3) ?: 3
        )
        
        return START_NOT_STICKY // 不重新启动服务
    }
    
    private fun showOverlayView(title: String, content: String, repeatCount: Int) {
        if (overlayView != null) return // 防止重复显示
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or 
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or 
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
        }
        
        // 创建覆盖视图
        overlayView = createOverlayLayout(title, content, repeatCount)
        
        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createOverlayLayout(title: String, content: String, repeatCount: Int): View {
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@OverlayService, android.R.color.black))
            alpha = 0.9f
        }
        
        val titleTextView = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@OverlayService, android.R.color.white))
            gravity = Gravity.CENTER
            setPadding(50, 100, 50, 20)
        }
        
        val contentTextView = TextView(this).apply {
            text = content
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@OverlayService, android.R.color.white))
            gravity = Gravity.CENTER
            setPadding(50, 20, 50, 20)
        }
        
        val countTextView = TextView(this).apply {
            text = "此提醒将重复 $repeatCount 次"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@OverlayService, android.R.color.darker_gray))
            gravity = Gravity.CENTER
            setPadding(50, 20, 50, 50)
        }
        
        val dismissButton = Button(this).apply {
            text = "我知道了 ($repeatCount)"
            setBackgroundResource(android.R.drawable.btn_default)
            setTextColor(ContextCompat.getColor(this@OverlayService, android.R.color.black))
            setPadding(50, 20, 50, 20)
            
            var clickCount = 0
            setOnClickListener {
                clickCount++
                if (clickCount >= repeatCount) {
                    closeOverlay()
                } else {
                    this.text = "我知道了 (${repeatCount - clickCount})"
                }
            }
        }
        
        linearLayout.addView(titleTextView)
        linearLayout.addView(contentTextView)
        linearLayout.addView(countTextView)
        linearLayout.addView(dismissButton, 
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 50, 0, 0)
            })
        
        return linearLayout
    }
    
    private fun closeOverlay() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
                overlayView = null
            }
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        closeOverlay()
    }
}