package com.example.greenery.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.greenery.R

class TodoAdapter(private val taskList: MutableList<String>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskTextView: TextView = view.findViewById(R.id.taskTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.taskTextView.text = taskList[position]

        // 사용자 정의 폰트 적용
        val typeface = ResourcesCompat.getFont(holder.itemView.context, R.font.nanumbarunpen_r)
        holder.taskTextView.typeface = typeface
    }

    override fun getItemCount(): Int {
        return taskList.size
    }
}
