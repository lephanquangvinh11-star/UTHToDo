package com.uthtodo.dpgh.data.model

import com.google.firebase.firestore.DocumentSnapshot
import org.json.JSONArray
import org.json.JSONObject

data class Subtask(
    val title: String,
    val done: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("done", done)
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title.take(200),
            "done" to done
        )
    }

    companion object {
        fun fromJson(json: JSONObject): Subtask {
            return Subtask(
                title = json.optString("title", ""),
                done = json.optBoolean("done", false)
            )
        }

        fun fromMap(map: Map<*, *>): Subtask {
            val title = (map["title"] as? String)?.trim().orEmpty()
            val done = (map["done"] as? Boolean) ?: false
            return Subtask(title = title, done = done)
        }
    }
}

data class Task(
    val id: Int = 0,
    val title: String,
    val note: String = "",
    val category: String = "study", // "study" | "life"
    val date: String = "",          // "yyyy-MM-dd"
    val time: String = "19:00",     // "HH:mm"
    val priority: String = "medium", // "low" | "medium" | "high"
    val remind: Boolean = true,
    val done: Boolean = false,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val categoryLabel: String
        get() = when (category.lowercase()) {
            "study" -> "Học tập"
            "life" -> "Cuộc sống"
            else -> category
        }

    val priorityLabel: String
        get() = when (priority.lowercase()) {
            "high" -> "Cao"
            "low" -> "Thấp"
            else -> "Vừa"
        }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            if (id > 0) put("id", id)
            put("title", title)
            put("note", note)
            put("category", category)
            put("date", date)
            put("time", time)
            put("priority", priority)
            put("remind", remind)
            put("done", done)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            val subArray = JSONArray()
            subtasks.forEach { subArray.put(it.toJson()) }
            put("subtasks", subArray)
        }
    }

    fun toFirestoreMap(uid: String): Map<String, Any> {
        val normalizedCategory = if (category.lowercase() == "life") "life" else "study"
        val normalizedPriority = when (priority.lowercase()) {
            "high" -> "high"
            "low" -> "low"
            else -> "medium"
        }
        val safeDate = if (date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) date else ""
        val safeTime = if (time.matches(Regex("^\\d{2}:\\d{2}$"))) time else "19:00"
        val safeCreatedAt = if (createdAt > 0) createdAt else System.currentTimeMillis()
        val safeUpdatedAt = if (updatedAt > 0) updatedAt else System.currentTimeMillis()

        return mapOf(
            "id" to (if (id > 0) id else 1),
            "uid" to uid,
            "title" to title.trim().ifBlank { "Công việc" }.take(200),
            "note" to note.trim().take(2000),
            "category" to normalizedCategory,
            "date" to safeDate,
            "time" to safeTime,
            "priority" to normalizedPriority,
            "remind" to remind,
            "done" to done,
            "subtasks" to subtasks.take(50).map { it.toMap() },
            "createdAt" to safeCreatedAt,
            "updatedAt" to safeUpdatedAt
        )
    }

    companion object {
        fun fromJson(json: JSONObject): Task {
            val subList = mutableListOf<Subtask>()
            val subArray = json.optJSONArray("subtasks")
            if (subArray != null) {
                for (i in 0 until subArray.length()) {
                    val subObj = subArray.optJSONObject(i)
                    if (subObj != null) {
                        subList.add(Subtask.fromJson(subObj))
                    }
                }
            }
            val now = System.currentTimeMillis()
            return Task(
                id = json.optInt("id", 0),
                title = json.optString("title", ""),
                note = json.optString("note", ""),
                category = json.optString("category", "study"),
                date = json.optString("date", ""),
                time = json.optString("time", "19:00"),
                priority = json.optString("priority", "medium"),
                remind = json.optBoolean("remind", true),
                done = json.optBoolean("done", false),
                subtasks = subList,
                createdAt = json.optLong("createdAt", now),
                updatedAt = json.optLong("updatedAt", now)
            )
        }

        fun fromFirestoreDoc(doc: DocumentSnapshot): Task? {
            val id = doc.getLong("id")?.toInt() ?: return null
            val title = doc.getString("title") ?: return null
            val note = doc.getString("note") ?: ""
            val category = doc.getString("category") ?: "study"
            val date = doc.getString("date") ?: ""
            val time = doc.getString("time") ?: "19:00"
            val priority = doc.getString("priority") ?: "medium"
            val remind = doc.getBoolean("remind") ?: true
            val done = doc.getBoolean("done") ?: false
            val now = System.currentTimeMillis()
            val createdAt = doc.getLong("createdAt") ?: now
            val updatedAt = doc.getLong("updatedAt") ?: now

            val rawSubtasks = doc.get("subtasks") as? List<*>
            val subtasks = rawSubtasks?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { Subtask.fromMap(it) }
            } ?: emptyList()

            return Task(
                id = id,
                title = title,
                note = note,
                category = category,
                date = date,
                time = time,
                priority = priority,
                remind = remind,
                done = done,
                subtasks = subtasks,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
