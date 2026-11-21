package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MedicineCategory
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.ui.components.ProductGridCard
import com.example.ui.components.RegulatoryNoticeCard
import com.example.ui.components.categoryIconForKey
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoftSurface
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.CatalogFilterMode
import com.example.ui.viewmodel.StorefrontViewModel

@Composable
fun CustomerHomeScreen(
    viewModel: StorefrontViewModel,
    onProductClick: (Product) -> Unit,
    onUploadRxClick: () -> Unit,
    onOpenAdminConsole: () -> Unit
) {
    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val wishlistIds = wishlistItems.map { it.productId }.toSet()
    val categories = (categoriesState as? UiState.Success)?.data?.filter { it.active } ?: emptyList()
    val categoryIconMap = categories.associate { it.id to it.iconKey }
    val popularProducts = filteredProducts.filter { it.popular }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 700.dp) 3 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daraz-inspired Emerald Top Header + Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(EmeraldDeep, EmeraldPrimary)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = CrispWhite,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalPharmacy,
                                        contentDescription = strings.appName,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = strings.appName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CrispWhite
                                )
                                Text(
                                    text = strings.tagline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldMint
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdmin) {
                                Surface(
                                    color = CrispWhite,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(onClick = onOpenAdminConsole)
                                        .testTag("home_admin_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = strings.adminConsoleTitle,
                                            tint = EmeraldDeep,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Admin",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldDeep,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = CrispWhite.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { viewModel.toggleLanguage() }
                                    .testTag("home_language_toggle")
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "اردو" else "EN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrispWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CrispWhite,
                            unfocusedContainerColor = CrispWhite,
                            focusedBorderColor = CrispWhite,
                            unfocusedBorderColor = CrispWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }

            // Promotional Carousel (Zero third-party ads — DocMeds verified healthcare banners)
            item {
                val pagerState = rememberPagerState(pageCount = { 2 })
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 12.dp
                    ) { page ->
                        PromoBannerCard(
                            drawableRes = if (page == 0) R.drawable.img_banner_pharmacy else R.drawable.img_banner_rx,
                            title = if (page == 0) strings.promoTitle1 else strings.promoTitle2,
                            subtitle = if (page == 0) strings.promoSubtitle1 else strings.promoSubtitle2,
                            ctaText = if (page == 1) strings.uploadRxNow else null,
                            onCtaClick = onUploadRxClick
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { idx ->
                            val selected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) EmeraldPrimary else EmeraldMint)
                            )
                        }
                    }
                }
            }

            // Quick Prescription Upload Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSoftSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onUploadRxClick)
                        .testTag("quick_upload_rx_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.DocumentScanner,
                                    contentDescription = strings.uploadRxNow,
                                    tint = CrispWhite
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.promoTitle2,
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldDark
                            )
                            Text(
                                text = strings.pharmacistVerifiedNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        Button(
                            onClick = onUploadRxClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(strings.uploadRxNow, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Medicine Categories Horizontal Strip
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            CategoryChipCard(
                                label = strings.allCategories,
                                iconKey = "medication",
                                selected = selectedCategoryId == null,
                                onClick = { viewModel.selectCategory(null) }
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            val label = if (lang == AppLanguage.UR && cat.nameUr.isNotBlank()) cat.nameUr else cat.nameEn
                            CategoryChipCard(
                                label = label,
                                iconKey = cat.iconKey,
                                selected = selectedCategoryId == cat.id,
                                onClick = { viewModel.selectCategory(cat.id) }
                            )
                        }
                    }
                }
            }

            // OTC / Rx Filter Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        CatalogFilterMode.ALL to strings.filterAll,
                        CatalogFilterMode.OTC to strings.filterOtc,
                        CatalogFilterMode.RX to strings.filterRx
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { viewModel.setFilterMode(mode) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = CrispWhite
                            )
                        )
                    }
                }
            }

            // Popular Products Horizontal Showcase
            if (popularProducts.isNotEmpty() && searchQuery.isBlank() && selectedCategoryId == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.popularMedicines,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularProducts, key = { "pop_${it.id}" }) { product ->
                                ProductGridCard(
                                    product = product,
                                    categoryIconKey = categoryIconMap[product.categoryId] ?: "medication",
                                    isWishlisted = wishlistIds.contains(product.id),
                                    onCardClick = { onProductClick(product) },
                                    onToggleWishlist = {
                                        viewModel.toggleWishlist(product.id, wishlistIds.contains(product.id))
                                    },
                                    onAddToCart = { viewModel.addToCart(product) },
                                    modifier = Modifier.width(172.dp)
                                )
                            }
                        }
                    }
                }
            }

            // All Catalog Grid Header
            item {
                Text(
                    text = strings.allMedicines,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when (productsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        RegulatoryNoticeCard(
                            text = (productsState as UiState.Error).message,
                            isWarning = true,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No matching medicines found",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = strings.regulatoryBanner,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        val rows = filteredProducts.chunked(columns)
                        items(rows) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { product ->
                                    ProductGridCard(
                                        product = product,
                                        categoryIconKey = categoryIconMap[product.categoryId] ?: "medication",
                                        isWishlisted = wishlistIds.contains(product.id),
                                        onCardClick = { onProductClick(product) },
                                        onToggleWishlist = {
                                            viewModel.toggleWishlist(product.id, wishlistIds.contains(product.id))
                                        },
                                        onAddToCart = { viewModel.addToCart(product) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Regulatory disclaimer footer
            item {
                RegulatoryNoticeCard(
                    text = strings.noMedicalAdviceDisclaimer,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    drawableRes: Int,
    title: String,
    subtitle: String,
    ctaText: String?,
    onCtaClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EmeraldDark.copy(alpha = 0.88f),
                                EmeraldDark.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = CrispWhite
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrispWhite.copy(alpha = 0.92f),
                        maxLines = 2
                    )
                    if (ctaText != null) {
                        Button(
                            onClick = onCtaClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrispWhite,
                                contentColor = EmeraldDeep
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(ctaText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipCard(
    label: String,
    iconKey: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) EmeraldPrimary else CrispWhite,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = categoryIconForKey(iconKey),
                contentDescription = label,
                tint = if (selected) CrispWhite else EmeraldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) CrispWhite else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
