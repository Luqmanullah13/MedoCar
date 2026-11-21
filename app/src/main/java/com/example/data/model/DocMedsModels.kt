package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole {
    CUSTOMER,
    ADMIN;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class PrescriptionStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): PrescriptionStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING_REVIEW
    }
}

enum class OrderStatus {
    AWAITING_RX_VERIFICATION,
    CONFIRMED,
    PACKED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED;

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AWAITING_RX_VERIFICATION
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val preferredLanguage: String = "en",
    val role: String = "CUSTOMER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val roleLabel: String = "Licensed Pharmacist",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val recipientName: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val landmark: String? = null,
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicineCategory(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameUr: String = "",
    val iconKey: String = "medication",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameUr: String = "",
    val genericName: String = "",
    val manufacturer: String = "",
    val categoryId: String = "",
    val descriptionEn: String = "",
    val descriptionUr: String = "",
    val packSize: String = "",
    val pricePkr: Double = 0.0,
    val discountPercent: Long = 0L,
    val stock: Long = 0L,
    val prescriptionRequired: Boolean = false,
    val popular: Boolean = false,
    val active: Boolean = true,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val discountedPricePkr: Double
        get() {
            val pct = discountPercent.coerceIn(0L, 90L)
            return if (pct <= 0L) pricePkr else pricePkr * (100.0 - pct) / 100.0
        }

    val isOutOfStock: Boolean
        get() = stock <= 0L
}

@Keep
@IgnoreExtraProperties
data class PrescriptionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val imageBase64: String = "",
    val patientNote: String? = null,
    val orderId: String? = null,
    val status: String = PrescriptionStatus.PENDING_REVIEW.name,
    val pharmacistNotes: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: PrescriptionStatus
        get() = PrescriptionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val nameEn: String = "",
    val nameUr: String = "",
    val qty: Long = 1L,
    val unitPricePkr: Double = 0.0,
    val prescriptionRequired: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class PharmacyOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val addressLabel: String = "",
    val recipientName: String = "",
    val deliveryPhone: String = "",
    val deliveryStreet: String = "",
    val deliveryCity: String = "",
    val deliveryLandmark: String? = null,
    val items: List<Map<String, Any>> = emptyList(),
    val itemCount: Long = 0L,
    val subtotalPkr: Double = 0.0,
    val discountPkr: Double = 0.0,
    val deliveryFeePkr: Double = 0.0,
    val totalPkr: Double = 0.0,
    val paymentMethod: String = "CASH_ON_DELIVERY",
    val requiresPrescription: Boolean = false,
    val prescriptionId: String? = null,
    val prescriptionStatus: String = "NOT_REQUIRED",
    val status: String = OrderStatus.CONFIRMED.name,
    val pharmacistNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: OrderStatus
        get() = OrderStatus.fromRaw(status)

    fun parsedItems(): List<OrderLineItem> = items.map { map ->
        OrderLineItem(
            productId = map["productId"] as? String ?: "",
            nameEn = map["nameEn"] as? String ?: "",
            nameUr = map["nameUr"] as? String ?: "",
            qty = (map["qty"] as? Number)?.toLong() ?: 1L,
            unitPricePkr = (map["unitPricePkr"] as? Number)?.toDouble() ?: 0.0,
            prescriptionRequired = map["prescriptionRequired"] as? Boolean ?: false
        )
    }
}
