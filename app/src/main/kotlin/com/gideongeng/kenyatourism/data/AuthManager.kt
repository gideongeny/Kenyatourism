package com.gideongeng.kenyatourism.data

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isLoggedIn.value = firebaseAuth.currentUser != null
        }
    }

    /**
     * Returns a configured GoogleSignInClient for launching the sign-in intent.
     * Call getSignInIntent() on the returned client and launch it with ActivityResultLauncher.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val webClientId = context.getString(
            context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
        )
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Call this after receiving the result from the Google Sign-In intent.
     * Pass in the data Intent from onActivityResult.
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser?> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user?.let { syncUserToFirestore(it) }
            Result.success(authResult.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { syncUserToFirestore(it) }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { syncUserToFirestore(it) }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(context: Context? = null) {
        context?.let { getGoogleSignInClient(it).signOut() }
        auth.signOut()
    }

    private suspend fun syncUserToFirestore(user: FirebaseUser) {
        try {
            val userDoc = firestore.collection("users").document(user.uid)
            val data = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to System.currentTimeMillis()
            )
            userDoc.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    suspend fun syncFavoritesToCloud(favoriteIds: Set<Int>) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid)
                .update("favorites", favoriteIds.toList()).await()
        } catch (_: Exception) {
            try {
                firestore.collection("users").document(user.uid)
                    .set(hashMapOf("favorites" to favoriteIds.toList()), com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (_: Exception) { }
        }
    }

    suspend fun loadFavoritesFromCloud(): Set<Int>? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("favorites") as? List<Long>
            list?.map { it.toInt() }?.toSet()
        } catch (_: Exception) {
            null
        }
    }
}
