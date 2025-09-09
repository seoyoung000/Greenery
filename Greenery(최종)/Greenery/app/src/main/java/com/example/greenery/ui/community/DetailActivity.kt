package com.example.greenery.ui.community

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.greenery.databinding.ActivityDetailBinding
import com.google.firebase.firestore.FirebaseFirestore


class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var commentAdapter: CommentAdapter
    private val firestore = FirebaseFirestore.getInstance()

    private var postId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전달받은 이미지 URL
        val imageUrl = intent.getStringExtra("img") ?: ""

        // 이미지가 있으면 로드
        if (imageUrl.isNotEmpty()) {
            binding.itemImage.visibility = View.VISIBLE // 이미지뷰를 보이도록 설정
            Glide.with(this)
                .load(imageUrl)
                .into(binding.itemImage)
        }

        // Post 데이터 설정
        postId = intent.getIntExtra("id", 0)
        val postTitle = intent.getStringExtra("title").orEmpty()
        val postName = intent.getStringExtra("name").orEmpty()
        val postDetail = intent.getStringExtra("detail").orEmpty()
        val postImg = intent.getStringExtra("img").orEmpty()

        binding.itemTitle.text = postTitle
        binding.itemName.text = postName
        binding.itemDetail.text = postDetail
        Glide.with(this).load(postImg).into(binding.itemImage)

        // 댓글 RecyclerView 설정
        setupRecyclerView()

        // 댓글 추가 버튼 동작
        binding.buttonAddComment.setOnClickListener {
            val newCommentContent = binding.editTextComment.text.toString().trim()
            if (newCommentContent.isNotEmpty()) {
                addComment(newCommentContent)
            } else {
                Toast.makeText(this, "댓글 내용을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 기존 댓글 로드 (실시간 업데이트)
        loadComments()

        // 게시글 삭제 버튼 동작
        binding.buttonDeletePost.setOnClickListener {
            deletePost()
        }

        // 뒤로가기 버튼
        binding.buttonBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter { commentId ->
            deleteComment(commentId)
        }
        binding.recyclerViewComments.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity)
            adapter = commentAdapter
        }
    }


    private fun loadComments() {
        firestore.collection("plantlog").document("plantlog$postId")
            .collection("comments")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.map { document ->
                        UserComment(
                            commentId = document.id, // Firestore ID 사용
                            body = document.getString("body").orEmpty(),
                            parentCommentId = document.getString("parentCommentId")
                        )
                    }
                    commentAdapter.setComments(comments)
                }
            }
    }


    private fun addComment(content: String) {
        val newComment = hashMapOf(
            "body" to content,
            "parentCommentId" to null
        )

        firestore.collection("plantlog").document("plantlog$postId")
            .collection("comments")
            .add(newComment)
            .addOnSuccessListener {
                binding.editTextComment.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "댓글 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteComment(commentId: String) {
        firestore.collection("plantlog").document("plantlog$postId")
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "댓글 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun deletePost() {
        firestore.collection("plantlog").document("plantlog$postId")
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "게시글 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

