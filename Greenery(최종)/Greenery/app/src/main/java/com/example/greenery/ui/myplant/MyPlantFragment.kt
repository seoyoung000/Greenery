package com.example.greenery.ui.myplant

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.greenery.R
import com.example.greenery.databinding.FragmentMyplantBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class MyPlantFragment : Fragment() {

    private var _binding: FragmentMyplantBinding? = null
    private val binding get() = _binding!!

    companion object {
        val plantList = mutableListOf<Plant>()
        private const val IMAGE_PICK_CODE = 1000
    }

    private lateinit var plantAdapter: PlantAdapter
    private var selectedImageUri: Uri? = null
    private lateinit var selectImageButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyplantBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // RecyclerView 설정
        binding.plantRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // PlantAdapter에 삭제 리스너 추가
        plantAdapter = PlantAdapter(plantList, { plant -> showPlantDetails(plant) }, { plant ->
            deletePlantFromFirestore(plant) // 삭제 리스너 추가
        })
        binding.plantRecyclerView.adapter = plantAdapter

        binding.addPlantButton.setOnClickListener {
            showAddPlantDialog()
        }

        // Firestore에서 실시간 데이터 가져오기
        loadUserPlants() // 사용자별로 식물만 가져오기

        return root
    }

    // Firebase에서 사용자별로 식물만 가져오는 함수
    private fun loadUserPlants() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid // 현재 사용자 ID

        firestore.collection("plants")
            .whereEqualTo("userId", userId) // 사용자 ID로 필터링
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "데이터 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // 기존 식물 리스트와 중복된 식물이 추가되지 않도록 처리
                    val newPlantList = mutableListOf<Plant>()
                    for (document in snapshot.documents) {
                        val plant = document.toObject(Plant::class.java)
                        plant?.let { newPlantList.add(it) }
                    }

                    // 기존 리스트를 새 리스트로 교체 (중복 없이)
                    plantList.clear()
                    plantList.addAll(newPlantList)

                    // 리사이클러뷰 업데이트
                    plantAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun showAddPlantDialog(plantToEdit: Plant? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_plant, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val speciesEditText = dialogView.findViewById<EditText>(R.id.speciesEditText)
        val radioGroupWaterInterval = dialogView.findViewById<RadioGroup>(R.id.radioGroupWaterInterval)
        val spinnerDays = dialogView.findViewById<Spinner>(R.id.spinnerDays)
        val layoutDaysOfWeek = dialogView.findViewById<LinearLayout>(R.id.layoutDaysOfWeek)
        val checkMonday = dialogView.findViewById<CheckBox>(R.id.checkMonday)
        val checkTuesday = dialogView.findViewById<CheckBox>(R.id.checkTuesday)
        val checkWednesday = dialogView.findViewById<CheckBox>(R.id.checkWednesday)
        val checkThursday = dialogView.findViewById<CheckBox>(R.id.checkThursday)
        val checkFriday = dialogView.findViewById<CheckBox>(R.id.checkFriday)
        val checkSaturday = dialogView.findViewById<CheckBox>(R.id.checkSaturday)
        val checkSunday = dialogView.findViewById<CheckBox>(R.id.checkSunday)
        selectImageButton = dialogView.findViewById(R.id.selectImageButton)

        // Spinner에 1부터 365까지의 숫자 추가
        val daysAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, (1..365).toList())
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDays.adapter = daysAdapter

        // 이미지 선택 버튼 클릭 리스너
        selectImageButton.setOnClickListener {
            openImageChooser()
        }

        // 물 주기 간격 선택에 따른 UI 조정
        radioGroupWaterInterval.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioEveryXDays -> {
                    spinnerDays.visibility = View.VISIBLE
                    layoutDaysOfWeek.visibility = View.GONE
                }
                R.id.radioSpecificDays -> {
                    spinnerDays.visibility = View.GONE
                    layoutDaysOfWeek.visibility = View.VISIBLE
                }
            }
        }

        // 기존 식물 정보가 있을 경우 입력
        plantToEdit?.let {
            nameEditText.setText(it.name)
            speciesEditText.setText(it.species)
            selectedImageUri = Uri.parse(it.image)
            selectImageButton.text = "이미지 선택 완료"
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (plantToEdit == null) "식물 추가" else "식물 수정")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val species = speciesEditText.text.toString().trim()
                var waterInterval: String? = null // 물 주기 간격 초기화

                // 선택된 물 주기 간격 확인
                when (radioGroupWaterInterval.checkedRadioButtonId) {
                    R.id.radioEveryXDays -> {
                        waterInterval = "${spinnerDays.selectedItem}일마다"
                    }
                    R.id.radioSpecificDays -> {
                        val daysOfWeek = mutableListOf<String>()
                        if (checkMonday.isChecked) daysOfWeek.add("월요일")
                        if (checkTuesday.isChecked) daysOfWeek.add("화요일")
                        if (checkWednesday.isChecked) daysOfWeek.add("수요일")
                        if (checkThursday.isChecked) daysOfWeek.add("목요일")
                        if (checkFriday.isChecked) daysOfWeek.add("금요일")
                        if (checkSaturday.isChecked) daysOfWeek.add("토요일")
                        if (checkSunday.isChecked) daysOfWeek.add("일요일")
                        waterInterval = daysOfWeek.joinToString(", ")
                    }
                }

                // 필수 입력값 확인 (이름과 종만 체크)
                if (name.isEmpty() || species.isEmpty()) {
                    Toast.makeText(context, "식물 이름과 종은 필수입니다.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Firestore에 이미지 업로드 및 식물 정보 저장
                if (selectedImageUri != null) {
                    uploadImageAndSavePlant(name, species, waterInterval ?: "미정")
                } else {
                    // 이미지가 없을 경우 Firestore에 데이터만 저장
                    savePlantToFirestore(name, species, waterInterval ?: "미정", null)
                }
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.show()
    }

    private fun uploadImageAndSavePlant(name: String, species: String, waterInterval: String) {
        if (selectedImageUri == null) {
            Toast.makeText(context, "이미지가 선택되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRef = storage.reference.child("plant_images/${UUID.randomUUID()}.jpg")
        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                    savePlantToFirestore(name, species, waterInterval, imageUrl.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePlantToFirestore(name: String, species: String, waterInterval: String, imageUrl: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid // 로그인한 사용자의 UID 가져오기
        val plant = Plant(
            id = UUID.randomUUID().toString(),
            name = name,
            species = species,
            waterInterval = waterInterval,
            image = imageUrl ?: "",
            userId = userId ?: "" // 사용자 ID 추가
        )

        firestore.collection("plants").document(plant.id).set(plant)
            .addOnSuccessListener {
                plantList.add(plant)
                plantAdapter.notifyItemInserted(plantList.size - 1)
                Toast.makeText(context, "식물이 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "식물 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePlantFromFirestore(plant: Plant) {
        Log.d("Delete", "Deleting plant: ${plant.name} with ID: ${plant.id}") // 로그 추가
        val plantRef = firestore.collection("plants").document(plant.id)

        // Firebase Storage에서 이미지 삭제
        if (plant.image.isNotEmpty()) {
            val imageRef = storage.reference.child("plant_images/${plant.image.split("/").last()}")
            imageRef.delete().addOnSuccessListener {
                Log.d("Delete", "Image deleted from Storage")
            }.addOnFailureListener { e ->
                Log.e("Delete", "Error deleting image from Storage: ${e.message}")
            }
        }

        // Firestore에서 식물 삭제
        plantRef.delete()
            .addOnSuccessListener {
                Log.d("Delete", "Firestore deletion successful")
                Toast.makeText(context, "식물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                // 리스트에서 해당 식물 제거
                val position = plantList.indexOf(plant)
                if (position != -1) {
                    plantList.removeAt(position) // 리스트에서 제거
                    plantAdapter.notifyItemRemoved(position) // RecyclerView에서 해당 아이템 삭제
                }
            }
            .addOnFailureListener { e ->
                Log.e("Delete", "Firestore deletion failed: ${e.message}")
                Toast.makeText(context, "식물 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPlantDetails(plant: Plant) {
        val detailsDialog = AlertDialog.Builder(requireContext())
            .setTitle(plant.name)
            .setMessage("종: ${plant.species}\n물 주기: ${plant.waterInterval}")
            .setNegativeButton("취소", null)
            .setPositiveButton("수정") { _, _ ->
                showAddPlantDialog(plant) // 수정 화면으로 전환
            }
            .setNeutralButton("삭제") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("삭제 확인")
                    .setMessage("${plant.name} 식물을 정말로 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        deletePlantFromFirestore(plant) // 삭제 함수 호출
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            .create()

        detailsDialog.show()
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectImageButton.text = "이미지 선택 완료" // 버튼 텍스트 변경
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
