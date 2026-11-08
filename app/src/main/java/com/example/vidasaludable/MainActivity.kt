package com.example.vidasaludable

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vidasaludable.database.DatabaseHelper
import com.example.vidasaludable.firebase.FirebaseHelper
import com.example.vidasaludable.model.Routine
import com.example.vidasaludable.notification.NotificationHelper
import com.example.vidasaludable.ui.EmergencyProfileActivity
import com.example.vidasaludable.ui.GamesActivity
import com.example.vidasaludable.ui.LoginActivity
import com.example.vidasaludable.ui.RoutineAdapter
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), RoutineAdapter.OnRoutineActionListener {

    companion object {
        private const val PERMISSION_REQUEST_NOTIF = 101
        private const val PERMISSION_REQUEST_LOCATION = 102
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper
    private val routines = ArrayList<Routine>()
    private lateinit var adapter: RoutineAdapter

    private lateinit var screenHoy: View
    private lateinit var screenConsejos: View
    private lateinit var screenEmergencia: View
    private lateinit var tabHoy: Button
    private lateinit var tabConsejos: Button
    private lateinit var tabEmergencia: Button
    private lateinit var tvUserGreeting: TextView
    private lateinit var tvSeniorCode: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvCongrats: TextView
    private lateinit var tvSaved: TextView
    private lateinit var tvStatDone: TextView
    private lateinit var tvStatPending: TextView
    private lateinit var tvStatTotal: TextView
    private lateinit var cardLoveNote: View
    private lateinit var tvLoveNoteContent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etContactName: EditText
    private lateinit var etContactPhone: EditText
    private lateinit var etMedicalNotes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        screenHoy = findViewById(R.id.screenHoy)
        screenConsejos = findViewById(R.id.screenConsejos)
        screenEmergencia = findViewById(R.id.screenEmergencia)
        tabHoy = findViewById(R.id.tabHoy)
        tabConsejos = findViewById(R.id.tabConsejos)
        tabEmergencia = findViewById(R.id.tabEmergencia)

        tvUserGreeting = findViewById(R.id.tvUserGreeting)
        tvSeniorCode = findViewById(R.id.tvSeniorCode)
        tvDate = findViewById(R.id.tvDate)
        tvPercent = findViewById(R.id.tvPercent)
        tvCongrats = findViewById(R.id.tvCongrats)
        tvSaved = findViewById(R.id.tvSaved)

        tvStatDone = findViewById(R.id.tvStatDone)
        tvStatPending = findViewById(R.id.tvStatPending)
        tvStatTotal = findViewById(R.id.tvStatTotal)

        cardLoveNote = findViewById(R.id.cardLoveNote)
        tvLoveNoteContent = findViewById(R.id.tvLoveNoteContent)
        val btnCloseLoveNote = findViewById<TextView>(R.id.btnCloseLoveNote)

        progressBar = findViewById(R.id.progressBar)

        etContactName = findViewById(R.id.etContactName)
        etContactPhone = findViewById(R.id.etContactPhone)
        etMedicalNotes = findViewById(R.id.etMedicalNotes)

        checkNotificationPermission()

        tvSeniorCode.setOnClickListener { copySeniorCodeToClipboard() }
        findViewById<View>(R.id.btnGames).setOnClickListener {
            val intent = Intent(this, GamesActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.btnTour).setOnClickListener { showCharacterTourDialog() }
        findViewById<View>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }
        findViewById<View>(R.id.btnLogout).setOnClickListener { logout() }

        NotificationHelper.syncAllPeriodicAlarms(this)

        loadRoutines()
        adapter = RoutineAdapter(this, routines, dbHelper, this)
        val lv = findViewById<ListView>(R.id.lvRoutines)
        lv.adapter = adapter

        findViewById<View>(R.id.btnAdd).setOnClickListener { showAddOrEditDialog(null) }

        buildTips()

        loadEmergencyDataFromDb()

        findViewById<View>(R.id.btn123).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Llamar a Emergencias (123)?")
                .setMessage("Se marcará a la línea pública de auxilio.")
                .setPositiveButton("Llamar Ahora") { _, _ -> dial("123") }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<View>(R.id.btnSaveContact).setOnClickListener {
            val name = etContactName.text.toString().trim()
            val phone = etContactPhone.text.toString().trim()
            val notes = etMedicalNotes.text.toString().trim()

            dbHelper.saveEmergencyInfo(name, phone, notes)

            tvSaved.visibility = View.VISIBLE
            Toast.makeText(this, "Información guardada correctamente en la Base de Datos", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnCallContact).setOnClickListener {
            val phone = etContactPhone.text.toString().trim()
            val name = etContactName.text.toString().trim()
            if (phone.isEmpty()) {
                Toast.makeText(this, "Guarda primero un número de contacto de confianza", Toast.LENGTH_LONG).show()
            } else {
                val contactLabel = if (name.isEmpty()) phone else "$name ($phone)"
                AlertDialog.Builder(this)
                    .setTitle("¿Llamar a contacto?")
                    .setMessage("Realizar llamada a $contactLabel")
                    .setPositiveButton("Llamar") { _, _ -> dial(phone) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        findViewById<View>(R.id.btnEmergencyProfile).setOnClickListener {
            startActivity(Intent(this, EmergencyProfileActivity::class.java))
        }

        tabHoy.setOnClickListener { showTab(0) }
        tabConsejos.setOnClickListener { showTab(1) }
        tabEmergencia.setOnClickListener { showTab(2) }

        setGreetingAndDate()
        updateProgress()
        checkLoveNote()
        showTab(0)

        btnCloseLoveNote?.setOnClickListener {
            cardLoveNote.visibility = View.GONE
            prefs.edit().remove("love_note").apply()
        }

        if (prefs.getBoolean("is_new_user", false)) {
            prefs.edit().putBoolean("is_new_user", false).apply()
            showCharacterTourDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        checkLoveNote()
    }

    private fun checkLoveNote() {
        val loveNote = prefs.getString("love_note", "") ?: ""
        if (loveNote.isNotEmpty()) {
            tvLoveNoteContent.text = "\"$loveNote\""
            cardLoveNote.visibility = View.VISIBLE
        } else {
            cardLoveNote.visibility = View.GONE
        }
    }

    private fun copySeniorCodeToClipboard() {
        val seniorCode = prefs.getString("senior_pairing_code", "USER-7842") ?: "USER-7842"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Código Adulto Mayor", seniorCode)
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "📋 Código $seniorCode copiado al portapapeles. ¡Pásalo a tu familiar!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEditRoutine(routine: Routine) {
        showAddOrEditDialog(routine)
    }

    override fun onRefreshNeeded() {
        refresh()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_NOTIF
                )
            }
        }
    }

    private fun showCharacterTourDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tour, null)

        val tvStepIndicator = view.findViewById<TextView>(R.id.tvStepIndicator)
        val tvTourMessage = view.findViewById<TextView>(R.id.tvTourMessage)
        val tvAvatarTitle = view.findViewById<TextView>(R.id.tvAvatarTitle)
        val btnNextTour = view.findViewById<Button>(R.id.btnNextTour)
        val btnSkipTour = view.findViewById<Button>(R.id.btnSkipTour)

        tvAvatarTitle?.text = "Dr. Salud (Guía Virtual 🧑‍⚕️)"

        val userName = prefs.getString("user_name", "Amigo/a")

        val steps = arrayOf(
            "¡Hola $userName! Soy el Dr. Salud 🧑‍⚕️. Te acompañaré a mantener tus hábitos diarios organizados y saludables.",
            "En la pantalla 'Hoy' puedes presionar '＋ Agregar nueva rutina' para añadir tus tareas y medicinas con hora y alarma personalizadas.",
            "Cada vez que completes una rutina, tócala para marcarla como hecha. ¡Verás cómo sube tu barra de progreso diario!",
            "En la pestaña 'Consejos' encontrarás valiosas recomendaciones sobre hidratación, caminatas y alimentación.",
            "In 'Emergencia' tienes acceso directo al 123 y a tu contacto de confianza. ¡Todo está diseñado para cuidarte!"
        )

        val currentStep = intArrayOf(0)

        tvStepIndicator.text = "Paso 1 de 5"
        tvTourMessage.text = steps[0]

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        btnNextTour.setOnClickListener {
            currentStep[0]++
            if (currentStep[0] < steps.size) {
                tvStepIndicator.text = "Paso ${currentStep[0] + 1} de 5"
                tvTourMessage.text = steps[currentStep[0]]
                if (currentStep[0] == steps.size - 1) {
                    btnNextTour.text = "¡Entendido! 👍"
                }
            } else {
                dialog.dismiss()
            }
        }

        btnSkipTour.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)

        val swMaster = view.findViewById<SwitchMaterial>(R.id.swMaster)
        val swWater = view.findViewById<SwitchMaterial>(R.id.swWater)
        val spWaterInterval = view.findViewById<Spinner>(R.id.spWaterInterval)

        val swMovement = view.findViewById<SwitchMaterial>(R.id.swMovement)
        val spMovementInterval = view.findViewById<Spinner>(R.id.spMovementInterval)

        val swRest = view.findViewById<SwitchMaterial>(R.id.swRest)
        val spRestInterval = view.findViewById<Spinner>(R.id.spRestInterval)
        val swShareLocation = view.findViewById<SwitchMaterial>(R.id.swShareLocation)

        val waterOptions = arrayOf("Cada 1 hora", "Cada 2 horas", "Cada 3 horas", "Cada 4 horas")
        val movementOptions = arrayOf("Cada 1 hora", "Cada 2 horas", "Cada 3 horas")
        val restOptions = arrayOf("Cada 2 horas", "Cada 3 horas", "Cada 4 horas")

        spWaterInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, waterOptions)
        spMovementInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, movementOptions)
        spRestInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, restOptions)

        val masterVal = prefs.getBoolean("notif_master_enabled", true)
        val waterVal = prefs.getBoolean("notif_water_enabled", true)
        val waterHours = prefs.getInt("notif_water_interval_hours", 2)

        val movementVal = prefs.getBoolean("notif_movement_enabled", true)
        val movementHours = prefs.getInt("notif_movement_interval_hours", 2)

        val restVal = prefs.getBoolean("notif_rest_enabled", true)
        val restHours = prefs.getInt("notif_rest_interval_hours", 3)
        val shareLocationVal = prefs.getBoolean("share_location_enabled", false)

        swMaster.isChecked = masterVal
        swWater.isChecked = waterVal
        swMovement.isChecked = movementVal
        swRest.isChecked = restVal
        swShareLocation.isChecked = shareLocationVal

        spWaterInterval.setSelection(Math.max(0, waterHours - 1))
        spMovementInterval.setSelection(Math.max(0, movementHours - 1))
        spRestInterval.setSelection(Math.max(0, restHours - 2))

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val newMaster = swMaster.isChecked
                val newWater = swWater.isChecked
                val newWaterHours = spWaterInterval.selectedItemPosition + 1

                val newMovement = swMovement.isChecked
                val newMovementHours = spMovementInterval.selectedItemPosition + 1

                val newRest = swRest.isChecked
                val newRestHours = spRestInterval.selectedItemPosition + 2

                val newShareLocation = swShareLocation.isChecked

                if (newShareLocation) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
                    }
                }

                prefs.edit()
                    .putBoolean("notif_master_enabled", newMaster)
                    .putBoolean("notif_water_enabled", newWater)
                    .putInt("notif_water_interval_hours", newWaterHours)
                    .putBoolean("notif_movement_enabled", newMovement)
                    .putInt("notif_movement_interval_hours", newMovementHours)
                    .putBoolean("notif_rest_enabled", newRest)
                    .putInt("notif_rest_interval_hours", newRestHours)
                    .putBoolean("share_location_enabled", newShareLocation)
                    .apply()

                NotificationHelper.syncAllPeriodicAlarms(this)

                if (newShareLocation) {
                    Toast.makeText(this, "📍 Ubicación compartida activada con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚙️ Ajustes guardados correctamente", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("¿Cerrar sesión?")
            .setMessage("Regresarás a la pantalla de inicio de sesión.")
            .setPositiveButton("Sí, salir") { _, _ ->
                prefs.edit()
                    .putBoolean("is_logged_in", false)
                    .remove("user_role")
                    .remove("active_email_key")
                    .apply()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTab(i: Int) {
        screenHoy.visibility = if (i == 0) View.VISIBLE else View.GONE
        screenConsejos.visibility = if (i == 1) View.VISIBLE else View.GONE
        screenEmergencia.visibility = if (i == 2) View.VISIBLE else View.GONE

        tabHoy.alpha = if (i == 0) 1.0f else 0.6f
        tabConsejos.alpha = if (i == 1) 1.0f else 0.6f
        tabEmergencia.alpha = if (i == 2) 1.0f else 0.6f
    }

    private fun setGreetingAndDate() {
        val userName = prefs.getString("user_name", "Usuario") ?: "Usuario"
        val seniorCode = prefs.getString("senior_pairing_code", "USER-7842") ?: "USER-7842"

        tvUserGreeting.text = "👋 Hola, $userName"
        tvSeniorCode.text = "🔑 $seniorCode 📋"

        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        var dateStr = sdf.format(Date())
        if (dateStr.isNotEmpty()) {
            dateStr = dateStr[0].uppercaseChar() + dateStr.substring(1)
        }
        tvDate.text = dateStr
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun loadRoutines() {
        routines.clear()
        routines.addAll(dbHelper.getAllRoutines())

        val lastDate = prefs.getString("date", "")
        if (today() != lastDate) {
            dbHelper.resetAllRoutines()
            prefs.edit().putString("date", today()).apply()
            routines.clear()
            routines.addAll(dbHelper.getAllRoutines())
        }
        sortRoutines()
        scheduleAllPendingNotifications()
    }

    private fun scheduleAllPendingNotifications() {
        for (r in routines) {
            if (!r.done) {
                NotificationHelper.scheduleRoutineAlarm(this, r.id, r.name, r.emoji, r.time)
            } else {
                NotificationHelper.cancelRoutineAlarm(this, r.id)
            }
        }
    }

    private fun loadEmergencyDataFromDb() {
        val info = dbHelper.getEmergencyInfo()
        etContactName.setText(info[0])
        etContactPhone.setText(info[1])
        etMedicalNotes.setText(info[2])
    }

    private fun sortRoutines() {
        routines.sortWith(compareBy { it.time })
    }

    private fun refresh() {
        loadRoutines()
        adapter.notifyDataSetChanged()
        updateProgress()
    }

    private fun updateProgress() {
        val total = routines.size
        var done = 0
        for (r in routines) {
            if (r.done) done++
        }
        val pending = total - done
        val percent = if (total > 0) done * 100 / total else 0

        tvStatDone.text = done.toString()
        tvStatPending.text = pending.toString()
        tvStatTotal.text = total.toString()

        tvPercent.text = "$percent%"
        progressBar.max = Math.max(total, 1)
        progressBar.progress = done
        tvCongrats.visibility = if (total > 0 && done == total) View.VISIBLE else View.GONE
    }

    private fun showAddOrEditDialog(routineToEdit: Routine?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val btnTimePicker = view.findViewById<Button>(R.id.btnTimePicker)
        val spEmoji = view.findViewById<Spinner>(R.id.spEmoji)

        val options = arrayOf(
            "💊 Medicina",
            "🍎 Comida",
            "💧 Agua",
            "🚶 Caminata",
            "🧘 Ejercicio",
            "😴 Descanso",
            "🩺 Salud",
            "🧠 Mente / Lectura"
        )

        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spEmoji.adapter = adapterSpinner

        if (routineToEdit != null) {
            etName.setText(routineToEdit.name)
            etTime.setText(routineToEdit.time)
            for (i in options.indices) {
                if (options[i].startsWith(routineToEdit.emoji)) {
                    spEmoji.setSelection(i)
                    break
                }
            }
        } else {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR_OF_DAY)
            val m = now.get(Calendar.MINUTE)
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m))
        }

        val openTimePicker = View.OnClickListener {
            val currentTime = etTime.text.toString().trim()
            var hour = 8
            var minute = 0
            if (currentTime.contains(":")) {
                try {
                    val parts = currentTime.split(":")
                    hour = parts[0].toInt()
                    minute = parts[1].toInt()
                } catch (ignored: Exception) {
                }
            }
            val tpd = TimePickerDialog(
                this,
                { _, hourOfDay, minute1 ->
                    etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1))
                },
                hour, minute, true
            )
            tpd.show()
        }

        etTime.setOnClickListener(openTimePicker)
        btnTimePicker.setOnClickListener(openTimePicker)

        val dialogTitle = if (routineToEdit == null) "Nueva Rutina" else "Editar Rutina"
        val dialog = AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(view)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Escribe el nombre de la rutina"
                return@setOnClickListener
            }

            if (time.isEmpty() || !time.contains(":")) {
                etTime.error = "Selecciona una hora válida"
                return@setOnClickListener
            }

            val emoji = options[spEmoji.selectedItemPosition].split(" ")[0]

            if (routineToEdit == null) {
                val newId = dbHelper.addRoutine(name, time, emoji)
                val newRoutine = Routine(newId, name, time, emoji, false)
                NotificationHelper.scheduleRoutineAlarm(this, newId, name, emoji, time)
                FirebaseHelper.syncRoutineToCloud(newRoutine)
            } else {
                dbHelper.updateRoutine(routineToEdit.id, name, time, emoji)
                routineToEdit.name = name
                routineToEdit.time = time
                routineToEdit.emoji = emoji
                NotificationHelper.scheduleRoutineAlarm(this, routineToEdit.id, name, emoji, time)
                FirebaseHelper.syncRoutineToCloud(routineToEdit)
            }

            refresh()
            Toast.makeText(this, "Rutina guardada y recordatorio programado a las $time", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun buildTips() {
        val box = findViewById<LinearLayout>(R.id.tipsContainer)
        box.removeAllViews()

        val tips = arrayOf(
            arrayOf("💧", "HIDRATACIÓN", "Hidratación Constante", "Toma agua durante todo el día, aunque no sientas sed intensa."),
            arrayOf("🚶", "EJERCICIO", "Movimiento Diario", "Camina al menos 30 minutos al día a tu propio ritmo con calzado cómodo."),
            arrayOf("🍎", "NUTRICIÓN", "Alimentación Balanceada", "Incluye frutas, verduras y fibra en cada comida. Reduce sal y azúcar."),
            arrayOf("😴", "DESCANSO", "Descanso Reparador", "Procura dormir de 7 a 8 horas y acostarte a horas constantes."),
            arrayOf("💊", "SALUD", "Horario de Medicinas", "Use alarmas o este recordatorio para no olvidar tus medicamentos."),
            arrayOf("🧠", "MENTE", "Agilidad Mental", "Lee, resuelve crucigramas o juegos de mesa para mantener activa la mente.")
        )

        for (t in tips) {
            val tipCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(0xFFFFFFFF.toInt())
                    setStroke(dp(1), 0xFFE0E0E0.toInt())
                }
            }

            val tvIcon = TextView(this).apply {
                text = t[0]
                textSize = 28f
                setPadding(0, 0, dp(14), 0)
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val tvTag = TextView(this).apply {
                text = t[1]
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(0xFF1B5E20.toInt())
            }

            val tvTitle = TextView(this).apply {
                text = t[2]
                textSize = 17f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(0xFF1A1A1A.toInt())
            }

            val tvDesc = TextView(this).apply {
                text = t[3]
                textSize = 14f
                setTextColor(0xFF616161.toInt())
                setPadding(0, dp(2), 0, 0)
            }

            textContainer.addView(tvTag)
            textContainer.addView(tvTitle)
            textContainer.addView(tvDesc)

            tipCard.addView(tvIcon)
            tipCard.addView(textContainer, LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            ))

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
            box.addView(tipCard, lp)
        }
    }

    private fun dial(number: String) {
        try {
            val clean = number.replace(Regex("[^0-9+]"), "")
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(v: Int): Int {
        return Math.round(v * resources.displayMetrics.density)
    }
}
