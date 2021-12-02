package jp.ac.it_college.std.s20010.quizv2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import jp.ac.it_college.std.s20010.quizv2.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MainActivity : AppCompatActivity() {
    // bindingの定義 templateurlの定義
    private lateinit var binding: ActivityMainBinding
    private val templateurl = "https://script.google.com/macros/s/AKfycbznWpk2m8q6lbLWSS6qaz3uS6j3L4zPwv7CqDEiC433YOgAdaFekGJmjoAO60quMg6l/exec?f="
    private lateinit var oldVersion: String
    private val helper = DataBaseHelper(this)

    // 画面の描画
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // bindingの定義
        binding = ActivityMainBinding.inflate(layoutInflater)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        oldVersion = pref.getString("version", "000000").toString()

        setContentView(binding.root)

        binding.Try.setOnClickListener {
            val intent = Intent(this, Sub::class.java)
            startActivity(intent)
        }
    }

    //画面遷移したあと
    override fun onResume() {
        super.onResume()
        getVersion(templateurl+"version") // 46行目のgetVersion関数の実行
    }

    @UiThread // MainActivityで実行する宣言
    // バージョンを取得する関数
    private fun getVersion(url: String) {
        // コルーチンを実行する
        lifecycleScope.launch {
            val result = getJson(url) // 65行目のgetJson関数の実行
            getVersionPost(result) // 100行目のgetVersionPost関数の実行
        }
    }

    @UiThread
    // データを取得する関数
    private fun getData(url: String) {
        lifecycleScope.launch {
            val result = getJson(url) // 65行目のgetJson関数の実行
            getDataPost(result) // 108行目のgetDataPost関数の実行
        }
    }

    @WorkerThread // MainActivityとは別のスレッドで実行する宣言
    // 通信をしてデータを取得する関数
    private suspend fun getJson(url: String): String {
        // ここら辺は難しいと思うからテンプレートみたいな感じって思ってもろてw
        val res = withContext(Dispatchers.IO) {
            var result = ""
            val url = URL(url) // URLクラスにする
            val con = url.openConnection() as? HttpURLConnection
            con?.let {
                try {
                    it.connectTimeout = 10000 // 通信する時間の設定
                    it.readTimeout = 10000 // 通信して読む時間の設定
                    it.requestMethod = "GET" // 通信の種類
                    it.connect()

                    val stream = it.inputStream
                    result = extendString(stream) // 93行目のextendString関数の実行
                    stream.close()
                } catch(ex: SocketTimeoutException) {
                    // 通信する時間を超えた場合の処理
                    println("通信タイムアウト")
                }
                it.disconnect() // 通信を解除
            }
            result
        }
        return res // 1回目は49行目に戻る 2回目は58行目に戻る
    }

    // InputStream型をString型に変更する関数
    private fun extendString(stream: InputStream?) : String {
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        return reader.readText()
    }

    @UiThread
    // バージョンをネットから取得したのちに実行する関数
    private fun getVersionPost(result: String) {
        val newVersion = JSONObject(result).getString("version")
        if (oldVersion != newVersion) {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            pref.edit().putString("version", newVersion).apply()
            getData(templateurl + "data")
        }

        // 56行目のgetData関数の実行
    }

    @UiThread
    // データをネットから取得したのちに実行する関数
    private fun getDataPost(result: String) {
        val rootData = JSONArray(result)
        val db = helper.writableDatabase
        for(i in 0 until rootData.length()) {
            val data = rootData.getJSONObject(i)
            val insert = """
                insert into eito(_id, question, answers, choices)
                values (?, ?, ?, ?)
            """.trimIndent()

            val stmt = db.compileStatement(insert)
            stmt.bindLong(1, data.getLong("id"))
            stmt.bindString(2, data.getString("question"))
            stmt.bindLong(3, data.getLong("answers"))
            stmt.bindString(4, data.getString("choices"))
            stmt.executeInsert()

        }
    }
}