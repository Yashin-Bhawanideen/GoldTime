package com.goldtime.app.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/** All Firebase Authentication (email + password) logic lives here. */
object AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * 1. Creates the Firebase Auth user.
     * 2. Sends the profile (name, phone) to the API, which stores it in Firestore.
     * If step 2 fails the new auth user is removed so the account isn't half-created.
     */
    suspend fun register(
        firstName: String,
        surname: String,
        email: String,
        phone: String,
        password: String
    ) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw ApiException("Account creation failed.")
        try {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName("$firstName $surname").build()
            ).await()
            val token = user.getIdToken(true).await().token
                ?: throw ApiException("Could not get a login token.")
            ApiClient.registerProfile(token, firstName, surname, email, phone)
        } catch (e: Exception) {
            runCatching { user.delete().await() }
            auth.signOut()
            throw e
        }
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun idToken(): String =
        auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw ApiException("Not signed in.")

    fun signOut() = auth.signOut()
}

fun Throwable.toFriendlyMessage(): String = when (this) {
    is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 8 characters."
    is FirebaseAuthInvalidUserException -> "No account found with that email."
    is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    is FirebaseNetworkException -> "No internet connection. Please try again."
    is ApiException -> message ?: "Server error. Please try again."
    is java.io.IOException -> "Can't reach the server. Please check your connection."
    else -> "Something went wrong. Please try again."
}
