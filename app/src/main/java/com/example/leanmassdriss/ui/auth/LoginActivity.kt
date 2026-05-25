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
import com.example.leanmassdriss.utils.UiState
import com.example.leanmassdriss.utils.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class LoginActivity : AppCompatActivity() {

    // ── ViewModel ──────────────────────────────────────────────────
    private val viewModel: AuthViewModel by viewModels()

    // ── Vues (liées via findViewById — PAS de ViewBinding) ─────────
    private lateinit var tilEmail      : TextInputLayout
    private lateinit var tilPassword   : TextInputLayout
    private lateinit var etEmail        : TextInputEditText
    private lateinit var etPassword     : TextInputEditText
    private lateinit var btnLogin      : Button
    private lateinit var progressBar   : ProgressBar
    private lateinit var tvGoToRegister: TextView

    // ══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── SÉCURISATION DU CYCLE DE VIE AU DÉMARRAGE (BOOT-LOOP SHIELD) ──
        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser
            
            if (currentUser != null) {
                // Vérification stricte de l'intégrité de la session
                if (currentUser.uid.isNotEmpty() && currentUser.email != null) {
                    navigateToMain()
                    return
                } else {
                    // Session corrompue ou incomplète -> Nettoyage immédiat
                    firebaseAuth.signOut()
                }
            }
        } catch (e: Exception) {
            // En cas d'erreur fatale lors de l'init (SDK crash, corruption cache)
            // On tente un reset de force avant que l'app ne "brick"
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (inner: Exception) {}
        }

        setContentView(R.layout.activity_login)

        bindViews()
        setupListeners()
        observeLoginState()
    }

    // ══════════════════════════════════════════════════════════════
    // LIAISON DES VUES — findViewById (contrainte sans ViewBinding)
    // ══════════════════════════════════════════════════════════════

    private fun bindViews() {
        tilEmail       = findViewById(R.id.tilEmail)
        tilPassword    = findViewById(R.id.tilPassword)
        etEmail        = findViewById(R.id.etEmail)
        etPassword     = findViewById(R.id.etPassword)
        btnLogin       = findViewById(R.id.btnLogin)
        progressBar    = findViewById(R.id.progressBar)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
    }

    // ══════════════════════════════════════════════════════════════
    // LISTENERS
    // ══════════════════════════════════════════════════════════════

    private fun setupListeners() {

        // ── Bouton Se connecter ───────────────────────────────────
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                viewModel.login(
                    email    = etEmail.value(),
                    password = etPassword.value()
                )
            }
        }

        // ── Lien vers l'inscription ───────────────────────────────
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // ── Efface l'erreur dès que l'utilisateur retape ─────────
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilEmail.error = null
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION LOCALE DES CHAMPS
    // ══════════════════════════════════════════════════════════════

    private fun validateInputs(): Boolean {
        var isValid = true

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

        val password = etPassword.value()
        when {
            password.isEmpty() -> {
                tilPassword.error = "Le mot de passe est requis"
                isValid = false
            }
            password.length < 6 -> {
                tilPassword.error = "Minimum 6 caractères"
                isValid = false
            }
            else -> tilPassword.error = null
        }

        return isValid
    }

    // ══════════════════════════════════════════════════════════════
    // OBSERVATION DU STATE FLOW (Lifecycle-safe)
    // ══════════════════════════════════════════════════════════════

    private fun observeLoginState() {
        viewModel.loginState.collectIn(this) { state ->
            when (state) {
                is UiState.Idle -> { }

                is UiState.Loading -> {
                    progressBar.show()
                    btnLogin.isEnabled = false
                    btnLogin.alpha     = 0.7f
                    tilEmail.error      = null
                    tilPassword.error   = null
                }

                is UiState.Success -> {
                    progressBar.hide()
                    btnLogin.isEnabled = true
                    btnLogin.alpha     = 1f
                    
                    // On affiche une petite confirmation (Optionnel mais recommandé)
                    toast("Connexion réussie !")
                    
                    // Reset de l'état du ViewModel AVANT la navigation
                    viewModel.resetLoginState()
                    
                    // Navigation
                    navigateToMain()
                }

                is UiState.Error -> {
                    progressBar.hide()
                    btnLogin.isEnabled = true
                    btnLogin.alpha     = 1f
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
            // Direct redirection to CalculatorActivity to avoid intermediate loops
            val intent = Intent(this@LoginActivity, com.example.leanmassdriss.ui.calculator.CalculatorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            viewModel.resetLoginState()
            finish()
        } catch (e: Exception) {
            toast("Erreur de navigation : ${e.message}")
        }
    }
}