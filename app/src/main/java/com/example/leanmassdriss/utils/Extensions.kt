package com.example.leanmassdriss.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── VIEW — Visibilité ─────────────────────────────────────────────
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

// ── VIEW — Feedback utilisateur ───────────────────────────────────
fun View.snack(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.snackWithAction(
    message: String,
    actionLabel: String,
    duration: Int = Snackbar.LENGTH_LONG,
    action: () -> Unit
) {
    Snackbar.make(this, message, duration).setAction(actionLabel) { action() }.show()
}

// ── CONTEXT — Toast ───────────────────────────────────────────────
fun Context.toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
fun Context.toastLong(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }

// ── EDITTEXT — Validation & lecture ────────────────────────────────
fun EditText.value(): String = text.toString().trim()

fun EditText.validateNotEmpty(errorMessage: String = "Champ requis"): Boolean {
    return if (value().isEmpty()) {
        error = errorMessage
        requestFocus()
        false
    } else {
        error = null
        true
    }
}

fun EditText.toDoubleOrNull(): Double? = value().toDoubleOrNull()

// ── DOUBLE — Formatage numérique ──────────────────────────────────
fun Double.round2(): Double = Math.round(this * 100.0) / 100.0
fun Double.format2(): String = String.format(Locale.getDefault(), "%.2f", this)

// ── DATE — Formatage pour l'historique ─────────────────────────────
fun Date.toFormattedString(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.getDefault())
    return sdf.format(this)
}

// ── FLOW — Collecte lifecycle-safe (Pour l'affichage modern) ──────
fun <T> Flow<T>.collectIn(
    activity: FragmentActivity,
    action: suspend (T) -> Unit
) {
    activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { action(it) }
        }
    }
}