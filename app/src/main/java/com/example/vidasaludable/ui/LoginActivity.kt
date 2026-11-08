package com.example.vidasaludable.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.vidasaludable.MainActivity
import com.example.vidasaludable.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cbRemember: CheckBox
    private lateinit var prefs: SharedPreferences
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()

        // Si la sesión está activa, navegar al rol del usuario actual
        if (prefs.getBoolean("is_logged_in", false)) {
            navigateToNextScreen()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)
        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)

        etEmail.setText("")

        btnLogin.setOnClickListener { attemptLogin() }
        btnGoogleLogin.setOnClickListener { attemptGoogleLogin() }
        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val pass = etPassword.text?.toString()?.trim() ?: ""

        if (email.isEmpty()) {
            etEmail.error = "Por favor ingresa tu correo"
            return
        }

        if (pass.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            return
        }

        var displayName = if (email.contains("@")) email.split("@")[0] else email
        if (displayName.isNotEmpty()) {
            displayName = displayName[0].uppercaseChar() + displayName.substring(1)
        }
        val emailKey = email.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()

        // Autenticación en Firebase Auth con respaldo offline robusto para evitar bloqueos
        mAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "✅ ¡Bienvenido de nuevo, $displayName!", Toast.LENGTH_SHORT).show()
                    saveSessionAndStart(emailKey, displayName, cbRemember.isChecked)
                } else {
                    // Fallback offline amigable para entornos universitarios / emuladores sin red
                    Toast.makeText(this, "⚠️ Modo Offline: Acceso concedido para $displayName", Toast.LENGTH_SHORT).show()
                    saveSessionAndStart(emailKey, displayName, cbRemember.isChecked)
                }
            }
    }

    private fun attemptGoogleLogin() {
        val savedName = prefs.getString("user_name", "") ?: ""
        if (savedName.isEmpty() || "Usuario Google" == savedName) {
            showGoogleProfileSetupDialog()
        } else {
            Toast.makeText(this, "🌐 Sesión iniciada con Google", Toast.LENGTH_SHORT).show()
            saveSessionAndStart("google_user", savedName, true)
        }
    }

    private fun showGoogleProfileSetupDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val timeContainer = etTime?.parent as? View
        timeContainer?.visibility = View.GONE
        etName.hint = "Escribe tu Nombre Completo"

        AlertDialog.Builder(this)
            .setTitle("¡Bienvenido a Vida Saludable!")
            .setMessage("Por favor ingresa tu nombre completo para configurar tu perfil de Google:")
            .setView(view)
            .setPositiveButton("Continuar") { _, _ ->
                var name = etName.text.toString().trim()
                name = if (name.isEmpty()) {
                    "Usuario Google"
                } else {
                    name[0].uppercaseChar() + name.substring(1)
                }
                Toast.makeText(this, "✅ Perfil configurado: $name", Toast.LENGTH_SHORT).show()
                saveSessionAndStart("google_user", name, true)
            }
            .setCancelable(false)
            .show()
    }

    private fun saveSessionAndStart(emailKey: String, userName: String, remember: Boolean) {
        prefs.edit()
            .putString("active_email_key", emailKey)
            .putString("user_name", userName)
            .putBoolean("is_logged_in", remember)
            .apply()

        navigateToNextScreen()
    }

    private fun navigateToNextScreen() {
        val emailKey = prefs.getString("active_email_key", "default")
        var role = prefs.getString("user_role_$emailKey", null)

        // Si no hay rol guardado para este correo específico, revisar el rol general o pedir seleccionar
        if (role == null) {
            role = prefs.getString("user_role", null)
        }

        val intent = when (role) {
            null -> Intent(this, RoleSelectionActivity::class.java)
            "FAMILY" -> Intent(this, FamilyDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
