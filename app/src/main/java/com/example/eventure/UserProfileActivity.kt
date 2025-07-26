package com.example.eventure

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

data class UserProfile(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var topic: String = ""
)

class ProfileActivity : AppCompatActivity() {

    // UI elements
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var topicEditText: EditText
    private lateinit var saveButton: Button


    private var currentUser = UserProfile(
        uid = "1",
        name = "Induwara",
        email = "induwara@gmail.com",
        phone = "0771234567",
        topic = "Music"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        initViews()
        loadUserProfile()

        saveButton.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun initViews() {
        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        phoneEditText = findViewById(R.id.editTextPhone)
        topicEditText = findViewById(R.id.editTextTopic)
        saveButton = findViewById(R.id.buttonSave)
    }

    private fun loadUserProfile() {
        nameEditText.setText(currentUser.name)
        emailEditText.setText(currentUser.email)
        phoneEditText.setText(currentUser.phone)
        topicEditText.setText(currentUser.topic)
    }

    private fun saveUserProfile() {
        currentUser = currentUser.copy(
            name = nameEditText.text.toString(),
            email = emailEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            topic = topicEditText.text.toString()
        )

        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
    }
}
