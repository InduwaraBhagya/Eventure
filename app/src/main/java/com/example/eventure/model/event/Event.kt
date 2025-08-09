package com.example.eventure.model.event

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val date: Timestamp? = null,  // Use Timestamp instead of String
    val location: String = "",
    val imageUrl: String = ""
)
