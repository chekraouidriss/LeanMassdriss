package com.example.leanmassdriss.data.repository

import com.example.leanmassdriss.data.local.dao.LbmRecordDao
import com.example.leanmassdriss.data.local.entity.LbmRecordEntity
import com.example.leanmassdriss.data.remote.FirestoreDataSource
import com.example.leanmassdriss.domain.model.LbmRecord
import com.example.leanmassdriss.utils.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date


class LbmRepository(
    private val dao: LbmRecordDao,
    private val remoteSource: FirestoreDataSource
) {

    // ── CREATE : Double persistance (Room EN PREMIER, puis Firestore) ──
    suspend fun saveRecord(
        poids: Double,
        taille: Double,
        isHomme: Boolean
    ): Result<LbmRecord> {
        return try {
            // 1. Calcul de la formule de Boer
            val lbmValue = if (isHomme) {
                (0.407 * poids) + (0.267 * taille) - 19.2
            } else {
                (0.252 * poids) + (0.473 * taille) - 48.3
            }

            // 2. Évaluation du statut selon les seuils ajustables du prof
            val statut = AppConfig.evaluerStatut(lbmValue, isHomme)

            // 3. Sauvegarde locale Room pour générer l'ID auto-incrémenté (localId)
            val entity = LbmRecordEntity(
                poids = poids,
                taille = taille,
                isHomme = isHomme,
                lbmValue = lbmValue,
                statut = statut
            )
            val generatedLocalId = dao.insertRecord(entity)

            // 4. Construction du modèle métier propre
            val record = LbmRecord(
                id = generatedLocalId.toString(),
                poids = poids,
                taille = taille,
                isHomme = isHomme,
                lbmValue = lbmValue,
                statut = statut,
                dateCalcul = Date(entity.timestamp)
            )

            // 5. Synchronisation Cloud Firebase (Best-effort en tâche de fond)
            remoteSource.saveRecord(record)

            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── READ : Récupération de l'historique depuis Room (Offline-First) ──
    fun getAllRecords(): Flow<List<LbmRecord>> {
        return dao.getAllRecords().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // ── DELETE : Suppression synchronisée local + distant ──
    suspend fun deleteRecord(record: LbmRecord): Result<Unit> {
        return try {
            // Suppression locale via l'ID
            dao.deleteRecordById(record.id.toLong())
            // Suppression Cloud
            remoteSource.deleteRecord(record.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Supprimer tout l'historique (ex: lors de la déconnexion)
    suspend fun deleteAllRecords(): Result<Unit> {
        return try {
            dao.clearAllRecords()
            remoteSource.deleteAllRecords()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


fun LbmRecordEntity.toDomainModel() = LbmRecord(
    id = localId.toString(),
    poids = poids,
    taille = taille,
    isHomme = isHomme,
    lbmValue = lbmValue,
    statut = statut,
    dateCalcul = Date(timestamp)
)