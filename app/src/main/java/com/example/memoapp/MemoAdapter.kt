package com.example.memoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MemoAdapter(
    private var memos: MutableList<Memo>,
    private val onItemClick: (Memo) -> Unit,
    private val onDeleteClick: (Memo) -> Unit,
    private val onToggleComplete: (Memo) -> Unit
) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())

    class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(android.R.id.text1)
        val contentTextView: TextView = itemView.findViewById(android.R.id.text2)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
        val completeButton: Button = itemView.findViewById(R.id.complete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)

        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memos[position]
        
        holder.titleTextView.text = memo.title
        holder.contentTextView.text = memo.content
        holder.dateTextView.text = dateFormat.format(memo.dateModified)
        
        // 设置完成按钮的状态
        holder.completeButton.text = if (memo.isCompleted) "未完成" else "已完成"
        holder.completeButton.setOnClickListener {
            onToggleComplete(memo)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(memo)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(memo)
        }
    }

    override fun getItemCount(): Int = memos.size

    fun updateMemos(newMemos: List<Memo>) {
        this.memos.clear()
        this.memos.addAll(newMemos)
        notifyDataSetChanged()
    }
    
    fun removeItem(position: Int) {
        memos.removeAt(position)
        notifyItemRemoved(position)
    }
    
    fun addItem(item: Memo) {
        memos.add(0, item)  // 添加到开头
        notifyItemInserted(0)
    }
}