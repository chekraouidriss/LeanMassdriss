package com.example.leanmassdriss.domain.model

import java.util.Date

data class LbmRecord(
    val id: String = "",
    val poids: Double,
    val taille: Double,
    val isHomme: Boolean,
    val lbmValue: Double,
    val statut: String,
    val dateCalcul: Date = Date()
)