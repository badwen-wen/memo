package com.example.memoapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.Date

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddMemo: FloatingActionButton
    private lateinit var themeSpinner: Spinner
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var memoManager: MemoManager
    private lateinit var sharedPrefs: SharedPreferences
    
    private val memos = mutableListOf<Memo>()
    
    // 请求权限的回调处理
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it == true }
        if (!allGranted) {
            Toast.makeText(this, "部分权限被拒绝，可能影响应用功能", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        memoManager = MemoManager(this)
        sharedPrefs = getSharedPreferences("memo_prefs", MODE_PRIVATE)
        
        setupViews()
        setupRecyclerView()
        setupThemeSpinner()
        loadMemos()
        requestPermissions()
        
        fabAddMemo.setOnClickListener {
            openAddEditMemoDialog(null)
        }
        
        // 设置选项菜单
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_import -> {
                    importMemos()
                    true
                }
                R.id.action_export -> {
                    exportMemos()
                    true
                }
                R.id.action_settings -> {
                    showSettingsDialog()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("设置")
            .setMessage("在这里可以调整应用的各种设置")
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recycler_view_memos)
        fabAddMemo = findViewById(R.id.fab_add_memo)
        themeSpinner = findViewById(R.id.theme_spinner)
    }
    
    private fun setupThemeSpinner() {
        val themes = arrayOf("自动", "浅色", "深色")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = adapter
        
        // 从偏好设置中加载当前主题
        val currentTheme = sharedPrefs.getInt("selected_theme", 0) // 默认为自动
        themeSpinner.setSelection(currentTheme)
        
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyTheme(position)
                
                // 保存选择的主题到偏好设置
                with(sharedPrefs.edit()) {
                    putInt("selected_theme", position)
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun applyTheme(themeIndex: Int) {
        when (themeIndex) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    
    private fun setupRecyclerView() {
        memoAdapter = MemoAdapter(
            memos,
            onItemClick = { memo -> openAddEditMemoDialog(memo) },
            onDeleteClick = { memo -> deleteMemo(memo) },
            onToggleComplete = { memo -> toggleCompleteStatus(memo) }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = memoAdapter
        }
    }
    
    private fun loadMemos() {
        memos.clear()
        memos.addAll(memoManager.loadMemos())
        memoAdapter.notifyDataSetChanged()
    }
    
    private fun saveMemos() {
        memoManager.saveMemos(memos)
    }
    
    private fun addOrUpdateMemo(memo: Memo) {
        val existingIndex = memos.indexOfFirst { it.id == memo.id }
        if (existingIndex >= 0) {
            memos[existingIndex] = memo
        } else {
            memos.add(0, memo) // 添加到开头
        }
        saveMemos()
        memoAdapter.updateMemos(memos.toList())
    }
    
    private fun deleteMemo(memo: Memo) {
        AlertDialog.Builder(this)
            .setTitle("删除备忘录")
            .setMessage("确定要删除这条备忘录吗？")
            .setPositiveButton("删除") { _, _ ->
                memos.remove(memo)
                saveMemos()
                memoAdapter.updateMemos(memos.toList())
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun toggleCompleteStatus(memo: Memo) {
        val updatedMemo = memo.copy(isCompleted = !memo.isCompleted, dateModified = Date())
        addOrUpdateMemo(updatedMemo)
    }
    
    private fun openAddEditMemoDialog(memo: Memo? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_memo, null)
        val titleEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_text_title)
        val contentEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_text_content)
        
        if (memo != null) {
            titleEditText.setText(memo.title)
            contentEditText.setText(memo.content)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (memo != null) "编辑备忘录" else "添加备忘录")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val title = titleEditText.text.toString().trim()
                val content = contentEditText.text.toString().trim()
                
                if (title.isEmpty()) {
                    Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val newMemo = if (memo != null) {
                    memo.copy(title = title, content = content, dateModified = Date())
                } else {
                    Memo(title = title, content = content)
                }
                
                addOrUpdateMemo(newMemo)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun exportMemos() {
        // 导出备忘录到外部存储
        val exportDir = "${getExternalFilesDir(null)?.absolutePath}/exports"
        val exportFile = "$exportDir/memos_${System.currentTimeMillis()}.json"
        
        val dir = File(exportDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        if (memoManager.exportToFile(exportFile)) {
            Toast.makeText(this, "备忘录已导出至: $exportFile", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun importMemos() {
        // 打开文件选择器导入备忘录
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            startActivityForResult(intent, 1001)
        } catch (ex: Exception) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File(cacheDir, "temp_import.json")
                    
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    if (memoManager.importFromFile(tempFile.absolutePath)) {
                        loadMemos()
                        Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show()
                    }
                    
                    tempFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "导入过程中出现错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
