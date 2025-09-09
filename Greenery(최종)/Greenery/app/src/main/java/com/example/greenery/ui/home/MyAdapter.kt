package com.example.greenery.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.greenery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MyAdapter(
    private var taskList: MutableList<Task>,
    private val selectedDate: String,
    private val onTaskCheckedChange: (Task, Boolean) -> Unit, // 체크박스 상태 변경 콜백
    private val onDeleteTask: (Task) -> Unit // 삭제 콜백
) : RecyclerView.Adapter<MyAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskTextView: TextView = view.findViewById(R.id.taskTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val taskCheckBox: CheckBox = view.findViewById(R.id.taskCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskTextView.text = task.title
        holder.taskCheckBox.isChecked = task.isChecked

        // 체크박스 상태 변경 리스너 초기화
        holder.taskCheckBox.setOnCheckedChangeListener(null) // 기존 리스너 제거
        holder.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            onTaskCheckedChange(task, isChecked) // 체크박스 상태 변경 호출
        }

        // 삭제 버튼 클릭 리스너
        holder.deleteButton.setOnClickListener {
            onDeleteTask(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTaskList(newTaskList: List<Task>) {
        taskList.clear()
        taskList.addAll(newTaskList)
        notifyDataSetChanged()
    }

    private fun updateTaskInFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("tasks").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Firestore에서 해당 날짜의 작업 목록 가져오기
                val tasksForDate = document.get(selectedDate) as? MutableList<Map<String, Any>> ?: mutableListOf()

                // 작업 업데이트
                val updatedTasks = tasksForDate.map { taskMap ->
                    if (taskMap["id"] == task.id) {
                        taskMap.toMutableMap().apply { this["isChecked"] = task.isChecked }
                    } else {
                        taskMap
                    }
                }

                userDocRef.update(selectedDate, updatedTasks)
                    .addOnSuccessListener {
                        Log.d("MyAdapter", "Task updated successfully: ${task.title}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyAdapter", "Error updating task: ${e.message}")
                    }
            } else {
                Log.e("MyAdapter", "Document does not exist for user $userId")
            }
        }.addOnFailureListener { e ->
            Log.e("MyAdapter", "Error fetching document: ${e.message}")
        }
    }

    fun deleteTaskFromFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("tasks").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val tasksForDate = document.get(selectedDate) as? MutableList<Map<String, Any>> ?: mutableListOf()

                val taskToDelete = mapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "isChecked" to task.isChecked
                )

                userDocRef.update(selectedDate, FieldValue.arrayRemove(taskToDelete))
                    .addOnSuccessListener {
                        Log.d("MyAdapter", "Task deleted from Firestore: ${task.title}")
                        // 로컬 리스트에서 삭제
                        taskList.remove(task)
                        notifyDataSetChanged() // UI 업데이트
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyAdapter", "Error deleting task: ${e.message}")
                    }
            } else {
                Log.e("MyAdapter", "No document found for user $userId")
            }
        }.addOnFailureListener { e ->
            Log.e("MyAdapter", "Error fetching document: ${e.message}")
        }
    }
}

