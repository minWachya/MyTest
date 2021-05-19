package com.example.min1

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FragmentWrite : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    lateinit var tvDate : TextView
    lateinit var imgCalendar : ImageView
    lateinit var editTemp : EditText
    lateinit var editTop : EditText
    lateinit var editBottom : EditText
    lateinit var editOuter : EditText
    lateinit var editMemo : EditText
    lateinit var btnCompleteMemo : Button
    lateinit var databaseRef : DatabaseReference    // 파이어베이스 접근 가능한 자료형

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weite, container, false)

        tvDate = view.findViewById(R.id.tvDate)
        imgCalendar = view.findViewById(R.id.imgCalendar)
        editTemp = view.findViewById(R.id.editTemp)
        editTop = view.findViewById(R.id.editTop)
        editBottom = view.findViewById(R.id.editBottom)
        editOuter = view.findViewById(R.id.editOuter)
        editMemo = view.findViewById(R.id.editMemo)
        btnCompleteMemo = view.findViewById(R.id.btnCompleteMemo)

        // intent에서 온도 정보 받이서 온도 설정하기
        //val temp = intent.getStringExtra("temp").toString()
        //editTemp.text = Editable.Factory.getInstance().newEditable(temp)

        // 텍스트뷰에 오늘 날짜 미리 보이기
        var calender = Calendar.getInstance()
        var year = calender.get(Calendar.YEAR)
        var month = calender.get(Calendar.MONDAY)
        var day = calender.get(Calendar.DAY_OF_MONTH)
        var m = "${month + 1}"
        var d = "${day}"
        if (month + 1 < 10) m = "0" + (month + 1)
        if (day < 10) d = "0" + day
        tvDate.text = "${year}.${m}.${d}"

        // 캘린더 이미지 누르면 데이트피커 다이얼로그 보이게
        imgCalendar.setOnClickListener {
            var listner = DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
                // 한자리 숫자 앞에 0 붙여서 2자리로 통일
                m = "${i2}"
                d = "${i3}"
                if (i2 + 1 < 10) m = "0" + (i2 + 1)
                if (i3 < 10) d = "0" + i3

                tvDate.text = "${i}.${m}.${d}"
            }

            // 오늘 날짜가 선택된 다이얼로그 보이기
            var picker = DatePickerDialog(context!!, listner, year, month, day)
            picker.show()
        }

        // 연결된 파이어베이스에서 데이터 가져오기
        databaseRef = FirebaseDatabase.getInstance().reference

        // <기록 완료> 버튼 누르면 위에 적은 내용을 파이어베이스에 저장하고
        // 토스트 띄운 뒤 처음 화면으로 돌아가기
        btnCompleteMemo.setOnClickListener {
            val date = tvDate.text.toString()
            val temp = editTemp.text.toString()
            val top = editTop.text.toString()
            val bottom = editBottom.text.toString()
            val outer = editOuter.text.toString()
            val memo = editMemo.text.toString()

            val month = date.substring(5, 7)
            val tempGroup = getTempGroup(temp)

            // 파이어베이스에 데이터 저장하기
            saveMemo(date, temp, top, bottom, outer, memo, month, tempGroup)

            Toast.makeText(context, "기록 완료하였습니다.", Toast.LENGTH_SHORT).show()
            //finish()
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentWrite().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // 온도 그룹(TempGroup) 정하기
    fun getTempGroup(temp : String) : String {
        val tempInt = temp.toInt()
        var result = ""
        when (tempInt) {
            in 5..8 -> result = "5_8"
            in 9..11 -> result = "9_11"
            in 12..16 -> result = "12_16"
            in 17..19 -> result = "17_19"
            in 20..22 -> result = "20_22"
            in 23..27 -> result = "23_27"
            in 28..50 -> result = "28_"
            else -> result = "_4"
        }
        return result
    }

    // 파이어베이스에 저장
    fun saveMemo(date: String, temp: String, top: String, bottom: String, outer: String,
                 memo: String, month: String, tempGroup: String) {
        // memo에 child로 감상평 추가(이때 키 자동 생성, 이 키 얻어오기)
        var key : String? = databaseRef.child("memo").push().getKey()

        // 객체 생성
        val obj = WeatherMemo(key!!, date, temp, top, bottom, outer, memo, month, tempGroup)
        // 객체를 맵 형으로 변환
        val memotValues : HashMap<String, String> = obj.toMap()

        // 파이어베이스에 넣어주기(인자에 해시맵과 해시맵에 접근할 수 있는 경로 들어가야함)
        // -> 별도의 해시맵을 만들어줘야함
        val childUpdate : MutableMap<String, Any> = HashMap()
        childUpdate["/memo/$key"] = memotValues

        databaseRef.updateChildren(childUpdate)
    }
}