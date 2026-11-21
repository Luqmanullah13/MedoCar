package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AdminRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthSessionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "luqmankhan89lk23@gmail.com"
    }

    private fun requireUser(): FirebaseUser {
        return auth.currentUser
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun isBootstrapAdminUser(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.isEmailVerified && user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun ensureUserProfileInitialized(): Result<Boolean> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val uid = user.uid
        val userRef = db.collection("users").document(uid)
        val adminRef = db.collection("admins").document(uid)
        return try {
            val isBootstrap = isBootstrapAdminUser(user)
            val adminSnap = adminRef.get().await()
            val isAdmin = isBootstrap || adminSnap.exists()

            if (isBootstrap && !adminSnap.exists()) {
                adminRef.set(
                    mapOf(
                        "uid" to uid,
                        "email" to (user.email ?: BOOTSTRAP_ADMIN_EMAIL),
                        "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "DocMeds Pharmacist"),
                        "roleLabel" to "Lead Pharmacist & Admin",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val profileSnap = userRef.get().await()
            if (!profileSnap.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "DocMeds Customer"),
                        "email" to (user.email ?: "customer@docmeds.pk"),
                        "preferredLanguage" to "en",
                        "role" to if (isAdmin) UserRole.ADMIN.name else UserRole.CUSTOMER.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (isAdmin && profileSnap.getString("role") != UserRole.ADMIN.name) {
                userRef.update(
                    mapOf(
                        "role" to UserRole.ADMIN.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(isAdmin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emit(null)
        db.collection("users").document(uid)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeIsAdmin(uid: String): Flow<Boolean> = flow {
        val user = auth.currentUser
        val bootstrap = user?.uid == uid && isBootstrapAdminUser(user)
        if (bootstrap) {
            emit(true)
        }
        val path = "admins/$uid"
        db.collection("admins").document(uid)
            .snapshots()
            .map { snap -> snap.exists() || bootstrap }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                emit(bootstrap)
            }
            .collect { emit(it) }
    }

    suspend fun updateProfile(displayName: String, phone: String?, preferredLanguage: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            require(displayName.trim().length in 1..80) { "Name must be 1-80 characters." }
            val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanPhone != null) require(cleanPhone.length <= 30) { "Phone too long." }
            val lang = if (preferredLanguage == "ur") "ur" else "en"
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim(),
                "preferredLanguage" to lang,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhone != null) updates["phone"] = cleanPhone
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeAllCustomers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllAdmins(): Flow<List<AdminRecord>> =
        db.collection("admins")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun grantAdminRole(user: UserProfile, roleLabel: String = "Licensed Pharmacist"): Result<Unit> {
        return try {
            requireUser()
            val batch = db.batch()
            val adminRef = db.collection("admins").document(user.userId)
            val userRef = db.collection("users").document(user.userId)
            batch.set(
                adminRef,
                mapOf(
                    "uid" to user.userId,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "roleLabel" to roleLabel.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "role" to UserRole.ADMIN.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admins/${user.userId}")
            Result.failure(e)
        }
    }

    suspend fun revokeAdminRole(targetUid: String): Result<Unit> {
        return try {
            val current = requireUser()
            check(current.uid != targetUid) { "Cannot revoke your own active admin access." }
            val batch = db.batch()
            batch.delete(db.collection("admins").document(targetUid))
            batch.update(
                db.collection("users").document(targetUid),
                mapOf(
                    "role" to UserRole.CUSTOMER.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "admins/$targetUid")
            Result.failure(e)
        }
    }
}
