package com.iiitnr.inventoryapp.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class GoogleSignInHelper(
    private val context: Context, private val webClientId: String
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): String? {
        if (webClientId.isBlank()) {
            android.util.Log.e("GoogleSignInHelper", "Web Client ID is not configured")
            return null
        }

        android.util.Log.d(
            "GoogleSignInHelper",
            "Starting Google Sign-In with Web Client ID: ${webClientId.take(20)}..."
        )

        val nonce = generateNonce()

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = webClientId
        ).setNonce(nonce).build()

        val request =
            GetCredentialRequest.Builder().addCredentialOption(signInWithGoogleOption).build()

        return try {
            val result = credentialManager.getCredential(
                request = request, context = context
            )
            handleSignInResult(result)
        } catch (_: NoCredentialException) {
            null
        } catch (e: GetCredentialException) {
            android.util.Log.e(
                "GoogleSignInHelper", "GetCredentialException: ${e.type} - ${e.message}", e
            )
            null
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "Unexpected error during Google Sign-In", e)
            null
        }
    }

    private fun handleSignInResult(result: androidx.credentials.GetCredentialResponse): String? {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        android.util.Log.d(
                            "GoogleSignInHelper", "Successfully obtained Google ID token"
                        )
                        googleIdTokenCredential.idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        android.util.Log.e(
                            "GoogleSignInHelper", "Received an invalid google id token response", e
                        )
                        null
                    }
                } else {
                    android.util.Log.e(
                        "GoogleSignInHelper", "Unexpected type of credential: ${credential.type}"
                    )
                    null
                }
            }

            else -> {
                android.util.Log.e(
                    "GoogleSignInHelper",
                    "Unexpected type of credential: ${credential::class.simpleName}"
                )
                null
            }
        }
    }

    private fun generateNonce(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars.random() }.joinToString("")
    }
}
