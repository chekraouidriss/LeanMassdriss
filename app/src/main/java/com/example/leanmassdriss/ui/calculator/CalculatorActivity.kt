package com.example.leanmassdriss.ui.calculator

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.leanmassdriss.R
import com.example.leanmassdriss.data.local.AppDatabase
import com.example.leanmassdriss.data.remote.FirestoreDataSource
import com.example.leanmassdriss.data.repository.LbmRepository
import com.example.leanmassdriss.databinding.ActivityCalculatorBinding
import com.example.leanmassdriss.domain.model.LbmRecord
import com.example.leanmassdriss.ui.auth.LoginActivity
import com.example.leanmassdriss.ui.history.HistoryActivity
import com.example.leanmassdriss.utils.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Écran principal du calculateur LBM.
 * Implémenté AVEC ViewBinding (contrainte professeur).
 */
class CalculatorActivity : AppCompatActivity() {

    // ── VIEWBINDING ───────────────────────────────────────────────
    private lateinit var binding: ActivityCalculatorBinding

    // ── VIEWMODEL ──────────────────────────────────────────────────
    private lateinit var viewModel: LbmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        //  OWASP MASVS-PLATFORM-4: Prevent screenshots and screen recording for privacy
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  Senior Architect Fix: Async initialization to prevent Main Thread freeze/crash
        lifecycleScope.launch(Dispatchers.IO) {
            val passphrase   = SecurityUtils.getDatabaseKeyHex(applicationContext)
            val dao          = AppDatabase.getInstance(applicationContext, passphrase).lbmRecordDao()
            val remoteSource = FirestoreDataSource()
            val repository   = LbmRepository(dao, remoteSource)
            
            withContext(Dispatchers.Main) {
                val factory = LbmViewModel.Factory(repository)
                viewModel = ViewModelProvider(this@CalculatorActivity, factory)[LbmViewModel::class.java]
                
                // Initialize UI Logic only when ViewModel is ready
                setupGenderSelector()
                setupListeners()
                observeCalculateState()
            }
        }
    }
    private fun setupGenderSelector() {
        highlightGender(isMale = true)

        binding.rbMale.setOnClickListener {
            binding.rbFemale.isChecked = false
            highlightGender(isMale = true)
        }

        binding.rbFemale.setOnClickListener {
            binding.rbMale.isChecked = false
            highlightGender(isMale = false)
        }

        binding.cardMale.setOnClickListener {
            binding.rbMale.isChecked = true
            binding.rbFemale.isChecked = false
            highlightGender(isMale = true)
        }

        binding.cardFemale.setOnClickListener {
            binding.rbFemale.isChecked = true
            binding.rbMale.isChecked = false
            highlightGender(isMale = false)
        }
    }

    private fun highlightGender(isMale: Boolean) {
        with(binding) {
            if (isMale) {
                cardMale.strokeColor = getColor(android.R.color.holo_blue_dark)
                cardMale.setCardBackgroundColor(getColor(android.R.color.white))
                cardFemale.strokeColor = getColor(android.R.color.darker_gray)
                cardFemale.setCardBackgroundColor(getColor(android.R.color.white))
            } else {
                cardFemale.strokeColor = getColor(android.R.color.holo_red_light)
                cardFemale.setCardBackgroundColor(getColor(android.R.color.white))
                cardMale.strokeColor = getColor(android.R.color.darker_gray)
                cardMale.setCardBackgroundColor(getColor(android.R.color.white))
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // LISTENERS
    // ══════════════════════════════════════════════════════════════

    private fun setupListeners() {

        // Bouton Calculer + Sauvegarder
        binding.btnCalculate.setOnClickListener {
            if (validateInputs()) {
                val poids   = binding.etWeight.toDoubleOrNull() ?: return@setOnClickListener
                val taille  = binding.etHeight.toDoubleOrNull() ?: return@setOnClickListener
                val isHomme = binding.rbMale.isChecked

                viewModel.calculateAndSave(poids, taille, isHomme)
            }
        }

        // Bouton Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Bouton Historique
        binding.btnGoToHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Efface les erreurs au focus
        binding.etWeight.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilWeight.error = null }
        binding.etHeight.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilHeight.error = null }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val weightText = binding.etWeight.value()
        val weight     = weightText.toDoubleOrNull()
        when {
            weightText.isEmpty() -> {
                binding.tilWeight.error = "Le poids est requis"
                isValid = false
            }
            weight == null -> {
                binding.tilWeight.error = "Valeur numérique invalide"
                isValid = false
            }
            else -> binding.tilWeight.error = null
        }

        val heightText = binding.etHeight.value()
        val height     = heightText.toDoubleOrNull()
        when {
            heightText.isEmpty() -> {
                binding.tilHeight.error = "La taille est requise"
                isValid = false
            }
            height == null -> {
                binding.tilHeight.error = "Valeur numérique invalide"
                isValid = false
            }
            else -> binding.tilHeight.error = null
        }

        return isValid
    }

    private fun observeCalculateState() {
        viewModel.calculateState.collectIn(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    setBtnCalculateEnabled(true)
                }
                is UiState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.cardResult.visibility = android.view.View.GONE
                    setBtnCalculateEnabled(false)
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    setBtnCalculateEnabled(true)
                    displayResult(state.data)
                    viewModel.resetCalculateState()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    setBtnCalculateEnabled(true)
                    binding.root.snack(state.message)
                }
            }
        }
    }

    private fun displayResult(record: LbmRecord) {
        with(binding) {
            tvLbmValue.text = "${record.lbmValue.format2()} kg"
            tvResultStatus.text = record.statut

            val genreStr = if (record.isHomme) "♂ Homme" else "♀ Femme"
            tvResultDetail.text = "Données : ${record.poids}kg | ${record.taille}cm | $genreStr"

            if (record.statut.contains("✅")) {
                tvResultIcon.text   = "✅"
                tvResultStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                cardResult.strokeColor = getColor(android.R.color.holo_green_light)
            } else {
                tvResultIcon.text   = "⚠️"
                tvResultStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                cardResult.strokeColor = getColor(android.R.color.holo_orange_light)
            }

            cardResult.setCardBackgroundColor(getColor(android.R.color.white))
            cardResult.alpha = 0f
            cardResult.show()
            cardResult.animate().alpha(1f).setDuration(400).start()
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Oui") { _, _ -> performLogout() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setBtnCalculateEnabled(enabled: Boolean) {
        binding.btnCalculate.isEnabled = enabled
        binding.btnCalculate.alpha     = if (enabled) 1f else 0.7f
    }
}
