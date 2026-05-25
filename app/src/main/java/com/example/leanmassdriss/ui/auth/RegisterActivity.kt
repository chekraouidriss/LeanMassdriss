package com.example.leanmassdriss.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.leanmassdriss.R
import com.example.leanmassdriss.ui.main.MainActivity
import com.example.leanmassdriss.utils.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class RegisterActivity : AppCompatActivity() {

    // ── ViewModel partagé avec LoginActivity ───────────────────────
    private val viewModel: AuthViewModel by viewModels()

    // ── Vues (liées via findViewById — PAS de ViewBinding) ─────────
    private lateinit var tilEmail          : TextInputLayout
    private lateinit var tilPassword        : TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etEmail            : TextInputEditText
    private lateinit var etPassword         : TextInputEditText
    private lateinit var etConfirmPassword  : TextInputEditText
    private lateinit var btnRegister        : Button
    private lateinit var progressBar        : ProgressBar
    private lateinit var tvGoToLogin        : TextView

    // ══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        setupListeners()
        observeRegisterState()
    }

    // ══════════════════════════════════════════════════════════════
    // LIAISON DES VUES — findViewById (sans ViewBinding)
    // ══════════════════════════════════════════════════════════════

    private fun bindViews() {
        tilEmail           = findViewById(R.id.tilEmail)
        tilPassword        = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etEmail            = findViewById(R.id.etEmail)
        etPassword         = findViewById(R.id.etPassword)
        etConfirmPassword  = findViewById(R.id.etConfirmPassword)
        btnRegister        = findViewById(R.id.btnRegister)
        progressBar        = findViewById(R.id.progressBar)
        tvGoToLogin        = findViewById(R.id.tvGoToLogin)
    }

    // ══════════════════════════════════════════════════════════════
    // LISTENERS
    // ══════════════════════════════════════════════════════════════

    private fun setupListeners() {

        // ── Bouton S'inscrire ─────────────────────────────────────
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                viewModel.register(
                    email           = etEmail.value(),
                    password        = etPassword.value(),
                    confirmPassword = etConfirmPassword.value()
                )
            }
        }

        // ── Retour vers Login ─────────────────────────────────────
        tvGoToLogin.setOnClickListener {
            finish() // Dépile RegisterActivity → retour à LoginActivity
        }

        // ── Efface les erreurs dès que l'utilisateur retape ───────
        etEmail.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tilEmail.error = null }
        etPassword.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tilPassword.error = null }
        etConfirmPassword.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tilConfirmPassword.error = null }
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION LOCALE DES CHAMPS
    // ══════════════════════════════════════════════════════════════

    private fun validateInputs(): Boolean {
        var isValid = true

        // ── Email ─────────────────────────────────────────────────
        val email = etEmail.value()
        when {
            email.isEmpty() -> {
                tilEmail.error = "L'adresse email est requise"
                isValid = false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Format d'email invalide"
                isValid = false
            }
            else -> tilEmail.error = null
        }

        // ── Mot de passe (Strict Validation OWASP MASVS-AUTH-1) ────
        val password = etPassword.value()
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$".toRegex()

        when {
            password.isEmpty() -> {
                tilPassword.error = "Le mot de passe est requis"
                isValid = false
            }
            !password.matches(passwordPattern) -> {
                tilPassword.error = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial (@#$%^&+=!)"
                isValid = false
            }
            else -> tilPassword.error = null
        }

        // ── Confirmation mot de passe ─────────────────────────────
        val confirmPassword = etConfirmPassword.value()
        when {
            confirmPassword.isEmpty() -> {
                tilConfirmPassword.error = "Veuillez confirmer le mot de passe"
                isValid = false
            }
            confirmPassword != password -> {
                tilConfirmPassword.error = "Les mots de passe ne correspondent pas"
                isValid = false
            }
            else -> tilConfirmPassword.error = null
        }

        return isValid
    }

    // ══════════════════════════════════════════════════════════════
    // OBSERVATION DU STATE FLOW (Lifecycle-safe)
    // ══════════════════════════════════════════════════════════════

    private fun observeRegisterState() {
        viewModel.registerState.collectIn(this) { state ->
            when (state) {
                is UiState.Idle -> { }

                is UiState.Loading -> {
                    progressBar.show()
                    btnRegister.isEnabled = false
                    btnRegister.alpha     = 0.7f
                    tilEmail.error           = null
                    tilPassword.error        = null
                    tilConfirmPassword.error = null
                }

                is UiState.Success -> {
                    progressBar.hide()
                    btnRegister.isEnabled = true
                    btnRegister.alpha     = 1f
                    
                    // On affiche le message de succès avant de reset le state
                    toast("Compte créé avec succès ! Bienvenue 🎉")
                    
                    // Navigation sécurisée vers l'écran principal
                    navigateToMain()
                }

                is UiState.Error -> {
                    progressBar.hide()
                    btnRegister.isEnabled = true
                    btnRegister.alpha     = 1f
                    toast(state.message)
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private fun navigateToMain() {
        try {
            // Direct redirection to CalculatorActivity
            val intent = Intent(this@RegisterActivity, com.example.leanmassdriss.ui.calculator.CalculatorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            // Dispatch de l'intent avant toute destruction de l'activité
            startActivity(intent)
            
            // On reset le state du ViewModel APRES avoir lancé l'intent
            viewModel.resetRegisterState()
            
            // On termine proprement l'activité
            finish()
        } catch (e: Exception) {
            toast("Erreur lors de la redirection : ${e.message}")
        }
    }
}