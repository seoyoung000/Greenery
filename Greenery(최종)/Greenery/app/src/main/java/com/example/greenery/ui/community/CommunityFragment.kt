package com.example.greenery.ui.community

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.greenery.databinding.FragmentCommunityBinding
import com.google.firebase.firestore.FirebaseFirestore

class CommunityFragment : Fragment() {
    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    lateinit var adapter: MyViewHolder.MyAdapter2
    var dbId = 0
    var userId = ""

    // ActivityResultLauncher 초기화
        private val startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val data = result.data
                    val title = data?.getStringExtra("title") ?: ""
                    val detail = data?.getStringExtra("detail") ?: ""
                    val postId = data?.getIntExtra("postId", -1) ?: -1

                    // 새로운 PlantLog 아이템 추가 또는 기존 아이템 수정
                    if (postId == -1) {
                        // 새로운 글 추가
                        val plantLog = PlantLog(
                            id = ++dbId,
                            name = userId,
                            title = title,
                            detail = detail
                        )
                        adapter.addItem(plantLog)
                    }
                }
            }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 사용자의 ID를 가져오기
        userId = activity?.intent?.getStringExtra("user") ?: "User" // 액티비티로부터 ID 가져오기

        // RecyclerView 설정
        binding.recyclerViewCommunity.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyViewHolder.MyAdapter2()
        binding.recyclerViewCommunity.adapter = adapter
        binding.recyclerViewCommunity.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        )

        // Firebase에서 데이터를 읽어오기
        readDB()

        // FAB 클릭 시 LogActivity로 이동
        binding.fabAddLog.setOnClickListener {
            val intent = Intent(requireContext(), LogActivity::class.java)
            startForResult.launch(intent) // startActivityForResult 대신 launch 사용
        }
    }

    // Firebase에서 데이터를 읽어오는 함수
    private fun readDB() {
        val db = FirebaseFirestore.getInstance()
        db.collection("plantlog")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val log = document.toObject(PlantLog::class.java)
                    if (dbId <= log.id) dbId = log.id
                    adapter.addItem(log)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "서버 데이터 획득 실패", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}