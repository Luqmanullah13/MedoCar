package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.ui.components.ProductVisual
import com.example.ui.components.formatPkr
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.StorefrontViewModel

@Composable
fun WishlistScreen(
    viewModel: StorefrontViewModel,
    onProductClick: (Product) -> Unit,
    onBrowseMedicines: () -> Unit
) {
    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current

    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val productsById = (productsState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
    val savedProducts = wishlistItems.mapNotNull { productsById[it.productId] }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CrispWhite, shadowElevation = 2.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(text = strings.wishlistTitle, style = MaterialTheme.typography.headlineSmall)
            }
        }

        if (savedProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.emptyWishlistTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    strings.emptyWishlistSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onBrowseMedicines,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(strings.browseMedicines)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedProducts, key = { it.id }) { prod ->
                    val title = if (lang == AppLanguage.UR && prod.nameUr.isNotBlank()) prod.nameUr else prod.nameEn
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductClick(prod) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProductVisual(
                                product = prod,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    text = formatPkr(prod.discountedPricePkr),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldDeep,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (prod.isOutOfStock) strings.outOfStock else strings.inStockCount(prod.stock),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (prod.isOutOfStock) CoralAlert else EmeraldPrimary
                                )
                            }
                            Button(
                                onClick = { viewModel.addToCart(prod) },
                                enabled = !prod.isOutOfStock,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(strings.moveToCart, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.toggleWishlist(prod.id, true) }) {
                                Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CoralAlert)
                            }
                        }
                    }
                }
            }
        }
    }
}
