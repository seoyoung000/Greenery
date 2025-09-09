package com.example.greenery.ui.community

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.greenery.databinding.ActivityLogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class LogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogBinding
    private var selectedImageUri: Uri? = null
    private val storageReference = FirebaseStorage.getInstance().reference
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    // 권한 요청을 위한 ActivityResultContracts
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한이 허용되면 이미지 선택기 호출
                imagePickerLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "이미지 선택 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    // 이미지 선택을 위한 Launcher
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                try {
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    binding.imagePreview.setImageBitmap(bitmap)
                    binding.imagePreview.visibility = ImageView.VISIBLE
                    binding.buttonDeleteImage.visibility = Button.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "이미지를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 권한 확인 후 이미지 선택
        if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        }

        // 새 글 작성 모드
        // 글 추가 버튼 클릭 시 처리
        binding.buttonOK.setOnClickListener {
            val detailInfo = binding.editTextText.text.toString().trim()
            val title = binding.editTextTitle.text.toString().trim()

            if (detailInfo.isEmpty() || title.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadImageToFirebase { imageUrl ->
                    savePostToFirestore(title, detailInfo, imageUrl)
                }
            } else {
                savePostToFirestore(title, detailInfo, "")
            }
        }

        // 취소 버튼 클릭 시 처리
        binding.buttonCancel.setOnClickListener {
            Toast.makeText(this, "작성이 취소되었습니다.", Toast.LENGTH_SHORT).show()  // 취소되었음을 알리는 메시지
            finish()  // Activity 종료
        }

        binding.buttonSelectImage.setOnClickListener {
            // 권한이 허용된 경우에만 이미지 선택을 실행
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                imagePickerLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        binding.buttonDeleteImage.setOnClickListener {
            binding.imagePreview.setImageBitmap(null)
            binding.imagePreview.visibility = ImageView.GONE
            binding.buttonDeleteImage.visibility = Button.GONE
            selectedImageUri = null
            Toast.makeText(this, "이미지가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 이미지 업로드 후 다운로드 URL을 받아 Firestore에 저장
    private fun uploadImageToFirebase(onSuccess: (String) -> Unit) {
        val imageRef = storageReference.child("images/${UUID.randomUUID()}.jpg")

        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        imageRef.putBytes(imageData)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "다운로드 URL 가져오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace() // 에러 로그 출력
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace() // 에러 로그 출력
            }
    }

    // 새 글을 Firestore에 저장
    private fun savePostToFirestore(title: String, detailInfo: String, imageUrl: String) {
        val postId = UUID.randomUUID().hashCode() // Int 형식으로 ID 생성
        val currentUserEmail = currentUser?.email ?: "Anonymous"

        val plantLog = PlantLog(
            id = postId, // Int 형식의 ID
            name = currentUserEmail,
            title = title,
            detail = detailInfo,
            img = imageUrl
        )

        firestore.collection("plantlog")
            .document("plantlog$postId") // Int 형식의 ID를 사용하여 Firestore 문서 저장
            .set(plantLog)
            .addOnSuccessListener {
                Toast.makeText(this, "게시글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "게시글 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace() // 에러 로그 출력
            }
    }
}
