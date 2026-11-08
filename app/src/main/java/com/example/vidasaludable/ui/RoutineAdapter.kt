package com.example.vidasaludable.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.vidasaludable.R
import com.example.vidasaludable.database.DatabaseHelper
import com.example.vidasaludable.model.Routine
import com.example.vidasaludable.notification.NotificationHelper

class RoutineAdapter(
    private val context: Context,
    private val routines: List<Routine>,
    private val dbHelper: DatabaseHelper,
    private val actionListener: OnRoutineActionListener?
) : BaseAdapter() {

    interface OnRoutineActionListener {
        fun onEditRoutine(routine: Routine)
        fun onRefreshNeeded()
    }

    override fun getCount(): Int {
        return routines.size
    }

    override fun getItem(position: Int): Any {
        return routines[position]
    }

    override fun getItemId(position: Int): Long {
        return routines[position].id
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_routine, parent, false)
        val r = routines[position]

        val card = view.findViewById<View>(R.id.card)
        val tvEmoji = view.findViewById<TextView>(R.id.tvEmoji)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        val cb = view.findViewById<CheckBox>(R.id.cb)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        tvEmoji.text = r.emoji
        tvName.text = r.name
        tvTime.text = "🕒 ${r.time} hrs"
        cb.isChecked = r.done

        if (r.done) {
            tvName.paintFlags = tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvName.setTextColor(0xFF757575.toInt())
            tvStatusBadge.text = "COMPLETADO ✓"
            tvStatusBadge.setTextColor(0xFF1B5E20.toInt())

            val badgeBg = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFFC8E6C9.toInt())
            }
            tvStatusBadge.background = badgeBg
        } else {
            tvName.paintFlags = tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tvName.setTextColor(0xFF1A1A1A.toInt())
            tvStatusBadge.text = "PENDIENTE 🔔"
            tvStatusBadge.setTextColor(0xFFE65100.toInt())

            val badgeBg = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(0xFFFFE0B2.toInt())
            }
            tvStatusBadge.background = badgeBg
        }

        val emojiBg = GradientDrawable().apply {
            cornerRadius = dp(27).toFloat() // circular shape for 54dp box
            setColor(if (r.done) 0xFFE8F5E9.toInt() else 0xFFF5F5F5.toInt())
        }
        tvEmoji.background = emojiBg

        card.setOnClickListener {
            val newDone = !r.done
            dbHelper.updateRoutineStatus(r.id, newDone)
            if (newDone) {
                NotificationHelper.cancelRoutineAlarm(context, r.id)
            } else {
                NotificationHelper.scheduleRoutineAlarm(context, r.id, r.name, r.emoji, r.time)
            }
            actionListener?.onRefreshNeeded()
        }

        btnEdit.setOnClickListener {
            actionListener?.onEditRoutine(r)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("¿Eliminar rutina?")
                .setMessage("\"${r.name}\"")
                .setPositiveButton("Eliminar") { _, _ ->
                    NotificationHelper.cancelRoutineAlarm(context, r.id)
                    dbHelper.deleteRoutine(r.id)
                    actionListener?.onRefreshNeeded()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }

    private fun dp(v: Int): Int {
        return Math.round(v * context.resources.displayMetrics.density)
    }
}
