package com.example.leanmassdriss.data.remote

import com.example.leanmassdriss.domain.model.LbmRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date


class FirestoreDataSource {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    // ── Référence à la sous-collection de l'utilisateur connecté ──
    private fun userRecordsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore
            .collection("users")
            .document(uid)
            .collection("lbm_records")
    }

    // ── CREATE : Sauvegarde un enregistrement dans Firestore ───────
    suspend fun saveRecord(record: LbmRecord): Result<Unit> {
        return try {
            val collection = userRecordsCollection()
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val data = mapOf(
                "id"         to record.id,
                "poids"      to record.poids,
                "taille"     to record.taille,
                "isHomme"    to record.isHomme,
                "lbmValue"   to record.lbmValue,
                "statut"     to record.statut,
                "timestamp"  to record.dateCalcul.time
            )

            collection
                .document(record.id)
                .set(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── READ : Écoute en temps réel les enregistrements (Flow) ─────
    fun getRecordsStream(): Flow<List<LbmRecord>> = callbackFlow {
        val collection = userRecordsCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val records = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        LbmRecord(
                            id         = doc.getString("id") ?: "",
                            poids      = doc.getDouble("poids") ?: 0.0,
                            taille     = doc.getDouble("taille") ?: 0.0,
                            isHomme    = doc.getBoolean("isHomme") ?: true,
                            lbmValue   = doc.getDouble("lbmValue") ?: 0.0,
                            statut     = doc.getString("statut") ?: "",
                            dateCalcul = Date(doc.getLong("timestamp") ?: System.currentTimeMillis())
                        )
                    }.getOrNull()
                }
                trySend(records)
            }

        awaitClose { listener.remove() }
    }

    // ── DELETE : Supprime un enregistrement par son ID ─────────────
    suspend fun deleteRecord(recordId: String): Result<Unit> {
        return try {
            val collection = userRecordsCollection()
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            collection
                .document(recordId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── DELETE ALL : Supprime tous les enregistrements (deconnexion) ─
    suspend fun deleteAllRecords(): Result<Unit> {
        return try {
            val collection = userRecordsCollection()
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = collection.get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}