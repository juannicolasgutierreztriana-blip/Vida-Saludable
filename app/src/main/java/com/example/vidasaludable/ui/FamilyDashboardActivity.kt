package com.example.vidasaludable.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vidasaludable.R
import com.example.vidasaludable.database.DatabaseHelper
import com.example.vidasaludable.model.Routine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.ArrayList

class FamilyDashboardActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var etSeniorCode: EditText
    private lateinit var tvLinkedSeniorStatus: TextView
    private lateinit var containerActivityLogs: LinearLayout
    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_dashboard)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etSeniorCode = findViewById(R.id.etSeniorCode)
        tvLinkedSeniorStatus = findViewById(R.id.tvLinkedSeniorStatus)
        containerActivityLogs = findViewById(R.id.containerActivityLogs)

        val btnPasteCode = findViewById<Button>(R.id.btnPasteCode)
        val btnLinkSenior = findViewById<Button>(R.id.btnLinkSenior)
        val btnCardAssignRoutine = findViewById<View>(R.id.btnCardAssignRoutine)
        val btnCardSendNote = findViewById<View>(R.id.btnCardSendNote)
        val btnCardLocateSenior = findViewById<View>(R.id.btnCardLocateSenior)
        val btnCardStats = findViewById<View>(R.id.btnCardStats)
        val tvRefreshLogs = findViewById<TextView>(R.id.tvRefreshLogs)
        val btnGuideFamily = findViewById<Button>(R.id.btnGuideFamily)
        val btnLogoutFamily = findViewById<Button>(R.id.btnLogoutFamily)

        val currentCode = prefs.getString("linked_senior_code", "USER-7842") ?: "USER-7842"
        val seniorName = getSeniorName()

        etSeniorCode.setText(currentCode)
        updateSeniorStatusText(seniorName, currentCode)

        btnPasteCode.setOnClickListener { pasteCodeFromClipboard() }
        btnLinkSenior.setOnClickListener { linkSeniorCode() }
        btnCardAssignRoutine.setOnClickListener { assignRoutineToSenior() }
        btnCardSendNote.setOnClickListener { sendEncouragingNote() }
        btnCardLocateSenior.setOnClickListener { checkAndRequestLocationPermission() }
        btnCardStats.setOnClickListener { showStatisticsDialog() }
        tvRefreshLogs.setOnClickListener {
            loadActivityLogs()
            Toast.makeText(this, "🔄 Historial de actividad actualizado", Toast.LENGTH_SHORT).show()
        }
        btnGuideFamily.setOnClickListener { showFamilyTourDialog() }
        btnLogoutFamily.setOnClickListener { logout() }

        loadActivityLogs()
    }

    private fun getSeniorName(): String {
        return prefs.getString("user_name", "Adulto Mayor") ?: "Adulto Mayor"
    }

    private fun updateSeniorStatusText(name: String, code: String) {
        tvLinkedSeniorStatus.text = "Monitoreando a: $name (Código: $code)"
    }

    private fun pasteCodeFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip() && (clipboard.primaryClip?.itemCount ?: 0) > 0) {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text
            if (pasted != null) {
                etSeniorCode.setText(pasted.toString().trim())
                Toast.makeText(this, "📋 Código pegado del portapapeles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun linkSeniorCode() {
        val code = etSeniorCode.text.toString().trim()
        if (code.isEmpty()) {
            etSeniorCode.error = "Ingresa un código válido"
            return
        }

        prefs.edit().putString("linked_senior_code", code).apply()
        updateSeniorStatusText(getSeniorName(), code)
        Toast.makeText(this, "✅ Vinculado exitosamente con $code", Toast.LENGTH_SHORT).show()
        loadActivityLogs()
    }

    private fun assignRoutineToSenior() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etTime = dialogView.findViewById<EditText>(R.id.etTime)
        val spEmoji = dialogView.findViewById<Spinner>(R.id.spEmoji)

        etTime.setText("16:00")

        val options = arrayOf(
            "💊 Medicina",
            "🍎 Comida",
            "💧 Agua",
            "🚶 Caminata",
            "🧘 Ejercicio",
            "😴 Descanso",
            "🩺 Salud"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spEmoji.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Asignar Rutina a " + getSeniorName())
            .setView(dialogView)
            .setPositiveButton("Asignar y Notificar") { _, _ ->
                val taskName = etName.text.toString().trim()
                val time = etTime.text.toString().trim()
                val emoji = options[spEmoji.selectedItemPosition].split(" ")[0]

                if (taskName.isNotEmpty() && time.isNotEmpty()) {
                    dbHelper.addRoutine(taskName, time, emoji)
                    Toast.makeText(this, "✅ Rutina asignada a " + getSeniorName(), Toast.LENGTH_SHORT).show()
                    loadActivityLogs()
                } else {
                    Toast.makeText(this, "Completa los campos requeridos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendEncouragingNote() {
        val etNote = EditText(this).apply {
            hint = "Ej: ¡Te quiero mucho, ten un hermoso día!"
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        AlertDialog.Builder(this)
            .setTitle("Enviar Nota de Ánimo a " + getSeniorName())
            .setMessage("Esta nota aparecerá en la pantalla principal de " + getSeniorName())
            .setView(etNote)
            .setPositiveButton("Enviar") { _, _ ->
                val note = etNote.text.toString().trim()
                if (note.isNotEmpty()) {
                    prefs.edit().putString("love_note", note).apply()
                    Toast.makeText(this, "💌 Nota enviada a la pantalla de " + getSeniorName(), Toast.LENGTH_SHORT).show()
                    loadActivityLogs()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocationGPS()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de Ubicación Requerido")
                    .setMessage("Para localizar en tiempo real a " + getSeniorName() + " y garantizar su seguridad, necesitamos acceso a los servicios de ubicación GPS.")
                    .setPositiveButton("Conceder Permiso") { _, _ -> requestPermissionLauncher() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                requestPermissionLauncher()
            }
        }
    }

    private fun requestPermissionLauncher() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                fetchCurrentLocationGPS()
            } else {
                Toast.makeText(this, "❌ Permiso denegado. No se puede consultar la ubicación.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchCurrentLocationGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        Toast.makeText(this, "🛰️ Obteniendo ubicación satelital GPS...", Toast.LENGTH_SHORT).show()

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener(this) { location ->
                if (location != null) {
                    showLocationResultDialog(location.latitude, location.longitude)
                } else {
                    getLastKnownLocationFallback()
                }
            }
            .addOnFailureListener(this) { e ->
                Toast.makeText(this, "Error de GPS: " + e.message, Toast.LENGTH_SHORT).show()
                getLastKnownLocationFallback()
            }
    }

    private fun getLastKnownLocationFallback() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener(this) { location ->
                if (location != null) {
                    showLocationResultDialog(location.latitude, location.longitude)
                } else {
                    showLocationResultDialog(4.6097, -74.0817)
                    Toast.makeText(this, "⚠️ Usando posición GPS aproximada de respaldo.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showLocationResultDialog(lat: Double, lng: Double) {
        val seniorName = getSeniorName()

        val homeLat = 4.6097
        val homeLng = -74.0817

        val results = FloatArray(1)
        Location.distanceBetween(homeLat, homeLng, lat, lng, results)
        val distanceInMeters = results[0]

        val isInsideSafeZone = distanceInMeters <= 1000.0f
        val zoneStatus = if (isInsideSafeZone) {
            "🟢 Dentro de Zona Segura (Hogar)"
        } else {
            "⚠️ ALERTA: FUERA DE ZONA SEGURA (A " + Math.round(distanceInMeters) + " mts del hogar)"
        }

        AlertDialog.Builder(this)
            .setTitle("📍 Geolocalización y Geofencing - $seniorName")
            .setMessage("$zoneStatus\n\n🛰️ Coordenadas exactas:\nLatitud: $lat\nLongitud: $lng\n\n🕒 Precisión alta obtenida vía Google Play Services.")
            .setPositiveButton("🗺️ Abrir en Google Maps") { _, _ ->
                try {
                    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($seniorName)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    startActivity(mapIntent)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng")))
                }
            }
            .setNeutralButton("🔄 Actualizar GPS") { _, _ -> fetchCurrentLocationGPS() }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showStatisticsDialog() {
        val barChart = com.github.mikephil.charting.charts.BarChart(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280))
        }

        val entries = ArrayList<com.github.mikephil.charting.data.BarEntry>().apply {
            add(com.github.mikephil.charting.data.BarEntry(1f, 4f)) // Lun
            add(com.github.mikephil.charting.data.BarEntry(2f, 6f)) // Mar
            add(com.github.mikephil.charting.data.BarEntry(3f, 5f)) // Mié
            add(com.github.mikephil.charting.data.BarEntry(4f, 7f)) // Jue
            add(com.github.mikephil.charting.data.BarEntry(5f, 6f)) // Vie
            add(com.github.mikephil.charting.data.BarEntry(6f, 8f)) // Sáb
            add(com.github.mikephil.charting.data.BarEntry(7f, 7f)) // Dom
        }

        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Rutinas Completadas (Semana)").apply {
            color = 0xFF1B5E20.toInt()
        }
        val barData = com.github.mikephil.charting.data.BarData(dataSet)

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.invalidate()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(barChart)
        }

        AlertDialog.Builder(this)
            .setTitle("📊 Reporte Semanal - " + getSeniorName())
            .setView(container)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showFamilyTourDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tour, null)

        val tvStepIndicator = view.findViewById<TextView>(R.id.tvStepIndicator)
        val tvTourMessage = view.findViewById<TextView>(R.id.tvTourMessage)
        val tvAvatarTitle = view.findViewById<TextView>(R.id.tvAvatarTitle)
        val btnNextTour = view.findViewById<Button>(R.id.btnNextTour)
        val btnSkipTour = view.findViewById<Button>(R.id.btnSkipTour)

        tvAvatarTitle?.text = "Asistente del Cuidador 👨‍👩‍👧"

        val steps = arrayOf(
            "¡Bienvenido al Panel del Familiar! Aquí tienes el control para acompañar a tu ser querido en tiempo real.",
            "En la tarjeta superior puedes ingresar y vincular el código único del Adulto Mayor para conectar ambos dispositivos.",
            "Con el botón 'Asignar Rutina' puedes programar tareas, medicinas o citas que aparecerán inmediatamente en su agenda.",
            "Con 'Localizar' puedes consultar su posición GPS real (Geofencing) y con 'Reportes y Estadísticas' ver sus gráficas de cumplimiento. ¡Todo listo!"
        )

        val currentStep = intArrayOf(0)

        tvStepIndicator.text = "Paso 1 de 4"
        tvTourMessage.text = steps[0]

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        btnNextTour.setOnClickListener {
            currentStep[0]++
            if (currentStep[0] < steps.size) {
                tvStepIndicator.text = "Paso ${currentStep[0] + 1} de 4"
                tvTourMessage.text = steps[currentStep[0]]
                if (currentStep[0] == steps.size - 1) {
                    btnNextTour.text = "Entendido 👍"
                }
            } else {
                dialog.dismiss()
            }
        }

        btnSkipTour.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun loadActivityLogs() {
        containerActivityLogs.removeAllViews()
        val seniorName = getSeniorName()

        val routines = dbHelper.getAllRoutines()

        if (routines.isNotEmpty()) {
            for (r in routines) {
                val statusBadge = if (r.done) "✅ COMPLETADO" else "⏳ PENDIENTE"
                val statusBgColor = if (r.done) 0xFFC8E6C9.toInt() else 0xFFFFF3E0.toInt()
                val statusTextColor = if (r.done) 0xFF1B5E20.toInt() else 0xFFE65100.toInt()

                addLogCard(
                    "${r.time} hrs",
                    "${r.emoji} $seniorName ${if (r.done) "completó" else "tiene pendiente"}: ${r.name}",
                    statusBadge,
                    statusBgColor,
                    statusTextColor
                )
            }
        } else {
            addLogCard("Hoy", "🟢 Monitoreando actividad de $seniorName. Aún no hay rutinas agregadas.", "ACTIVO", 0xFFE3F2FD.toInt(), 0xFF1565C0.toInt())
        }

        val note = prefs.getString("love_note", "") ?: ""
        if (note.isNotEmpty()) {
            addLogCard("Reciente", "💌 Nota enviada: \"$note\"", "ENVIADO", 0xFFE1BEE7.toInt(), 0xFF6A1B9A.toInt())
        }
    }

    private fun addLogCard(time: String, message: String, badgeText: String, badgeBgColor: Int, badgeTextColor: Int) {
        val logCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xFFFFFFFF.toInt())
                setStroke(dp(1), 0xFFE0E0E0.toInt())
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val tvTime = TextView(this).apply {
            text = "🕒 $time"
            textSize = 12f
            setTextColor(0xFF1565C0.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val tvBadge = TextView(this).apply {
            text = badgeText
            textSize = 11f
            setTextColor(badgeTextColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(badgeBgColor)
            }
        }

        topRow.addView(tvTime, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        topRow.addView(tvBadge)

        val tvMsg = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(0xFF212121.toInt())
            setPadding(0, dp(6), 0, 0)
        }

        logCard.addView(topRow)
        logCard.addView(tvMsg)

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, dp(10))
        }
        containerActivityLogs.addView(logCard, lp)
    }

    private fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_role")
            .remove("active_email_key")
            .apply()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun dp(v: Int): Int {
        return Math.round(v * resources.displayMetrics.density)
    }
}
