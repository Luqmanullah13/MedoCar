package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.components.ProductVisual
import com.example.ui.components.RegulatoryNoticeCard
import com.example.ui.components.formatPkr
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.AmberRx
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SoftBackground

@Composable
fun ProductDetailScreen(
    product: Product,
    categoryIconKey: String,
    isWishlisted: Boolean,
    onBack: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToCart: () -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current
    val title = if (lang == AppLanguage.UR && product.nameUr.isNotBlank()) product.nameUr else product.nameEn
    val description = if (lang == AppLanguage.UR && product.descriptionUr.isNotBlank()) {
        product.descriptionUr
    } else {
        product.descriptionEn
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        // Top Bar
        Surface(color = CrispWhite, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = strings.viewDetails,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier.testTag("detail_wishlist_btn")
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = strings.wishlistTitle,
                        tint = if (isWishlisted) CoralAlert else InkSecondary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductVisual(
                product = product,
                categoryIconKey = categoryIconKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            // Badges + Name + Price
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = if (product.prescriptionRequired) AmberRx else EmeraldDeep,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.prescriptionRequired) strings.rxRequiredBadge else strings.otcBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrispWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (product.discountPercent > 0L) {
                            Surface(color = CoralAlert, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "-${product.discountPercent}% OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrispWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatPkr(product.discountedPricePkr),
                            style = MaterialTheme.typography.headlineMedium,
                            color = EmeraldDeep,
                            fontWeight = FontWeight.Bold
                        )
                        if (product.discountPercent > 0L) {
                            Text(
                                text = formatPkr(product.pricePkr),
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = InkSecondary
                            )
                        }
                    }

                    Text(
                        text = if (product.isOutOfStock) strings.outOfStock else strings.inStockCount(product.stock),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (product.isOutOfStock) CoralAlert else EmeraldPrimary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    DetailRow(strings.genericLabel, product.genericName)
                    DetailRow(strings.manufacturerLabel, product.manufacturer)
                    DetailRow(strings.packSizeLabel, product.packSize)
                }
            }

            // Description
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CrispWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.descriptionLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary
                    )
                }
            }

            if (product.prescriptionRequired) {
                RegulatoryNoticeCard(
                    text = strings.rxRequiredCheckoutDesc,
                    isWarning = true
                )
            }

            RegulatoryNoticeCard(text = strings.noMedicalAdviceDisclaimer)
        }

        // Bottom Add to Cart bar
        Surface(color = CrispWhite, shadowElevation = 8.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onAddToCart,
                    enabled = !product.isOutOfStock,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = CrispWhite,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("detail_add_cart_btn")
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product.isOutOfStock) strings.outOfStock else strings.addToCart,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
