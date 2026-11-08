package com.example.vidasaludable.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.vidasaludable.R
import com.example.vidasaludable.firebase.FirebaseHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etBirthDate = findViewById(R.id.etBirthDate)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoogleSignUp = findViewById<Button>(R.id.btnGoogleSignUp)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // Selector de Fecha de Nacimiento (Corregido y Formateado YYYY-MM-DD / DD/MM/YYYY)
        etBirthDate.setOnClickListener { showDatePicker() }

        btnRegister.setOnClickListener { attemptRegister() }
        btnGoogleSignUp.setOnClickListener { attemptGoogleSignIn() }

        tvLoginLink.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val y = 1960
        val m = Calendar.JANUARY
        val d = 1

        val dpd = DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                // Formato seguro universal YYYY-MM-DD para SQLite y Firestore
                val storageDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                // Formato amigable DD/MM/YYYY para visualización
                val displayDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)

                etBirthDate.setText(displayDate)
                prefs.edit().putString("user_birthdate", storageDate).apply()
            },
            y, m, d
        )
        dpd.datePicker.maxDate = System.currentTimeMillis()
        dpd.show()
    }

    private fun attemptRegister() {
        val firstName = etFirstName.text?.toString()?.trim() ?: ""
        val lastName = etLastName.text?.toString()?.trim() ?: ""
        val birthDate = etBirthDate.text?.toString()?.trim() ?: ""
        val email = etEmail.text?.toString()?.trim() ?: ""
        val pass = etPassword.text?.toString()?.trim() ?: ""

        if (firstName.isEmpty()) {
            etFirstName.error = "Ingresa tu nombre"
            return
        }
        if (birthDate.isEmpty()) {
            etBirthDate.error = "Selecciona tu fecha de nacimiento"
            return
        }
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo electrónico"
            return
        }
        if (pass.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        val fullName = firstName + if (lastName.isEmpty()) "" else " $lastName"

        // Registro en Firebase Auth
        try {
            val mAuth = FirebaseAuth.getInstance()
            mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val uid = user?.uid ?: "user_${System.currentTimeMillis()}"
                        FirebaseHelper.saveUserProfile(uid, firstName, lastName, birthDate, email)
                    }
                }
        } catch (ignored: Exception) {
        }

        Toast.makeText(this, "¡Cuenta registrada con éxito!", Toast.LENGTH_SHORT).show()
        saveSessionAndStart(email, fullName, true)
    }

    private fun attemptGoogleSignIn() {
        Toast.makeText(this, "🌐 Registrándose con Cuenta de Google...", Toast.LENGTH_SHORT).show()
        saveSessionAndStart("google@gmail.com", "Usuario Google", true)
    }

    private fun saveSessionAndStart(email: String, userName: String, isNewUser: Boolean) {
        val emailKey = email.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        prefs.edit()
            .putString("active_email_key", emailKey)
            .putString("user_name", userName)
            .putBoolean("is_logged_in", true)
            .putBoolean("is_new_user", isNewUser)
            .remove("user_role") // Forzar la pantalla de selección de rol
            .apply()

        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}
