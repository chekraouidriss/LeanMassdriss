# LeanMassDriss - Application Mobile Hardening (OWASP MASVS)

[![Android SDK](https://img.shields.io/badge/SDK-36%20%28Android%2015%2F16%29-green.svg)](https://developer.android.com)
[![OWASP MASVS](https://img.shields.io/badge/Security-OWASP%20MASVS%20Compliant-blue.svg)](https://mas.owasp.org/MASVS/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2F%20Clean%20Code-orange.svg)](https://developer.android.com/topic/architecture)

**LeanMassDriss** est une application Android native de suivi anthropométrique et de calcul de la masse maigre. Initialement développée pour répondre aux besoins de suivi de santé, cette application a subi un audit de sécurité complet et un processus de **durcissement binaire et structurel (Hardening)** basé rigoureusement sur le standard international **OWASP MASVS** (Mobile Application Security Verification Standard).

---

## 🛠️ Stack Technique de Développement

L'application est construite sur les standards modernes du développement Android :
* **Langage :** Kotlin (100%) avec gestion des cycles de vie asynchrones via les Coroutines.
* **Architecture :** MVVM (Model-View-ViewModel) respectant les principes du Clean Code pour une isolation stricte de la logique métier.
* **Composants Jetpack :** Architecture Room (SQLite local), ViewModel, LiveData et ViewBinding.
* **Services Cloud :** Intégration de Firebase Authentication pour la gestion dynamique des sessions utilisateurs.

---

## 🛡️ Matrice de Sécurisation (Implémentation OWASP MASVS)

Le tableau suivant synthétise les vulnérabilités majeures identifiées lors de l'audit initial et les contre-mesures de sécurité implémentées dans la version actuelle :

| Référence OWASP MASVS | Composant Vulnérable | Risque & Impact (Avant) | Solution Technique Appliquée (Après) |
| :--- | :--- | :--- | :--- |
| **MASVS-STORAGE-1** <br>*(Storage Hardening)* | Base de données locale `Room SQLite` | Historique des calculs (`LbmRecord`) stocké en clair. Fichier `.db` extractible via *ADB pull* ou forensic (*Andriller*) sur appareils rootés. | Intégration de **SQLCipher (AES-256)**. Isolation de la base sous `lean_mass_v4_secure` avec dérivation et injection dynamique de clé via la `SupportFactory`. |
| **MASVS-AUTH-1** <br>*(Authentication Policy)* | Interface d'inscription `SignUpActivity` | Acceptation de mots de passe triviaux (`123456`, `abc`) par Firebase Auth. Risque élevé de *Credential Stuffing* et force brute. | Implémentation d'une **validation par Expression Régulière (Regex)** stricte côté client : Minimum 8 caractères, présence d'une Majuscule, une Minuscule, un Chiffre et un Caractère spécial. |
| **MASVS-PLATFORM-4** <br>*(Platform Interaction)* | Écrans de données médicales | Fuite d'informations sensibles (poids, taille) par capture d'écran frauduleuse ou application malveillante lisant le `READ_FRAME_BUFFER`. | Injection dynamique du flag système de sécurité **`WindowManager.LayoutParams.FLAG_SECURE`** dans les activités sensibles (`CalculatorActivity`, `HistoryActivity`). |
| **MASVS-RESILIENCE-3** <br>*(Code Anti-Reversing)* | Logs de débogage globaux `android.util.Log` | Fuite de données critiques (UID Firebase, métriques de santé) visibles en temps réel par un attaquant externe via la commande `adb logcat`. | Création d'un utilitaire centralisé **`SecureLogger`** conditionné à l'état du build (`BuildConfig.DEBUG`). Injection d'une règle de nettoyage de bytecode stricte dans **ProGuard** (`-assumenosideeffects`) pour éradiquer les logs résiduels en production. |

---

## 🔍 Focus Technique : Suppression Automatique des Logs f la Release

Pour valider l'impact de la règle **MASVS-RESILIENCE-3**, l'optimiseur ProGuard a été configuré via le fichier `app/proguard-rules.pro` comme suit :

```proguard
# OWASP MASVS-RESILIENCE-3: Automated production log stripping implementation
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
