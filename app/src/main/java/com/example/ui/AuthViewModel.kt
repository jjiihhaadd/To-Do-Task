package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    
    private val _user = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): String? {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            // Generate a nonce
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Sign-in failed", e)
            if (e.message?.contains("No credential") == true || e.message?.contains("No candidat") == true || e.javaClass.simpleName.contains("NoCredentialException")) {
                "No Google accounts found on this device. Please sign in to a Google account in the device Settings or Play Store first."
            } else {
                e.message ?: "Unknown error"
            }
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): String? {
        val credential = result.credential
        if (credential !is CustomCredential) {
            return "Not a CustomCredential"
        }
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth?.signInWithCredential(firebaseCredential)?.await()
                return if (auth != null) null else "FirebaseAuth is null"
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase auth failed", e)
                return "Firebase auth failed: ${e.message}"
            }
        }
        return "Unknown credential type"
    }

    fun signOut() {
        auth?.signOut()
    }
}
