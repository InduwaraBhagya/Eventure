package com.example.eventure

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase auth instance
        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.etLoginEmail)
        val passwordInput = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupText = findViewById<TextView>(R.id.tvSignupRedirect)
        val spinner = findViewById<Spinner>(R.id.spinnerUserRole)

        val roles = arrayOf("Select Role", "User", "Admin")
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, roles) {
            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent)
                val tv = view as TextView
                tv.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return view
            }

            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val tv = view as TextView
                tv.setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val selectedRole = spinner.selectedItem.toString()

            if (email.isEmpty() || password.isEmpty() || selectedRole == "Select Role") {
                Toast.makeText(this, "Please fill in all fields and select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login successful as $selectedRole", Toast.LENGTH_SHORT).show()
                        // You can add role-based navigation here
                        // startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        //signupText.setOnClickListener {
            // Navigate to SignupActivity (if created)
           // val intent = Intent(this, SignupActivity::class.java)
            //startActivity(intent)
       // }
    }
}
