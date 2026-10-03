package com.example.data.remote.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthErrorMapperTest {

    @Test
    fun `maps NoCredentialException to clear English message`() {
        val exception = NoCredentialException("No credential found")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("No Google accounts found on this device.", message)
    }

    @Test
    fun `maps GetCredentialCancellationException to user cancelled message`() {
        val exception = GetCredentialCancellationException("Activity was cancelled")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Sign-in was cancelled.", message)
    }

    @Test
    fun `maps UnknownHostException to network error message`() {
        val exception = UnknownHostException("Unable to resolve host")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Network unavailable. Please check your internet connection.", message)
    }

    @Test
    fun `maps FirebaseNetworkException to network error message`() {
        val exception = FirebaseNetworkException("Network error occurred")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Network unavailable. Please check your internet connection.", message)
    }

    @Test
    fun `maps GoogleIdTokenParsingException to token error message`() {
        val exception = GoogleIdTokenParsingException(null)
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Authentication token error. Please verify Google Play Services and try again.", message)
    }

    @Test
    fun `maps FirebaseAuthInvalidCredentialsException to token error message`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "The credential is bad")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Authentication token error. Please verify Google Play Services and try again.", message)
    }

    @Test
    fun `maps FirebaseAuthInvalidUserException to account disabled message`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "User is disabled")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("User account has been disabled or deleted.", message)
    }

    @Test
    fun `maps custom cancellation exception message`() {
        val exception = GetCredentialCustomException("custom", "sign_in_canceled by user")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertEquals("Sign-in was cancelled.", message)
    }

    @Test
    fun `maps unknown exception safely without crashing`() {
        val exception = RuntimeException("Something unexpected happened")
        val message = AuthErrorMapper.getUserMessage(exception)
        assertTrue(message.isNotBlank())
        assertEquals("Something unexpected happened", message)
    }
}
