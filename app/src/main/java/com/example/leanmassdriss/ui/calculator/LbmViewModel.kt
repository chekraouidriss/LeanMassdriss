package com.example.leanmassdriss.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.leanmassdriss.data.repository.LbmRepository
import com.example.leanmassdriss.domain.model.LbmRecord
import com.example.leanmassdriss.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LbmViewModel(
    private val repository: LbmRepository
) : ViewModel() {

    // ── STATE FLOW — Résultat du calcul en cours ──────────────────
    private val _calculateState = MutableStateFlow<UiState<LbmRecord>>(UiState.Idle)
    val calculateState: StateFlow<UiState<LbmRecord>> = _calculateState.asStateFlow()

    // ── STATE FLOW — Liste complète émise depuis Room ─────────────
    val allRecords: StateFlow<List<LbmRecord>> =
        repository.getAllRecords()
            .stateIn(
                scope         = viewModelScope,
                started       = SharingStarted.WhileSubscribed(5_000),
                initialValue  = emptyList()
            )

    // ── CALCUL + SAUVEGARDE SYNCHRONISÉE ──────────────────────────
    fun calculateAndSave(poids: Double, taille: Double, isHomme: Boolean) {
        // Validation des plages physiologiques avant calcul
        val validationError = validateInputs(poids, taille)
        if (validationError != null) {
            _calculateState.value = UiState.Error(validationError)
            return
        }

        _calculateState.value = UiState.Loading

        //  FORCE LE DÉMARRAGE SUR DISPATCHERS.IO POUR ÉVITER LE DEADLOCK ÉMULATEUR
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1 & 2. Calcul et sauvegarde Room/Firestore (Strictly Background)
                val result = repository.saveRecord(
                    poids = poids,
                    taille = taille,
                    isHomme = isHomme
                )

                // 3. FORCE LE RETOUR SUR LE MAIN THREAD POUR LA MISE À JOUR UI
                withContext(Dispatchers.Main) {
                    _calculateState.value = when {
                        result.isSuccess -> UiState.Success(result.getOrThrow())
                        else             -> UiState.Error(mapSaveError(result.exceptionOrNull()))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _calculateState.value = UiState.Error("Erreur fatale : ${e.message}")
                }
            }
        }
    }

    fun deleteRecord(record: LbmRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun resetCalculateState() {
        _calculateState.value = UiState.Idle
    }

    // ── VALIDATION MÉTIER LOCALE ──────────────────────────────────
    private fun validateInputs(poids: Double, taille: Double): String? {
        return when {
            poids <= 0 -> "Le poids doit être supérieur à 0 kg."
            poids < 20 -> "Le poids saisi ($poids kg) semble trop faible. Vérifiez la valeur."
            poids > 300 -> "Le poids saisi ($poids kg) dépasse la limite acceptée (300 kg)."
            taille <= 0 -> "La taille doit être supérieure à 0 cm."
            taille < 100 -> "La taille saisie ($taille cm) semble trop faible. Vérifiez la valeur."
            taille > 250 -> "La taille saisie ($taille cm) dépasse la limite acceptée (250 cm)."
            else -> null
        }
    }

    private fun mapSaveError(e: Throwable?): String {
        return when {
            e == null -> "Erreur inconnue lors de la sauvegarde."
            e.message?.contains("UNIQUE", ignoreCase = true) == true -> "Cet enregistrement existe déjà."
            e.message?.contains("network", ignoreCase = true) == true -> "Pas de connexion cloud. Sauvegardé localement."
            else -> "Erreur lors de la sauvegarde : ${e.message ?: "inconnue"}"
        }
    }

    // ── FACTORY POUR INJECTION MANUELLE ───────────────────────────
    class Factory(
        private val repository: LbmRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LbmViewModel::class.java)) {
                return LbmViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
        }
    }
}