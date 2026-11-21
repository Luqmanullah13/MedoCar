package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRecord
import com.example.data.model.MedicineCategory
import com.example.data.model.OrderStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.PrescriptionRecord
import com.example.data.model.PrescriptionStatus
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.OrderAndRxRepository
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

data class AdminDashboardMetrics(
    val totalRevenuePkr: Double = 0.0,
    val totalOrders: Int = 0,
    val pendingRxCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalCustomers: Int = 0,
    val statusBreakdown: Map<OrderStatus, Int> = emptyMap()
)

class AdminViewModel(
    private val authRepo: AuthSessionRepository,
    private val catalogRepo: CatalogRepository,
    private val orderRepo: OrderAndRxRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AdminVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val categoriesState: StateFlow<UiState<List<MedicineCategory>>> =
        catalogRepo.observeCategories().asUiState("categories")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val productsState: StateFlow<UiState<List<Product>>> =
        catalogRepo.observeProducts().asUiState("products")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allOrdersState: StateFlow<UiState<List<PharmacyOrder>>> =
        orderRepo.observeAllOrders().asUiState("allOrders")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allPrescriptionsState: StateFlow<UiState<List<PrescriptionRecord>>> =
        orderRepo.observeAllPrescriptions().asUiState("allPrescriptions")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allCustomersState: StateFlow<UiState<List<UserProfile>>> =
        authRepo.observeAllCustomers().asUiState("allCustomers")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val adminsList: StateFlow<List<AdminRecord>> =
        authRepo.observeAllAdmins()
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val metrics: StateFlow<AdminDashboardMetrics> = combine(
        allOrdersState,
        allPrescriptionsState,
        productsState,
        allCustomersState
    ) { ordersUi, rxUi, prodUi, custUi ->
        val orders = (ordersUi as? UiState.Success)?.data ?: emptyList()
        val rxs = (rxUi as? UiState.Success)?.data ?: emptyList()
        val prods = (prodUi as? UiState.Success)?.data ?: emptyList()
        val custs = (custUi as? UiState.Success)?.data ?: emptyList()

        val activeOrders = orders.filter {
            it.parsedStatus != OrderStatus.CANCELLED && it.parsedStatus != OrderStatus.REJECTED
        }
        val revenue = activeOrders.sumOf { it.totalPkr }
        val pendingRx = rxs.count { it.parsedStatus == PrescriptionStatus.PENDING_REVIEW }
        val lowStock = prods.count { it.stock in 1L..5L }
        val outOfStock = prods.count { it.stock <= 0L }
        val breakdown = orders.groupingBy { it.parsedStatus }.eachCount()

        AdminDashboardMetrics(
            totalRevenuePkr = revenue,
            totalOrders = orders.size,
            pendingRxCount = pendingRx,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock,
            totalCustomers = custs.size,
            statusBreakdown = breakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AdminDashboardMetrics())

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            catalogRepo.saveProduct(product)
                .onSuccess {
                    _statusMessage.value = "Medicine saved"
                    onSuccess()
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to save medicine" }
            _busy.value = false
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _busy.value = true
            catalogRepo.deleteProduct(productId)
                .onSuccess { _statusMessage.value = "Medicine deleted" }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Delete failed" }
            _busy.value = false
        }
    }

    fun saveCategory(category: MedicineCategory, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            catalogRepo.saveCategory(category)
                .onSuccess {
                    _statusMessage.value = "Category saved"
                    onSuccess()
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Failed to save category" }
            _busy.value = false
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _busy.value = true
            catalogRepo.deleteCategory(categoryId)
                .onSuccess { _statusMessage.value = "Category deleted" }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Delete failed" }
            _busy.value = false
        }
    }

    fun reviewPrescription(
        prescription: PrescriptionRecord,
        approved: Boolean,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            orderRepo.reviewPrescription(prescription, approved, notes)
                .onSuccess {
                    _statusMessage.value = if (approved) "Prescription approved" else "Prescription rejected"
                    onSuccess()
                }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Review failed" }
            _busy.value = false
        }
    }

    fun updateOrderStatus(order: PharmacyOrder, newStatus: OrderStatus, note: String? = null) {
        viewModelScope.launch {
            _busy.value = true
            orderRepo.updateOrderStatus(order, newStatus, note)
                .onSuccess { _statusMessage.value = "Order updated to ${newStatus.name}" }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Status update failed" }
            _busy.value = false
        }
    }

    fun grantAdmin(user: UserProfile) {
        viewModelScope.launch {
            _busy.value = true
            authRepo.grantAdminRole(user)
                .onSuccess { _statusMessage.value = "${user.displayName} granted Pharmacist Admin access" }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Role update failed" }
            _busy.value = false
        }
    }

    fun revokeAdmin(uid: String) {
        viewModelScope.launch {
            _busy.value = true
            authRepo.revokeAdminRole(uid)
                .onSuccess { _statusMessage.value = "Admin role revoked" }
                .onFailure { e -> _statusMessage.value = e.localizedMessage ?: "Could not revoke role" }
            _busy.value = false
        }
    }

    private fun <T> Flow<T>.asUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error in $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load admin data"))
            }
}
