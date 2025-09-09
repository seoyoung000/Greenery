package com.example.greenery.ui.community

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.greenery.databinding.ItemRecyclerviewBinding
import com.example.greenery.ui.account.MyApplication

data class PlantLog(
    var id: Int = 0,
    var name: String = "",
    var title: String = "",
    var detail: String = "",
    var img: String = ""
)

class MyViewHolder(val adapter: MyAdapter2, val binding: ItemRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root) {
    var id: Long = 0

    // 클릭했을 때 상세 화면으로 이동하는 이벤트
    init {
        itemView.setOnClickListener {
            val position = absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val plantLog = adapter.datas[position]
                val intent = Intent(itemView.context, DetailActivity::class.java)
                intent.putExtra("id", plantLog.id)
                intent.putExtra("name", plantLog.name)
                intent.putExtra("title", plantLog.title)
                intent.putExtra("detail", plantLog.detail)
                intent.putExtra("img", plantLog.img) // 이미지 URL 전달
                itemView.context.startActivity(intent)
            }
        }
    }

    // RecyclerView에 데이터 바인딩
    fun viewBind(pos: Int) {
        id = adapter.datas[pos].id.toLong()
        with(binding) {
            itemTitle.text = adapter.datas[pos].title

            // 이메일에서 @ 앞부분만 추출
            val email = adapter.datas[pos].name
            val userId = email.substringBefore("@")

            // 이메일에서 추출된 부분을 설정
            itemName.text = userId

        }
    }


    class MyAdapter2 : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val datas = mutableListOf<PlantLog>()

        override fun getItemCount(): Int {
            return datas.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return MyViewHolder(
                this,
                ItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as MyViewHolder).viewBind(position)
        }

        // 아이템 추가
        fun addItem(item: PlantLog) {
            MyApplication.db.collection("plantlog")
                .document("plantlog${item.id}") // 고유한 ID로 문서 생성
                .set(item)
            datas.add(item)
            notifyDataSetChanged()
        }
    }
}