package com.example.leanmassdriss.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lbm_records")
data class LbmRecordEntity(
    @PrimaryKey(autoGenerate = true)
    var localId: Long = 0,
    val firebaseId: String = "",
    val poids: Double,
    val taille: Double,
    val isHomme: Boolean,
    val lbmValue: Double,
    val statut: String,
    val timestamp: Long = System.currentTimeMillis()
)