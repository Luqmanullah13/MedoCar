package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.OrderStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.PrescriptionRecord
import com.example.data.model.PrescriptionStatus
import com.example.data.model.Product
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
import kotlin.math.roundToInt

class OrderAndRxRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserOrders(userId: String): Flow<List<PharmacyOrder>> = flow {
        val path = "orders"
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeUserPrescriptions(userId: String): Flow<List<PrescriptionRecord>> = flow {
        val path = "prescriptions"
        db.collection("prescriptions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PrescriptionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeAllOrders(): Flow<List<PharmacyOrder>> =
        db.collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllPrescriptions(): Flow<List<PrescriptionRecord>> =
        db.collection("prescriptions")
            .snapshots()
            .map { snap ->
                snap.toObjects(PrescriptionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    /**
     * Executes Cash on Delivery checkout inside a Firestore Transaction:
     * 1. Reads authoritative product docs & verifies live stock.
     * 2. Enforces prescription upload whenever any cart product requires Rx.
     * 3. Decrements product stock, creates prescription (PENDING_REVIEW) if needed, creates order, and clears cart.
     */
    suspend fun placeCodOrder(
        cartItems: List<CartItem>,
        productsById: Map<String, Product>,
        address: DeliveryAddress,
        prescriptionImageBase64: String?,
        patientNote: String?
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val orderDocRef = db.collection("orders").document()
        val rxDocRef = db.collection("prescriptions").document()

        return try {
            require(cartItems.isNotEmpty()) { "Cart is empty." }
            val resolvedPairs = cartItems.mapNotNull { cart ->
                productsById[cart.productId]?.let { prod -> cart to prod }
            }
            require(resolvedPairs.isNotEmpty()) { "Selected products unavailable." }
            val requiresRx = resolvedPairs.any { (_, prod) -> prod.prescriptionRequired }
            if (requiresRx) {
                require(!prescriptionImageBase64.isNullOrBlank() && prescriptionImageBase64.length >= 20) {
                    "Valid prescription image is required for Rx medicines."
                }
            }

            val orderNumber = "DM-${(100000..999999).random()}"

            db.runTransaction { tx ->
                // 1. Read all product snapshots BEFORE any writes
                val productSnaps = resolvedPairs.map { (cart, _) ->
                    val ref = db.collection("products").document(cart.productId)
                    Triple(cart, ref, tx.get(ref))
                }

                var subtotalPkr = 0.0
                var discountPkr = 0.0
                var itemCount = 0L
                val lineMaps = mutableListOf<Map<String, Any>>()

                for ((cart, _, snap) in productSnaps) {
                    if (!snap.exists()) {
                        throw IllegalStateException("A medicine in your cart is no longer available.")
                    }
                    val currentStock = snap.getLong("stock") ?: 0L
                    val active = snap.getBoolean("active") ?: true
                    val nameEn = snap.getString("nameEn") ?: "Medicine"
                    val nameUr = snap.getString("nameUr") ?: nameEn
                    val price = snap.getDouble("pricePkr") ?: 0.0
                    val discountPct = (snap.getLong("discountPercent") ?: 0L).coerceIn(0L, 90L)
                    val rxReq = snap.getBoolean("prescriptionRequired") ?: false

                    if (!active || currentStock < cart.quantity || cart.quantity <= 0L) {
                        throw IllegalStateException("$nameEn only has $currentStock left in stock.")
                    }

                    val unitDiscounted = if (discountPct > 0L) price * (100.0 - discountPct) / 100.0 else price
                    subtotalPkr += unitDiscounted * cart.quantity
                    discountPkr += (price - unitDiscounted) * cart.quantity
                    itemCount += cart.quantity

                    lineMaps.add(
                        mapOf(
                            "productId" to snap.id,
                            "nameEn" to nameEn,
                            "nameUr" to nameUr,
                            "qty" to cart.quantity,
                            "unitPricePkr" to (unitDiscounted * 100.0).roundToInt() / 100.0,
                            "prescriptionRequired" to rxReq
                        )
                    )
                }

                val deliveryFee = if (subtotalPkr >= 2000.0) 0.0 else 150.0
                val totalPkr = ((subtotalPkr + deliveryFee) * 100.0).roundToInt() / 100.0
                val roundedSubtotal = (subtotalPkr * 100.0).roundToInt() / 100.0
                val roundedDiscount = (discountPkr * 100.0).roundToInt() / 100.0

                // 2. Perform all writes after reads
                for ((cart, ref, snap) in productSnaps) {
                    val currentStock = snap.getLong("stock") ?: 0L
                    tx.update(
                        ref,
                        mapOf(
                            "stock" to (currentStock - cart.quantity),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (requiresRx && !prescriptionImageBase64.isNullOrBlank()) {
                    val rxPayload = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "customerName" to (user.displayName?.takeIf { it.isNotBlank() } ?: address.recipientName),
                        "customerEmail" to (user.email ?: "customer@docmeds.pk"),
                        "imageBase64" to prescriptionImageBase64,
                        "patientNote" to patientNote?.trim()?.takeIf { it.isNotEmpty() },
                        "orderId" to orderDocRef.id,
                        "status" to PrescriptionStatus.PENDING_REVIEW.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    @Suppress("UNCHECKED_CAST")
                    tx.set(rxDocRef, rxPayload.filterValues { it != null } as Map<String, Any>)
                }

                val orderPayload = mutableMapOf<String, Any?>(
                    "orderNumber" to orderNumber,
                    "userId" to uid,
                    "customerName" to (user.displayName?.takeIf { it.isNotBlank() } ?: address.recipientName),
                    "customerEmail" to (user.email ?: "customer@docmeds.pk"),
                    "addressLabel" to address.label,
                    "recipientName" to address.recipientName,
                    "deliveryPhone" to address.phone,
                    "deliveryStreet" to address.street,
                    "deliveryCity" to address.city,
                    "deliveryLandmark" to address.landmark?.takeIf { it.isNotBlank() },
                    "items" to lineMaps,
                    "itemCount" to itemCount,
                    "subtotalPkr" to roundedSubtotal,
                    "discountPkr" to roundedDiscount,
                    "deliveryFeePkr" to deliveryFee,
                    "totalPkr" to totalPkr,
                    "paymentMethod" to "CASH_ON_DELIVERY",
                    "requiresPrescription" to requiresRx,
                    "prescriptionId" to if (requiresRx) rxDocRef.id else null,
                    "prescriptionStatus" to if (requiresRx) PrescriptionStatus.PENDING_REVIEW.name else "NOT_REQUIRED",
                    "status" to if (requiresRx) OrderStatus.AWAITING_RX_VERIFICATION.name else OrderStatus.CONFIRMED.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                @Suppress("UNCHECKED_CAST")
                tx.set(orderDocRef, orderPayload.filterValues { it != null } as Map<String, Any>)

                // Clear cart items atomically
                for ((cart, _, _) in productSnaps) {
                    val cartRef = db.collection("users").document(uid).collection("cart").document(cart.productId)
                    tx.delete(cartRef)
                }
                orderNumber
            }.await()

            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders/${orderDocRef.id}")
            Result.failure(e)
        }
    }

    suspend fun submitStandalonePrescription(imageBase64: String, patientNote: String?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val rxRef = db.collection("prescriptions").document()
        return try {
            require(imageBase64.length >= 20) { "Invalid prescription image." }
            val payload = mutableMapOf<String, Any?>(
                "userId" to user.uid,
                "customerName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "DocMeds Customer"),
                "customerEmail" to (user.email ?: "customer@docmeds.pk"),
                "imageBase64" to imageBase64,
                "patientNote" to patientNote?.trim()?.takeIf { it.isNotEmpty() },
                "status" to PrescriptionStatus.PENDING_REVIEW.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            rxRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, rxRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            auth.currentUser ?: throw IllegalStateException("Not signed in")
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Pharmacist / Admin prescription review.
     * Atomically updates prescription status + linked order status when an orderId exists.
     */
    suspend fun reviewPrescription(
        prescription: PrescriptionRecord,
        approved: Boolean,
        pharmacistNotes: String
    ): Result<Unit> {
        val adminEmail = auth.currentUser?.email ?: "pharmacist@docmeds.pk"
        val path = "prescriptions/${prescription.id}"
        return try {
            require(pharmacistNotes.trim().length in 3..500) {
                "Please enter pharmacist verification notes (3-500 characters)."
            }
            val nextRxStatus = if (approved) PrescriptionStatus.APPROVED else PrescriptionStatus.REJECTED
            val batch = db.batch()
            batch.update(
                db.collection("prescriptions").document(prescription.id),
                mapOf(
                    "status" to nextRxStatus.name,
                    "pharmacistNotes" to pharmacistNotes.trim(),
                    "reviewedBy" to adminEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val linkedOrderId = prescription.orderId
            if (!linkedOrderId.isNullOrBlank()) {
                val nextOrderStatus = if (approved) OrderStatus.CONFIRMED else OrderStatus.REJECTED
                batch.update(
                    db.collection("orders").document(linkedOrderId),
                    mapOf(
                        "prescriptionStatus" to nextRxStatus.name,
                        "status" to nextOrderStatus.name,
                        "pharmacistNote" to pharmacistNotes.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(order: PharmacyOrder, newStatus: OrderStatus, note: String? = null): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            auth.currentUser ?: throw IllegalStateException("Not signed in")
            if (order.requiresPrescription &&
                newStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED) &&
                order.prescriptionStatus != PrescriptionStatus.APPROVED.name
            ) {
                throw IllegalStateException("Cannot advance Rx order until prescription is approved by pharmacist.")
            }
            val updates = mutableMapOf<String, Any?>(
                "status" to newStatus.name,
                "pharmacistNote" to note?.trim()?.takeIf { it.isNotEmpty() },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            db.collection("orders").document(order.id).update(updates.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
