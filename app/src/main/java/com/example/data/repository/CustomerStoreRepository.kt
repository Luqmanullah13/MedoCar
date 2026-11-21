package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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

class CustomerStoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        val path = "users/$userId/cart"
        db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snap -> snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        val path = "users/$userId/wishlist"
        db.collection("users").document(userId).collection("wishlist")
            .snapshots()
            .map { snap -> snap.toObjects(WishlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAddresses(userId: String): Flow<List<DeliveryAddress>> = flow {
        val path = "users/$userId/addresses"
        db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snap ->
                snap.toObjects(DeliveryAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.defaultAddress }
            }
            .catch { error ->
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addToCart(productId: String, maxStock: Long, delta: Long = 1L): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            require(maxStock > 0L) { "Product is out of stock." }
            val ref = db.collection("users").document(uid).collection("cart").document(productId)
            val snap = ref.get().await()
            if (snap.exists()) {
                val currentQty = snap.getLong("quantity") ?: 1L
                val nextQty = (currentQty + delta).coerceIn(1L, minOf(99L, maxStock))
                ref.update(
                    mapOf(
                        "quantity" to nextQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val initialQty = delta.coerceIn(1L, minOf(99L, maxStock))
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "productId" to productId,
                        "quantity" to initialQty,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(productId: String, newQty: Long, maxStock: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            val ref = db.collection("users").document(uid).collection("cart").document(productId)
            if (newQty <= 0L) {
                ref.delete().await()
            } else {
                val bounded = newQty.coerceIn(1L, minOf(99L, maxStock.coerceAtLeast(1L)))
                ref.update(
                    mapOf(
                        "quantity" to bounded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(productId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/cart/$productId"
        return try {
            db.collection("users").document(uid).collection("cart").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleWishlist(productId: String, currentlyWishlisted: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/wishlist/$productId"
        return try {
            val ref = db.collection("users").document(uid).collection("wishlist").document(productId)
            if (currentlyWishlisted) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "productId" to productId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAddress(address: DeliveryAddress): Result<Unit> {
        val uid = requireUserId()
        val addressesCol = db.collection("users").document(uid).collection("addresses")
        val addrId = address.id.ifBlank { addressesCol.document().id }
        val path = "users/$uid/addresses/$addrId"
        return try {
            require(address.label.trim().length in 1..40) { "Label must be 1-40 characters." }
            require(address.recipientName.trim().length in 2..80) { "Recipient name must be 2-80 characters." }
            require(address.phone.trim().length in 7..25) { "Phone must be 7-25 characters." }
            require(address.street.trim().length in 4..200) { "Street address must be at least 4 characters." }
            require(address.city.trim().length in 2..60) { "City must be 2-60 characters." }

            val batch = db.batch()
            if (address.defaultAddress) {
                val existingSnap = addressesCol.get().await()
                existingSnap.documents.forEach { doc ->
                    if (doc.id != addrId && doc.getBoolean("defaultAddress") == true) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "defaultAddress" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "label" to address.label.trim(),
                "recipientName" to address.recipientName.trim(),
                "phone" to address.phone.trim(),
                "street" to address.street.trim(),
                "city" to address.city.trim(),
                "landmark" to address.landmark?.trim()?.takeIf { it.isNotEmpty() },
                "defaultAddress" to address.defaultAddress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = addressesCol.document(addrId)
            if (address.id.isBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                @Suppress("UNCHECKED_CAST")
                batch.set(docRef, payload.filterValues { it != null } as Map<String, Any>)
            } else {
                payload.remove("userId")
                @Suppress("UNCHECKED_CAST")
                batch.update(docRef, payload.filterValues { it != null } as Map<String, Any>)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/addresses/$addressId"
        return try {
            db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
