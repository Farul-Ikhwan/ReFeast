package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.refeast.app.data.Repository
import com.refeast.app.util.Session
import com.refeast.app.util.Validators

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val role = intent.getStringExtra("role") ?: "Donor"
        findViewById<TextView>(R.id.tvRoleLabel).text = "Signing in as $role"

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("role", role)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnLogin).setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // 1. Check the inputs and show errors next to the fields.
            val emailError = Validators.email(email)
            etEmail.error = emailError
            if (password.isEmpty()) etPassword.error = "Password is required" else etPassword.error = null
            if (emailError != null || password.isEmpty()) return@setOnClickListener

            // 2. Check the account in the database.
            val user = Repository(this).login(email, password)
            if (user == null) {
                Toast.makeText(this, "Incorrect email or password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (user.role != role) {
                Toast.makeText(this, "This account is registered as a ${user.role}. Go back and choose that role.",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 3. Remember the user and open the home screen.
            Session(this).login(user.id, user.role)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
