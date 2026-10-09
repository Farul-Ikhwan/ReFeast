package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.refeast.app.data.Areas
import com.refeast.app.data.Repository
import com.refeast.app.data.Roles
import com.refeast.app.util.Session
import com.refeast.app.util.Validators

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val role = intent.getStringExtra("role") ?: Roles.DONOR
        findViewById<TextView>(R.id.tvRoleLabel).text = "Joining as $role"

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirm)
        val spinnerArea = findViewById<Spinner>(R.id.spinnerArea)

        spinnerArea.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Areas.ALL)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnRegister).setOnClickListener {
            // Check every field. Each check gives an error message, or null if the value is fine.
            etName.error = Validators.name(etName.text.toString())
            etEmail.error = Validators.email(etEmail.text.toString())
            etPhone.error = Validators.phone(etPhone.text.toString())
            etAddress.error = Validators.required(etAddress.text.toString(), "Address")
            etPassword.error = Validators.password(etPassword.text.toString())
            if (etConfirm.text.toString() != etPassword.text.toString()) {
                etConfirm.error = "Passwords do not match"
            } else {
                etConfirm.error = null
            }

            val hasError = etName.error != null || etEmail.error != null || etPhone.error != null ||
                etAddress.error != null || etPassword.error != null || etConfirm.error != null
            if (hasError) return@setOnClickListener

            val repo = Repository(this)
            val error = repo.register(
                etName.text.toString(), etEmail.text.toString(), etPassword.text.toString(), role,
                etPhone.text.toString(), etAddress.text.toString(), spinnerArea.selectedItem.toString()
            )
            if (error != null) {
                etEmail.error = error
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Log the new user in straight away.
            val user = repo.findUserByEmail(etEmail.text.toString())!!
            Session(this).login(user.id, user.role)
            Toast.makeText(this, "Welcome to Re:Feast, ${user.name}!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
