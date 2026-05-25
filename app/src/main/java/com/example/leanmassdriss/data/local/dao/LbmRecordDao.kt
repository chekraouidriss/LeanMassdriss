package com.example.leanmassdriss.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.leanmassdriss.data.local.entity.LbmRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LbmRecordDao {
    // 1. Sauvegarder un calcul localement
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LbmRecordEntity): Long

    // 2. Récupérer l'historique complet trié du plus récent au plus ancien (Flow pour le temps réel)
    @Query("SELECT * FROM lbm_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<LbmRecordEntity>>

    // 3. Supprimer un enregistrement spécifique de l'historique
    @Query("DELETE FROM lbm_records WHERE localId = :id")
    suspend fun deleteRecordById(id: Long)

    // 4. Supprimer tout l'historique (Utile si déconnexion)
    @Query("DELETE FROM lbm_records")
    suspend fun clearAllRecords()
}