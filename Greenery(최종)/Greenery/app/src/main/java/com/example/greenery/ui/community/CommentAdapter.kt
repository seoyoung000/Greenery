package com.example.greenery.ui.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.greenery.databinding.ItemCommentBinding

data class UserComment(
    var commentId: String = "", // Firestore ID는 문자열로 유지
    val body: String = "",
    val parentCommentId: String? = null
)

class CommentAdapter(private val deleteCommentCallback: (String) -> Unit) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val comments = mutableListOf<UserComment>()

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.textCommentBody.text = comment.body

        // 삭제 버튼 클릭 이벤트
        holder.binding.buttonDeleteComment.setOnClickListener {
            deleteCommentCallback(comment.commentId)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun setComments(newComments: List<UserComment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    fun addComment(comment: UserComment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }
}