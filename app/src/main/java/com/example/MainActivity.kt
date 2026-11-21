package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CustomerStoreRepository
import com.example.data.repository.OrderAndRxRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.CartAndCheckoutScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.OrdersAndRxScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.DocMedsTheme
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftBackground
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StorefrontViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocMedsTheme {
                DocMedsAppRoot()
            }
        }
    }
}

@Composable
fun DocMedsAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var preAuthLanguage by remember { mutableStateOf(AppLanguage.EN) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        val layoutDirection = if (preAuthLanguage == AppLanguage.UR) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(
            LocalAppLanguage provides preAuthLanguage,
            LocalDocMedsStrings provides stringsFor(preAuthLanguage),
            LocalLayoutDirection provides layoutDirection
        ) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onLanguageChange = { preAuthLanguage = it }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedDocMedsShell(
                currentUserId = user.uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedDocMedsShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val storefrontViewModel: StorefrontViewModel = viewModel(
        key = "store_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                StorefrontViewModel(
                    currentUserId = currentUserId,
                    authRepo = AuthSessionRepository(db),
                    catalogRepo = CatalogRepository(db),
                    customerRepo = CustomerStoreRepository(db),
                    orderRepo = OrderAndRxRepository(db)
                )
            }
        }
    )

    val language by storefrontViewModel.language.collectAsStateWithLifecycle()
    val isAdmin by storefrontViewModel.isAdmin.collectAsStateWithLifecycle()
    val cartItems by storefrontViewModel.cartItems.collectAsStateWithLifecycle()
    val categoriesState by storefrontViewModel.categoriesState.collectAsStateWithLifecycle()
    val bannerMessage by storefrontViewModel.bannerMessage.collectAsStateWithLifecycle()
    val wishlistItems by storefrontViewModel.wishlistItems.collectAsStateWithLifecycle()

    val strings = stringsFor(language)
    val layoutDirection = if (language == AppLanguage.UR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            snackbarHostState.showSnackbar(bannerMessage!!)
            storefrontViewModel.clearBannerMessage()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var ordersInitialSubTab by remember { mutableIntStateOf(0) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showAdminConsole by remember { mutableStateOf(false) }

    val totalCartQty = cartItems.sumOf { it.quantity }.toInt()
    val wishlistIds = wishlistItems.map { it.productId }.toSet()
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val categoryIconMap = categories.associate { it.id to it.iconKey }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalDocMedsStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        if (showAdminConsole && isAdmin) {
            val adminViewModel: AdminViewModel = viewModel(
                key = "admin_$currentUserId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) as Application
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        AdminViewModel(
                            authRepo = AuthSessionRepository(db),
                            catalogRepo = CatalogRepository(db),
                            orderRepo = OrderAndRxRepository(db)
                        )
                    }
                }
            )
            AdminDashboardScreen(
                adminViewModel = adminViewModel,
                currentUserId = currentUserId,
                onBackToStorefront = { showAdminConsole = false }
            )
        } else if (selectedProduct != null) {
            val detailProd = selectedProduct!!
            ProductDetailScreen(
                product = detailProd,
                categoryIconKey = categoryIconMap[detailProd.categoryId] ?: "medication",
                isWishlisted = wishlistIds.contains(detailProd.id),
                onBack = { selectedProduct = null },
                onToggleWishlist = {
                    storefrontViewModel.toggleWishlist(detailProd.id, wishlistIds.contains(detailProd.id))
                },
                onAddToCart = { storefrontViewModel.addToCart(detailProd) }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = SoftBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = CrispWhite,
                        tonalElevation = 4.dp
                    ) {
                        val navItems = listOf(
                            Triple(strings.tabHome, Icons.Filled.Home to Icons.Outlined.Home, "nav_home"),
                            Triple(strings.tabCart, Icons.Filled.ShoppingCart to Icons.Outlined.ShoppingCart, "nav_cart"),
                            Triple(strings.tabOrders, Icons.Filled.ReceiptLong to Icons.Outlined.ReceiptLong, "nav_orders"),
                            Triple(strings.tabWishlist, Icons.Filled.Favorite to Icons.Outlined.FavoriteBorder, "nav_wishlist"),
                            Triple(strings.tabProfile, Icons.Filled.Person to Icons.Outlined.Person, "nav_profile")
                        )
                        navItems.forEachIndexed { idx, (label, icons, tag) ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (idx == 2) ordersInitialSubTab = 0
                                    selectedTab = idx
                                },
                                icon = {
                                    if (idx == 1 && totalCartQty > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(totalCartQty.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) icons.first else icons.second,
                                                contentDescription = label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) icons.first else icons.second,
                                            contentDescription = label
                                        )
                                    }
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldDeep,
                                    selectedTextColor = EmeraldDeep,
                                    indicatorColor = EmeraldMint
                                ),
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> CustomerHomeScreen(
                            viewModel = storefrontViewModel,
                            onProductClick = { selectedProduct = it },
                            onUploadRxClick = {
                                ordersInitialSubTab = 1
                                selectedTab = 2
                            },
                            onOpenAdminConsole = {
                                if (isAdmin) showAdminConsole = true
                            }
                        )
                        1 -> CartAndCheckoutScreen(
                            viewModel = storefrontViewModel,
                            onBrowseMedicines = { selectedTab = 0 },
                            onOrderPlaced = {
                                ordersInitialSubTab = 0
                                selectedTab = 2
                            }
                        )
                        2 -> OrdersAndRxScreen(
                            viewModel = storefrontViewModel,
                            initialTab = ordersInitialSubTab
                        )
                        3 -> WishlistScreen(
                            viewModel = storefrontViewModel,
                            onProductClick = { selectedProduct = it },
                            onBrowseMedicines = { selectedTab = 0 }
                        )
                        4 -> ProfileScreen(
                            viewModel = storefrontViewModel,
                            onOpenAdminConsole = {
                                if (isAdmin) showAdminConsole = true
                            },
                            onSignedOut = onSignedOut
                        )
                    }
                }
            }
        }
    }
}
