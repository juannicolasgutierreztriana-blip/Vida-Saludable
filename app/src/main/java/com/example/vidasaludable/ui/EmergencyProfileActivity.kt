package com.example.vidasaludable.ui

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.vidasaludable.R
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class EmergencyProfileActivity : AppCompatActivity() {

    private lateinit var etBloodType: TextInputEditText
    private lateinit var etAllergies: TextInputEditText
    private lateinit var etEmergencyContact: TextInputEditText
    private lateinit var ivQrCode: ImageView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_profile)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)

        etBloodType = findViewById(R.id.etBloodType)
        etAllergies = findViewById(R.id.etAllergies)
        etEmergencyContact = findViewById(R.id.etEmergencyContact)
        ivQrCode = findViewById(R.id.ivQrCode)
        val btnSaveEmergency = findViewById<Button>(R.id.btnSaveEmergency)

        // Cargar datos guardados
        etBloodType.setText(prefs.getString("med_blood", ""))
        etAllergies.setText(prefs.getString("med_allergies", ""))
        etEmergencyContact.setText(prefs.getString("med_contact", ""))

        generateQrCode(getEmergencySummary())

        btnSaveEmergency.setOnClickListener {
            val blood = etBloodType.text?.toString()?.trim() ?: ""
            val allergies = etAllergies.text?.toString()?.trim() ?: ""
            val contact = etEmergencyContact.text?.toString()?.trim() ?: ""

            prefs.edit()
                .putString("med_blood", blood)
                .putString("med_allergies", allergies)
                .putString("med_contact", contact)
                .apply()

            generateQrCode(getEmergencySummary())
            Toast.makeText(this, "✅ Ficha médica y QR actualizados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getEmergencySummary(): String {
        val blood = etBloodType.text?.toString().takeIf { !it.isNullOrBlank() } ?: "O+"
        val allergies = etAllergies.text?.toString().takeIf { !it.isNullOrBlank() } ?: "Ninguna"
        val contact = etEmergencyContact.text?.toString().takeIf { !it.isNullOrBlank() } ?: "No registrado"
        return "VIDA SALUDABLE - SOS\nSangre: $blood\nAlergias: $allergies\nContacto: $contact"
    }

    private fun generateQrCode(text: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            ivQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
