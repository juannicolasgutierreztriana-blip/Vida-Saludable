package com.example.vidasaludable.firebase

import android.util.Log
import com.example.vidasaludable.model.Routine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    private const val TAG = "FirebaseHelper"

    fun getCurrentUser(): FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserProfile(userId: String, firstName: String, lastName: String, birthDate: String, email: String) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userMap = hashMapOf<String, Any>(
                "firstName" to firstName,
                "lastName" to lastName,
                "birthDate" to birthDate,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users").document(userId).set(userMap)
                .addOnSuccessListener { Log.d(TAG, "Usuario guardado en Firebase Firestore") }
                .addOnFailureListener { e -> Log.e(TAG, "Error guardando usuario en Firebase", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase no configurado o sin conexión: " + e.message)
        }
    }

    fun syncRoutineToCloud(routine: Routine) {
        try {
            val user = getCurrentUser() ?: return

            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users")
                .document(user.uid)
                .collection("routines")
                .document(routine.id.toString())

            val routineMap = hashMapOf<String, Any>(
                "id" to routine.id,
                "name" to routine.name,
                "time" to routine.time,
                "emoji" to routine.emoji,
                "done" to routine.done
            )

            doc.set(routineMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar rutina con Firebase: " + e.message)
        }
    }

    fun deleteRoutineFromCloud(routineId: Long) {
        try {
            val user = getCurrentUser() ?: return

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("routines")
                .document(routineId.toString())
                .delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar rutina en Firebase: " + e.message)
        }
    }
}
