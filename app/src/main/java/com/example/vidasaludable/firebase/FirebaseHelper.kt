package com.example.vidasaludable.firebase

import android.util.Log
import com.example.vidasaludable.model.Routine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    private const val TAG = "FirebaseHelper"

    init {
        // Asegurar autenticación anónima si no hay usuario activo para permitir guardado en Firestore
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener { Log.d(TAG, "Autenticación anónima exitosa para Firestore") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error en auth anónima", e) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando Firebase Auth: ${e.message}")
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            val auth = FirebaseAuth.getInstance()
            var user = auth.currentUser
            if (user == null) {
                auth.signInAnonymously()
                user = auth.currentUser
            }
            user
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

            val safeId = if (userId.isNotEmpty()) userId else "default_user"
            db.collection("users").document(safeId).set(userMap)
                .addOnSuccessListener { Log.d(TAG, "✅ Usuario guardado exitosamente en Firebase Firestore: $safeId") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Error guardando usuario en Firebase: ${e.message}", e) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción guardando usuario en Firebase: " + e.message, e)
        }
    }

    fun syncRoutineToCloud(routine: Routine) {
        try {
            val user = getCurrentUser()
            val uid = user?.uid ?: "guest_user"

            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users")
                .document(uid)
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
                .addOnSuccessListener { Log.d(TAG, "✅ Rutina sincronizada con Firebase: ${routine.name}") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Error al sincronizar rutina con Firebase: ${e.message}", e) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al sincronizar rutina con Firebase: " + e.message, e)
        }
    }

    fun deleteRoutineFromCloud(routineId: Long) {
        try {
            val user = getCurrentUser()
            val uid = user?.uid ?: "guest_user"

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("routines")
                .document(routineId.toString())
                .delete()
                .addOnSuccessListener { Log.d(TAG, "🗑️ Rutina eliminada en Firebase: $routineId") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Error al borrar rutina en Firebase: ${e.message}", e) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al borrar rutina en Firebase: " + e.message, e)
        }
    }
}
