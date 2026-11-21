package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddress
import com.example.data.model.MedicineCategory
import com.example.data.model.PharmacyOrder
import com.example.data.model.PrescriptionRecord
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CustomerStoreRepository
import com.example.data.repository.OrderAndRxRepository
import com.example.ui.i18n.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CatalogFilterMode { ALL, OTC, RX }

data class CartResolvedItem(
    val cartItem: CartItem,
    val product: Product
)

class StorefrontViewModel(
    private val currentUserId: String,
    private val authRepo: AuthSessionRepository,
    private val catalogRepo: CatalogRepository,
    private val customerRepo: CustomerStoreRepository,
    private val orderRepo: OrderAndRxRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StorefrontVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _filterMode = MutableStateFlow(CatalogFilterMode.ALL)
    val filterMode: StateFlow<CatalogFilterMode> = _filterMode.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _actionBusy = MutableStateFlow(false)
    val actionBusy: StateFlow<Boolean> = _actionBusy.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    val isAdmin: StateFlow<Boolean> = authRepo.observeIsAdmin(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val userProfile: StateFlow<UserProfile?> = authRepo.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException &&
                generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }
            ) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val categoriesState: StateFlow<UiState<List<MedicineCategory>>> =
        catalogRepo.observeCategories()
            .asUiState("observeCategories")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val productsState: StateFlow<UiState<List<Product>>> =
        catalogRepo.observeProducts()
            .asUiState("observeProducts")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val cartItems: StateFlow<List<CartItem>> =
        customerRepo.observeCart(currentUserId)
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val wishlistItems: StateFlow<List<WishlistItem>> =
        customerRepo.observeWishlist(currentUserId)
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val addresses: StateFlow<List<DeliveryAddress>> =
        customerRepo.observeAddresses(currentUserId)
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userOrdersState: StateFlow<UiState<List<PharmacyOrder>>> =
        orderRepo.observeUserOrders(currentUserId)
            .asUiState("observeUserOrders")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val userPrescriptionsState: StateFlow<UiState<List<PrescriptionRecord>>> =
        orderRepo.observeUserPrescriptions(currentUserId)
            .asUiState("observeUserPrescriptions")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val filteredProducts: StateFlow<List<Product>> = combine(
        productsState,
        searchQuery,
        selectedCategoryId,
        filterMode
    ) { state, query, catId, mode ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        val q = query.trim().lowercase()
        list.filter { prod ->
            prod.active &&
                (catId == null || prod.categoryId == catId) &&
                when (mode) {
                    CatalogFilterMode.ALL -> true
                    CatalogFilterMode.OTC -> !prod.prescriptionRequired
                    CatalogFilterMode.RX -> prod.prescriptionRequired
                } &&
                (q.isEmpty() ||
                    prod.nameEn.lowercase().contains(q) ||
                    prod.nameUr.lowercase().contains(q) ||
                    prod.genericName.lowercase().contains(q) ||
                    prod.manufacturer.lowercase().contains(q))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val resolvedCart: StateFlow<List<CartResolvedItem>> = combine(
        cartItems,
        productsState
    ) { cart, prodState ->
        val products = (prodState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
        cart.mapNotNull { c -> products[c.productId]?.let { p -> CartResolvedItem(c, p) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val initResult = authRepo.ensureUserProfileInitialized()
            if (initResult.getOrNull() == true) {
                catalogRepo.seedStarterCatalogIfEmpty()
            }
        }
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    _language.value = AppLanguage.fromCode(profile.preferredLanguage)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun setFilterMode(mode: CatalogFilterMode) {
        _filterMode.value = mode
    }

    fun toggleLanguage() {
        val next = if (_language.value == AppLanguage.EN) AppLanguage.UR else AppLanguage.EN
        _language.value = next
        val profile = userProfile.value
        if (profile != null) {
            viewModelScope.launch {
                authRepo.updateProfile(profile.displayName, profile.phone, next.code)
            }
        }
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun addToCart(product: Product) {
        if (product.isOutOfStock) {
            _bannerMessage.value = "Out of stock"
            return
        }
        viewModelScope.launch {
            val res = customerRepo.addToCart(product.id, product.stock, 1L)
            res.onSuccess {
                _bannerMessage.value = "${product.nameEn} added to cart"
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to add to cart"
            }
        }
    }

    fun updateCartQuantity(productId: String, newQty: Long, maxStock: Long) {
        viewModelScope.launch {
            customerRepo.updateCartQuantity(productId, newQty, maxStock).onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to update quantity"
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            customerRepo.removeFromCart(productId)
        }
    }

    fun toggleWishlist(productId: String, currentlyWishlisted: Boolean) {
        viewModelScope.launch {
            customerRepo.toggleWishlist(productId, currentlyWishlisted).onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Wishlist update failed"
            }
        }
    }

    fun saveAddress(address: DeliveryAddress, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _actionBusy.value = true
            customerRepo.saveAddress(address)
                .onSuccess {
                    _bannerMessage.value = "Address saved"
                    onDone()
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Failed to save address"
                }
            _actionBusy.value = false
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            customerRepo.deleteAddress(addressId).onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to delete address"
            }
        }
    }

    fun updateProfileDetails(displayName: String, phone: String?) {
        viewModelScope.launch {
            _actionBusy.value = true
            authRepo.updateProfile(displayName, phone, _language.value.code)
                .onSuccess { _bannerMessage.value = "Profile updated" }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Profile update failed" }
            _actionBusy.value = false
        }
    }

    fun placeCodOrder(
        address: DeliveryAddress,
        prescriptionImageBase64: String?,
        patientNote: String?,
        onSuccess: (String) -> Unit
    ) {
        val currentCart = cartItems.value
        val products = (productsState.value as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
        viewModelScope.launch {
            _actionBusy.value = true
            orderRepo.placeCodOrder(
                cartItems = currentCart,
                productsById = products,
                address = address,
                prescriptionImageBase64 = prescriptionImageBase64,
                patientNote = patientNote
            ).onSuccess { orderNumber ->
                _bannerMessage.value = "Order $orderNumber placed (Cash on Delivery)"
                onSuccess(orderNumber)
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Checkout failed"
            }
            _actionBusy.value = false
        }
    }

    fun submitStandalonePrescription(imageBase64: String, note: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _actionBusy.value = true
            orderRepo.submitStandalonePrescription(imageBase64, note)
                .onSuccess {
                    _bannerMessage.value = "Prescription uploaded for pharmacist review"
                    onDone()
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Upload failed"
                }
            _actionBusy.value = false
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _actionBusy.value = true
            orderRepo.cancelOrder(orderId)
                .onSuccess { _bannerMessage.value = "Order cancelled" }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Could not cancel order" }
            _actionBusy.value = false
        }
    }

    private fun isPureCancellation(error: Throwable): Boolean =
        error is CancellationException &&
            generateSequence<Throwable>(error) { it.cause }.none { it is FirebaseFirestoreException }

    private fun <T> Flow<T>.asUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error in $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load data"))
            }
}
