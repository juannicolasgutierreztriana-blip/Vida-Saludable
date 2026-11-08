package com.example.vidasaludable.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vidasaludable.model.Routine
import java.util.ArrayList

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "vidasaludable.db"
        private const val DATABASE_VERSION = 5 // Sin rutinas prediseñadas

        // Tabla Rutinas
        const val TABLE_ROUTINES = "routines"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TIME = "time"
        const val COLUMN_EMOJI = "emoji"
        const val COLUMN_DONE = "done"

        // Tabla Contacto Emergencia
        const val TABLE_EMERGENCY = "emergency"
        const val COLUMN_CONTACT_NAME = "contact_name"
        const val COLUMN_CONTACT_PHONE = "contact_phone"
        const val COLUMN_MEDICAL_NOTES = "medical_notes"

        fun clearAllData(context: Context) {
            try {
                val db = DatabaseHelper(context).writableDatabase
                db.execSQL("DELETE FROM $TABLE_ROUTINES")
                db.execSQL("DELETE FROM $TABLE_EMERGENCY")
                db.close()
            } catch (ignored: Exception) {
            }

            try {
                context.getSharedPreferences("habitos_saludables", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createRoutinesTable = ("CREATE TABLE " + TABLE_ROUTINES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_TIME + " TEXT NOT NULL, "
                + COLUMN_EMOJI + " TEXT NOT NULL, "
                + COLUMN_DONE + " INTEGER NOT NULL DEFAULT 0)")

        val createEmergencyTable = ("CREATE TABLE " + TABLE_EMERGENCY + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY DEFAULT 1, "
                + COLUMN_CONTACT_NAME + " TEXT, "
                + COLUMN_CONTACT_PHONE + " TEXT, "
                + COLUMN_MEDICAL_NOTES + " TEXT)")

        db.execSQL(createRoutinesTable)
        db.execSQL(createEmergencyTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTINES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMERGENCY")
        onCreate(db)
    }

    // ---------- OPERACIONES CRUD PARA RUTINAS ----------

    fun addRoutine(name: String, time: String, emoji: String): Long {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_TIME, time)
            put(COLUMN_EMOJI, emoji)
            put(COLUMN_DONE, 0)
        }
        return db.insert(TABLE_ROUTINES, null, cv)
    }

    fun getAllRoutines(): List<Routine> {
        val list = ArrayList<Routine>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ROUTINES ORDER BY $COLUMN_TIME ASC", null)

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    val id = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID))
                    val name = c.getString(c.getColumnIndexOrThrow(COLUMN_NAME))
                    val time = c.getString(c.getColumnIndexOrThrow(COLUMN_TIME))
                    val emoji = c.getString(c.getColumnIndexOrThrow(COLUMN_EMOJI))
                    val done = c.getInt(c.getColumnIndexOrThrow(COLUMN_DONE)) == 1

                    list.add(Routine(id, name, time, emoji, done))
                } while (c.moveToNext())
            }
        }
        return list
    }

    fun updateRoutine(id: Long, name: String, time: String, emoji: String) {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_TIME, time)
            put(COLUMN_EMOJI, emoji)
        }
        db.update(TABLE_ROUTINES, cv, "$COLUMN_ID=?", arrayOf(id.toString()))
    }

    fun updateRoutineStatus(id: Long, done: Boolean) {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_DONE, if (done) 1 else 0)
        }
        db.update(TABLE_ROUTINES, cv, "$COLUMN_ID=?", arrayOf(id.toString()))
    }

    fun deleteRoutine(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ROUTINES, "$COLUMN_ID=?", arrayOf(id.toString()))
    }

    fun resetAllRoutines() {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_DONE, 0)
        }
        db.update(TABLE_ROUTINES, cv, null, null)
    }

    // ---------- OPERACIONES CRUD PARA CONTACTO DE EMERGENCIA ----------

    fun saveEmergencyInfo(name: String, phone: String, notes: String) {
        val db = this.writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_ID, 1)
            put(COLUMN_CONTACT_NAME, name)
            put(COLUMN_CONTACT_PHONE, phone)
            put(COLUMN_MEDICAL_NOTES, notes)
        }

        db.insertWithOnConflict(TABLE_EMERGENCY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getEmergencyInfo(): Array<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_EMERGENCY WHERE $COLUMN_ID=1", null)

        val result = arrayOf("", "", "")
        cursor.use { c ->
            if (c.moveToFirst()) {
                result[0] = c.getString(c.getColumnIndexOrThrow(COLUMN_CONTACT_NAME)) ?: ""
                result[1] = c.getString(c.getColumnIndexOrThrow(COLUMN_CONTACT_PHONE)) ?: ""
                result[2] = c.getString(c.getColumnIndexOrThrow(COLUMN_MEDICAL_NOTES)) ?: ""
            }
        }
        return result
    }
}
