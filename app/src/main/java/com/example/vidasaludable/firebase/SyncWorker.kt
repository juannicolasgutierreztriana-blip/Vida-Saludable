package com.example.vidasaludable.firebase

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vidasaludable.database.DatabaseHelper

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val dbHelper = DatabaseHelper(applicationContext)
            val routines = dbHelper.getAllRoutines()
            for (r in routines) {
                FirebaseHelper.syncRoutineToCloud(r)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
