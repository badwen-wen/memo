package com.example.memoapp

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MemoManager(private val context: Context) {
    
    private val fileName = "memos.json"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "MemoManager"
    }
    
    fun saveMemos(memos: List<Memo>) {
        try {
            val jsonArray = JSONArray()
            
            for (memo in memos) {
                val jsonObject = JSONObject().apply {
                    put("id", memo.id)
                    put("title", memo.title)
                    put("content", memo.content)
                    put("dateCreated", dateFormat.format(memo.dateCreated))
                    put("dateModified", dateFormat.format(memo.dateModified))
                    put("reminderTime", memo.reminderTime ?: 0L)
                    put("isCompleted", memo.isCompleted)
                    put("latitude", memo.latitude ?: 0.0)
                    put("longitude", memo.longitude ?: 0.0)
                }
                jsonArray.put(jsonObject)
            }
            
            val jsonString = jsonArray.toString(2) // 格式化为带缩进的JSON字符串
            
            // 写入内部存储
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            
            Log.d(TAG, "备忘录已保存到 ${context.filesDir}/$fileName")
        } catch (e: Exception) {
            Log.e(TAG, "保存备忘录失败", e)
        }
    }
    
    fun loadMemos(): MutableList<Memo> {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                return mutableListOf()
            }
            
            val jsonString = context.openFileInput(fileName).use { inputStream ->
                inputStream.bufferedReader().readText()
            }
            
            val jsonArray = JSONArray(jsonString)
            val memos = mutableListOf<Memo>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val dateCreated = dateFormat.parse(jsonObject.getString("dateCreated")) ?: Date()
                val dateModified = dateFormat.parse(jsonObject.getString("dateModified")) ?: Date()
                
                val memo = Memo(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    content = jsonObject.getString("content"),
                    dateCreated = dateCreated,
                    dateModified = dateModified,
                    reminderTime = if (jsonObject.getLong("reminderTime") > 0) jsonObject.getLong("reminderTime") else null,
                    isCompleted = jsonObject.getBoolean("isCompleted"),
                    latitude = if (jsonObject.getDouble("latitude") != 0.0) jsonObject.getDouble("latitude") else null,
                    longitude = if (jsonObject.getDouble("longitude") != 0.0) jsonObject.getDouble("longitude") else null
                )
                
                memos.add(memo)
            }
            
            Log.d(TAG, "从 ${context.filesDir}/$fileName 加载了 ${memos.size} 条备忘录")
            return memos
        } catch (e: Exception) {
            Log.e(TAG, "加载备忘录失败", e)
            return mutableListOf()
        }
    }
    
    fun exportToFile(filePath: String): Boolean {
        return try {
            val memos = loadMemos()
            val jsonArray = JSONArray()
            
            for (memo in memos) {
                val jsonObject = JSONObject().apply {
                    put("id", memo.id)
                    put("title", memo.title)
                    put("content", memo.content)
                    put("dateCreated", dateFormat.format(memo.dateCreated))
                    put("dateModified", dateFormat.format(memo.dateModified))
                    put("reminderTime", memo.reminderTime ?: 0L)
                    put("isCompleted", memo.isCompleted)
                    put("latitude", memo.latitude ?: 0.0)
                    put("longitude", memo.longitude ?: 0.0)
                }
                jsonArray.put(jsonObject)
            }
            
            val jsonString = jsonArray.toString(2)
            val file = File(filePath)
            
            // 确保目录存在
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            
            file.writeText(jsonString)
            Log.d(TAG, "备忘录已导出到 $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "导出备忘录失败", e)
            false
        }
    }
    
    fun importFromFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "导入文件不存在: $filePath")
                return false
            }
            
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val memos = mutableListOf<Memo>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val dateCreated = dateFormat.parse(jsonObject.getString("dateCreated")) ?: Date()
                val dateModified = dateFormat.parse(jsonObject.getString("dateModified")) ?: Date()
                
                val memo = Memo(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    content = jsonObject.getString("content"),
                    dateCreated = dateCreated,
                    dateModified = dateModified,
                    reminderTime = if (jsonObject.getLong("reminderTime") > 0) jsonObject.getLong("reminderTime") else null,
                    isCompleted = jsonObject.getBoolean("isCompleted"),
                    latitude = if (jsonObject.getDouble("latitude") != 0.0) jsonObject.getDouble("latitude") else null,
                    longitude = if (jsonObject.getDouble("longitude") != 0.0) jsonObject.getDouble("longitude") else null
                )
                
                memos.add(memo)
            }
            
            // 保存导入的数据
            saveMemos(memos)
            Log.d(TAG, "从 $filePath 导入了 ${memos.size} 条备忘录")
            true
        } catch (e: Exception) {
            Log.e(TAG, "导入备忘录失败", e)
            false
        }
    }
}