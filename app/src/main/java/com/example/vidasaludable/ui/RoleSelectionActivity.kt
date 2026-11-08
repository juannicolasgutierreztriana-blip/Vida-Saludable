package com.example.vidasaludable.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.vidasaludable.MainActivity
import com.example.vidasaludable.R
import java.util.Random

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)

        findViewById<View>(R.id.cardSeniorRole).setOnClickListener { selectRole("SENIOR") }
        findViewById<View>(R.id.cardFamilyRole).setOnClickListener { selectRole("FAMILY") }
    }

    private fun selectRole(role: String) {
        val emailKey = prefs.getString("active_email_key", "default")
        val userName = prefs.getString("user_name", "Usuario") ?: "Usuario"
        val prefix = if (userName.length >= 4) userName.substring(0, 4).uppercase() else "USER"
        val randomDigits = 1000 + Random().nextInt(9000)
        val seniorCode = "$prefix-$randomDigits"

        prefs.edit()
            .putString("user_role", role)
            .putString("user_role_$emailKey", role)
            .putString("senior_pairing_code", seniorCode)
            .apply()

        val intent = if ("FAMILY" == role) {
            Intent(this, FamilyDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
