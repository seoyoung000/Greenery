package com.example.greenery.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.greenery.R
import com.example.greenery.databinding.FragmentHomeBinding
import com.example.greenery.ui.home.weatherAPI.WeatherApiService
import com.example.greenery.ui.home.weatherAPI.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = "3bff322e73709c0f25c2cf6f48841dfe" // OpenWeatherMap API 키
    private val taskMap = mutableMapOf<String, MutableList<Task>>() // 날짜별 할 일 목록
    private var selectedDate: String = getCurrentDate() // 기본 날짜 설정
    private lateinit var myAdapter: MyAdapter // 할 일 목록 어댑터
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // RecyclerView 설정
        binding.taskRecyclerView.layoutManager = LinearLayoutManager(context)
        // MyAdapter 초기화 시 selectedDate를 전달합니다.
        myAdapter = MyAdapter(
            taskMap[selectedDate] ?: mutableListOf(),
            selectedDate,
            { task, isChecked -> onTaskCheckedChanged(task, isChecked) }, // 체크박스 상태 변경
            { task -> showTaskDetails(task) } // 삭제 버튼 클릭
        )
        binding.taskRecyclerView.adapter = myAdapter



        // 캘린더 날짜 변경 리스너
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
            updateTaskList()
        }

        // 할 일 추가 버튼 클릭 리스너
        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        // 날씨 정보 표시 (현재 위치로)
        getLastLocation()

        // Firestore에서 할 일 목록 불러오기
        loadAllTasksFromFirestore() // 모든 할 일 불러오기
        return root
    }


    // 현재 위치를 가져오는 메소드
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 요청
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // 위치 정보를 가져옴
        fusedLocationClient.lastLocation
            .addOnSuccessListener(requireActivity()) { location: Location? ->
                if (location != null) {
                    Log.d(
                        "HomeFragment",
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    )
                    getCityName(location)  // 위치가 있으면 도시 이름을 가져오고 날씨를 표시
                } else {
                    binding.weatherTextView.text = "위치를 찾을 수 없습니다."
                    fetchWeatherData("Seoul")  // 기본적으로 서울 날씨 가져오기
                }
            }
    }

    // 도시 이름을 가져오는 메소드
    private fun getCityName(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses: List<Address>? =
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val city = address.locality ?: "서울" // 도시 이름 가져오기
            fetchWeatherData(city)  // 날씨 데이터 가져오기
            binding.weatherTextView.text = "위치: $city"
        } else {
            fetchWeatherData("Seoul")  // 기본적으로 서울 날씨 가져오기
            binding.weatherTextView.text = "도시 이름을 가져올 수 없습니다. 서울의 날씨를 가져옵니다."
        }
    }

    // 날씨 API 호출
    private fun fetchWeatherData(city: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService::class.java)
        service.getCurrentWeather(city, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    val temperature = weatherResponse?.main?.temp
                    val description =
                        translateWeatherDescription(weatherResponse?.weather?.get(0)?.description)
                    displayWeather(city, temperature, description)
                } else {
                    displayWeather(city, null, "날씨 정보를 가져오는 데 실패했습니다. 오류 코드: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                if (_binding == null) {
                    Log.e("HomeFragment", "Binding is null, cannot display weather.")
                    return // binding이 null이면 조기 반환
                }
                displayWeather(city, null, "API 호출 실패: ${t.message ?: "알 수 없는 오류"}")
            }
        })
    }

    // 날씨 설명 번역
    private fun translateWeatherDescription(description: String?): String {
        return when (description?.toLowerCase(Locale.getDefault())) {
            "clear sky" -> "맑음"
            "few clouds" -> "약간의 구름"
            "scattered clouds" -> "흩어진 구름"
            "broken clouds" -> "구름 많음"
            "shower rain" -> "소나기"
            "rain" -> "비"
            "thunderstorm" -> "천둥번개"
            "snow" -> "눈"
            "mist" -> "안개"
            else -> description ?: "알 수 없음"
        }
    }

    // 날씨 정보를 화면에 표시하는 메소드
    private fun displayWeather(city: String, temperature: Float?, description: String?) {
        // 날씨 정보 카드에 데이터를 반영
        val weatherLocation = binding.weatherLocation
        val weatherTemperature = binding.weatherTemperature
        val weatherDescription = binding.weatherDescription
        val weatherIcon = binding.weatherIcon

        // 위치를 TextView에 반영
        weatherLocation.text = "위치: $city"
        // 온도를 안전하게 처리
        weatherTemperature.text = "온도: ${temperature?.toInt() ?: "정보 없음"}°C"
        // 날씨 상태를 안전하게 처리
        weatherDescription.text = description ?: "상태 정보 없음"

        // 날씨 아이콘을 변경
        val iconRes = when (description?.toLowerCase(Locale.getDefault())) {
            "맑음" -> R.drawable.ic_clear_sky
            "약간의 구름" -> R.drawable.ic_broken_clouds
            "흩어진 구름" -> R.drawable.ic_broken_clouds
            "구름 많음" -> R.drawable.ic_broken_clouds
            "소나기" -> R.drawable.ic_rain
            "비" -> R.drawable.ic_rain
            "천둥번개" -> R.drawable.ic_rain
            "눈" -> R.drawable.ic_snow
            "안개" -> R.drawable.ic_broken_clouds
            else -> R.drawable.ic_unknown
        }
        weatherIcon.setImageResource(iconRes)
    }

    private fun loadAllTasksFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("HomeFragment", "User is not logged in.")
            return
        }

        db.collection("tasks").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val allTasks = document.data ?: return@addOnSuccessListener
                    taskMap.clear()
                    allTasks.keys.forEach { date ->
                        val tasks = allTasks[date] as? List<Map<String, Any>> ?: listOf()
                        taskMap[date] = tasks.mapNotNull {
                            val id = it["id"] as? String
                            val title = it["title"] as? String
                            val isChecked = it["isChecked"] as? Boolean ?: false
                            if (id != null && title != null) {
                                Task(id, title, isChecked)
                            } else {
                                null
                            }
                        }.toMutableList()
                    }
                    updateTaskList()
                } else {
                    taskMap.clear()
                    updateTaskList()
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Failed to load tasks: ${e.message}")
            }
    }

    private fun updateTaskList() {
        val tasksForSelectedDate = taskMap[selectedDate] ?: mutableListOf()

        // 어댑터 초기화
        myAdapter = MyAdapter(
            tasksForSelectedDate,
            selectedDate,
            { task, isChecked -> onTaskCheckedChanged(task, isChecked) }, // 체크박스 상태 변경
            { task -> showTaskDetails(task) } // 삭제 버튼 클릭
        )

        binding.taskRecyclerView.adapter = myAdapter // RecyclerView에 어댑터 설정
    }




    private fun showAddTaskDialog() {
        val taskInput = EditText(context)
        taskInput.hint = "할 일을 입력하세요"
        val dialog = AlertDialog.Builder(requireContext()).setTitle("할 일 추가")
            .setView(taskInput)
            .setPositiveButton("추가") { _, _ ->
                val taskTitle = taskInput.text.toString()
                if (taskTitle.isNotEmpty()) {
                    val taskId = UUID.randomUUID().toString() // 고유 ID 생성
                    val newTask = Task(taskId, taskTitle, false) // Task 객체 생성
                    taskMap.putIfAbsent(selectedDate, mutableListOf())
                    taskMap[selectedDate]?.add(newTask) // 새로운 할 일 추가
                    updateTaskList() // UI 업데이트
                    saveTaskToFirestore(newTask) // Firestore에 저장
                } else {
                    Toast.makeText(context, "할 일을 입력하세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }

    private fun saveTaskToFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskData = mapOf(
            "id" to task.id,
            "title" to task.title,
            "isChecked" to task.isChecked
        )

        val userDocRef = db.collection("tasks").document(userId)

        // 날짜 필드에 할 일을 추가
        userDocRef.set(mapOf(selectedDate to FieldValue.arrayUnion(taskData)), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("HomeFragment", "Task saved to Firestore: ${task.title}")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error saving task to Firestore: ${e.message}")
            }
    }

    private fun showTaskDetails(task: Task) {
        val detailsDialog = AlertDialog.Builder(requireContext()).setTitle("할 일 삭제")
            .setMessage("이 할 일을 삭제하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                deleteTaskFromFirestore(task)
            }
            .create()
        detailsDialog.show()
    }

    private fun deleteTaskFromFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskDataToDelete = mapOf(
            "id" to task.id,
            "title" to task.title,
            "isChecked" to task.isChecked
        )

        db.collection("tasks").document(userId)
            .update(selectedDate, FieldValue.arrayRemove(taskDataToDelete)) // 정확한 task 객체 삭제
            .addOnSuccessListener {
                Log.d("HomeFragment", "Task deleted from Firestore: ${task.title}")
                taskMap[selectedDate]?.remove(task) // 로컬 리스트에서 삭제
                updateTaskList() // RecyclerView 업데이트
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error deleting task from Firestore: ${e.message}")
            }
    }

    // 체크박스 상태 변경 시 호출
    private fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        // 로컬에서 상태 업데이트
        task.isChecked = isChecked
        updateTaskInFirestore(task) // Firestore에도 상태 업데이트
        updateTaskList() // UI 갱신
    }

    // Firestore에서 할 일 상태 업데이트
    private fun updateTaskInFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("tasks").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // 날짜별 할 일 배열을 가져오기
                val tasksForDate = document.get(selectedDate) as? List<Map<String, Any>> ?: listOf()

                // 해당 task의 isChecked 상태를 업데이트
                val updatedTasks = tasksForDate.map {
                    if (it["id"] == task.id) {
                        it.toMutableMap().apply {
                            this["isChecked"] = task.isChecked // 체크 상태를 업데이트
                        }
                    } else {
                        it
                    }
                }

                // Firestore에 날짜 필드 업데이트
                userDocRef.update(selectedDate, updatedTasks)
                    .addOnSuccessListener {
                        Log.d("HomeFragment", "Task updated successfully: ${task.title}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Error updating task: ${e.message}")
                    }
            } else {
                Log.e("HomeFragment", "Document does not exist for user $userId")
            }
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "Error fetching document: ${e.message}")
        }
    }

    // 오늘 날짜 가져오기
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    // 뷰가 파괴될 때 바인딩 객체를 null로 설정
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}