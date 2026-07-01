package com.example.data

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    @field:JvmField
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null
)
