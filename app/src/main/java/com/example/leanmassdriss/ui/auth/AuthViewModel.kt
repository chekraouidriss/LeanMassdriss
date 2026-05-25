package com.example.leanmassdriss.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leanmassdriss.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel gérant toute la logique d'authentification Firebase.
 *
 * Expose deux StateFlow indépendants :
 *  - [loginState]    → observé par LoginActivity
 *  - [registerState] → observé par RegisterActivity
 *
 * Les messages d'erreur sont traduits en français pour l'utilisateur final.
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()



    // UiState<String> : String = uid de l'utilisateur connecté
    private val _loginState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val loginState: StateFlow<UiState<String>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val registerState: StateFlow<UiState<String>> = _registerState.asStateFlow()

    private fun isEmailValid(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isPasswordValid(password: String): Boolean = password.length >= 6



    /**
     * Connecte un utilisateur existant via Firebase Auth.
     *
     * @param email    Email saisi par l'utilisateur.
     * @param password Mot de passe saisi par l'utilisateur.
     */
    fun login(email: String, password: String) {

        // ── Validation locale avant appel réseau ──────────────────
        if (!isEmailValid(email)) {
            _loginState.value = UiState.Error("Adresse email invalide.")
            return
        }
        if (password.isEmpty()) {
            _loginState.value = UiState.Error("Le mot de passe est requis.")
            return
        }

        // ── Appel Firebase ────────────────────────────────────────
        _loginState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val result = auth
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val uid = result.user?.uid
                    ?: throw Exception("Identifiant utilisateur introuvable.")

                _loginState.value = UiState.Success(uid)

            } catch (e: Exception) {
                _loginState.value = UiState.Error(mapFirebaseError(e))
            }
        }
    }



    /**
     * Crée un nouveau compte utilisateur via Firebase Auth.
     *
     * @param email           Email choisi par l'utilisateur.
     * @param password        Mot de passe choisi.
     * @param confirmPassword Confirmation du mot de passe.
     */
    fun register(email: String, password: String, confirmPassword: String) {

        // ── Validations locales ───────────────────────────────────
        if (!isEmailValid(email)) {
            _registerState.value = UiState.Error("Adresse email invalide.")
            return
        }
        if (!isPasswordValid(password)) {
            _registerState.value = UiState.Error(
                "Le mot de passe doit contenir au moins 6 caractères."
            )
            return
        }
        if (password != confirmPassword) {
            _registerState.value = UiState.Error(
                "Les mots de passe ne correspondent pas."
            )
            return
        }

        // ── Appel Firebase ────────────────────────────────────────
        _registerState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val result = auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val uid = result.user?.uid
                    ?: throw Exception("Identifiant utilisateur introuvable.")

                _registerState.value = UiState.Success(uid)

            } catch (e: Exception) {
                _registerState.value = UiState.Error(mapFirebaseError(e))
            }
        }
    }



    /**
     * Déconnecte l'utilisateur et remet les états à Idle.
     * À appeler depuis MainActivity avant de revenir au Login.
     */
    fun logout() {
        auth.signOut()
        resetStates()
    }

    /**
     * Vérifie si un utilisateur est déjà connecté (session persistante).
     * @return true si Firebase a une session active.
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // ══════════════════════════════════════════════════════════════
    // RESET STATES
    // ══════════════════════════════════════════════════════════════

    /**
     * Remet loginState à Idle.
     * Utile pour éviter de rejouer la navigation si l'Activity est recréée
     * (rotation écran).
     */
    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    /**
     * Remet registerState à Idle.
     */
    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }

    private fun resetStates() {
        _loginState.value    = UiState.Idle
        _registerState.value = UiState.Idle
    }

    // ══════════════════════════════════════════════════════════════
    // MAPPING DES ERREURS FIREBASE → Messages français
    // ══════════════════════════════════════════════════════════════

    /**
     * Traduit les exceptions Firebase en messages lisibles en français.
     *
     * Couvre les cas les plus fréquents :
     *  - Mot de passe trop court
     *  - Email déjà utilisé
     *  - Identifiants incorrects
     *  - Utilisateur introuvable
     *  - Réseau indisponible
     */
    private fun mapFirebaseError(e: Exception): String {
        return when (e) {

            // ── Erreurs mot de passe ──────────────────────────────
            is FirebaseAuthWeakPasswordException ->
                "Mot de passe trop faible. Utilisez au moins 6 caractères."

            // ── Email déjà enregistré ─────────────────────────────
            is FirebaseAuthUserCollisionException ->
                "Cette adresse email est déjà associée à un compte."

            // ── Email ou mot de passe incorrect ───────────────────
            is FirebaseAuthInvalidCredentialsException ->
                "Email ou mot de passe incorrect. Veuillez réessayer."

            // ── Compte introuvable ou désactivé ───────────────────
            is FirebaseAuthInvalidUserException -> {
                when (e.errorCode) {
                    "ERROR_USER_DISABLED"   ->
                        "Ce compte a été désactivé. Contactez le support."
                    "ERROR_USER_NOT_FOUND"  ->
                        "Aucun compte trouvé avec cette adresse email."
                    else                    ->
                        "Compte utilisateur invalide. Veuillez vous réinscrire."
                }
            }

            // ── Erreurs génériques Firebase ───────────────────────
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_NETWORK_REQUEST_FAILED" ->
                        "Pas de connexion internet. Vérifiez votre réseau."
                    "ERROR_TOO_MANY_REQUESTS"      ->
                        "Trop de tentatives. Réessayez dans quelques minutes."
                    "ERROR_OPERATION_NOT_ALLOWED"  ->
                        "Connexion par email non activée. Contactez l'administrateur."
                    else ->
                        "Erreur d'authentification : ${e.errorCode}"
                }
            }

            // ── Erreur réseau générique ───────────────────────────
            else -> {
                if (e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("NETWORK", ignoreCase = true) == true) {
                    "Pas de connexion internet. Vérifiez votre réseau."
                } else {
                    "Une erreur inattendue est survenue. Veuillez réessayer."
                }
            }
        }
    }
}