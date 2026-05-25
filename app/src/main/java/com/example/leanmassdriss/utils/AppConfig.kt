package com.example.leanmassdriss.utils

object AppConfig {
    // Seuils par défaut (ajustables au besoin pendant l'exécution)
    var SEUIL_HOMME: Double = 38.0
    var SEUIL_FEMME: Double = 24.0

    /**
     * Méthode pour valider le statut LBM de l'utilisateur
     */
    fun evaluerStatut(lbmValue: Double, isHomme: Boolean): String {
        val seuilAjuste = if (isHomme) SEUIL_HOMME else SEUIL_FEMME
        return if (lbmValue >= seuilAjuste) {
            "✅ Normal (Supérieur ou égal au seuil de $seuilAjuste kg)"
        } else {
            "⚠️ Insuffisant (Inférieur au seuil de $seuilAjuste kg)"
        }
    }
}