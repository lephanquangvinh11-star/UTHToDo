package com.uthtodo.dpgh.data.remote

import com.uthtodo.dpgh.data.model.Task
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object TaskApiClient {
    private const val BASE_URL = "https://share.tryapps.store/mock/teach/tasks"
    private const val AUTH_TOKEN = "TuanTruong-uth-student"

    private fun readBody(conn: HttpURLConnection): String {
        val stream = try {
            if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        } catch (_: Exception) {
            conn.errorStream
        } ?: return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    suspend fun getTasks(): Result<List<Task>> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(BASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Authorization", "Bearer $AUTH_TOKEN")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val bodyStr = readBody(conn)
            conn.disconnect()
            if (code in 200..299) {
                val jsonArray = JSONArray(bodyStr)
                val list = mutableListOf<Task>()
                for (i in 0 until jsonArray.length()) {
                    list.add(Task.fromJson(jsonArray.getJSONObject(i)))
                }
                Result.success(list)
            } else {
                val errorMsg = try {
                    JSONObject(bodyStr).optString("message", bodyStr)
                } catch (_: Exception) {
                    bodyStr
                }
                Result.failure(Exception(if (errorMsg.isNotBlank()) errorMsg else "Lỗi $code khi tải công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
