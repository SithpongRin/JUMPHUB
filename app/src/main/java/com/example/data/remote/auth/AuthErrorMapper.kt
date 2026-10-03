package com.example.data.remote.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AuthErrorMapper {

    /**
     * Maps an authentication-related Exception to a human-readable English error message.
     * Guaranteed never to crash.
     */
    fun getUserMessage(throwable: Throwable, context: Context? = null): String {
        return when (throwable) {
            is NoCredentialException -> {
                context?.getString(R.string.auth_error_no_accounts)
                    ?: "No Google accounts found on this device."
            }

            is GetCredentialCancellationException -> {
                context?.getString(R.string.auth_error_cancelled)
                    ?: "Sign-in was cancelled."
            }

            is UnknownHostException,
            is SocketTimeoutException,
            is FirebaseNetworkException -> {
                context?.getString(R.string.auth_error_network)
                    ?: "Network unavailable. Please check your internet connection."
            }

            is IOException -> {
                val msg = throwable.message.orEmpty().lowercase()
                if (msg.contains("network") || msg.contains("timeout") || msg.contains("connection")) {
                    context?.getString(R.string.auth_error_network)
                        ?: "Network unavailable. Please check your internet connection."
                } else {
                    context?.getString(R.string.auth_error_generic)
                        ?: "Sign-in failed. Please try again."
                }
            }

            is GoogleIdTokenParsingException,
            is FirebaseAuthInvalidCredentialsException -> {
                context?.getString(R.string.auth_error_token)
                    ?: "Authentication token error. Please verify Google Play Services and try again."
            }

            is GetCredentialCustomException -> {
                val msg = throwable.message.orEmpty()
                if (msg.contains("sign_in_canceled", ignoreCase = true) || msg.contains("cancel", ignoreCase = true)) {
                    context?.getString(R.string.auth_error_cancelled)
                        ?: "Sign-in was cancelled."
                } else if (msg.contains("network", ignoreCase = true)) {
                    context?.getString(R.string.auth_error_network)
                        ?: "Network unavailable. Please check your internet connection."
                } else {
                    "Google Sign-In error: ${throwable.type}"
                }
            }

            is GetCredentialException -> {
                val msg = throwable.message.orEmpty()
                if (msg.contains("cancel", ignoreCase = true)) {
                    context?.getString(R.string.auth_error_cancelled)
                        ?: "Sign-in was cancelled."
                } else {
                    context?.getString(R.string.auth_error_generic)
                        ?: "Sign-in failed. Please try again."
                }
            }

            is FirebaseAuthInvalidUserException -> {
                "User account has been disabled or deleted."
            }

            is FirebaseAuthException -> {
                throwable.localizedMessage ?: "Firebase authentication failed."
            }

            is IllegalStateException -> {
                throwable.message ?: "Authentication service configuration error."
            }

            else -> {
                throwable.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: (context?.getString(R.string.auth_error_generic) ?: "Sign-in failed. Please try again.")
            }
        }
    }
}
