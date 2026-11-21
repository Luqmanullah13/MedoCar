package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.OrderStatus
import com.example.data.model.PrescriptionStatus
import com.example.data.model.Product
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.AmberRx
import com.example.ui.theme.AmberRxBg
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralAlertBg
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoftSurface
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.SkyInfoBg
import com.example.util.ImageCodec
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun formatPkr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "Rs. ${formatter.format(amount.roundToInt())}"
}

fun categoryIconForKey(key: String): ImageVector = when (key.lowercase()) {
    "healing" -> Icons.Filled.Healing
    "medication" -> Icons.Filled.Medication
    "wellness" -> Icons.Filled.HealthAndSafety
    "heart" -> Icons.Filled.MonitorHeart
    "monitor" -> Icons.Filled.LocalHospital
    "derma" -> Icons.Filled.Spa
    "baby" -> Icons.Filled.ChildCare
    "firstaid" -> Icons.Filled.MedicalServices
    else -> Icons.Filled.Medication
}

@Composable
fun ProductVisual(
    product: Product,
    categoryIconKey: String = "medication",
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(product.imageBase64) {
        ImageCodec.decodeBase64ToImageBitmap(product.imageBase64)
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(EmeraldSoftSurface, EmeraldMint.copy(alpha = 0.55f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = product.nameEn,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !product.imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.nameEn,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Surface(
                    shape = CircleShape,
                    color = CrispWhite.copy(alpha = 0.88f),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIconForKey(categoryIconKey),
                            contentDescription = product.nameEn,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Base64ImagePreview(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64) { ImageCodec.decodeBase64ToImageBitmap(base64) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(EmeraldSoftSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MedicalServices,
                contentDescription = contentDescription,
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    categoryIconKey: String,
    isWishlisted: Boolean,
    onCardClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current
    val title = if (lang == AppLanguage.UR && product.nameUr.isNotBlank()) product.nameUr else product.nameEn
    val heartTint by animateColorAsState(
        targetValue = if (isWishlisted) CoralAlert else InkSecondary,
        label = "wishlistTint"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrispWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                ProductVisual(
                    product = product,
                    categoryIconKey = categoryIconKey,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left discount or Rx badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (product.discountPercent > 0L) {
                        Surface(
                            color = CoralAlert,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "-${product.discountPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        color = if (product.prescriptionRequired) AmberRx else EmeraldDeep,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (product.prescriptionRequired) strings.rxRequiredBadge else strings.otcBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = CrispWhite,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Wishlist heart button
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(48.dp)
                        .testTag("wishlist_btn_${product.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CrispWhite.copy(alpha = 0.9f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = strings.wishlistTitle,
                                tint = heartTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (product.isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = CoralAlert,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = strings.outOfStock,
                                style = MaterialTheme.typography.labelMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${product.genericName} • ${product.packSize}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = InkSecondary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatPkr(product.discountedPricePkr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDeep
                    )
                    if (product.discountPercent > 0L) {
                        Text(
                            text = formatPkr(product.pricePkr),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = InkSecondary
                        )
                    }
                }

                Text(
                    text = if (product.isOutOfStock) strings.outOfStock else strings.inStockCount(product.stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isOutOfStock) CoralAlert else EmeraldPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onAddToCart,
                    enabled = !product.isOutOfStock,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = CrispWhite,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = InkSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("add_cart_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.isOutOfStock) strings.outOfStock else strings.addToCart,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RegulatoryNoticeCard(
    text: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isWarning) AmberRxBg else EmeraldSoftSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Filled.Warning else Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = if (isWarning) AmberRx else EmeraldDeep,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isWarning) Color(0xFF78350F) else EmeraldDark
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (bg, fg, label) = when (status) {
        OrderStatus.AWAITING_RX_VERIFICATION -> Triple(AmberRxBg, AmberRx, "Awaiting Rx Review")
        OrderStatus.CONFIRMED -> Triple(SkyInfoBg, SkyInfo, "Confirmed")
        OrderStatus.PACKED -> Triple(EmeraldSoftSurface, EmeraldPrimary, "Packed")
        OrderStatus.OUT_FOR_DELIVERY -> Triple(EmeraldMint, EmeraldDark, "Out for Delivery")
        OrderStatus.DELIVERED -> Triple(EmeraldPrimary, CrispWhite, "Delivered")
        OrderStatus.REJECTED -> Triple(CoralAlertBg, CoralAlert, "Rejected")
        OrderStatus.CANCELLED -> Triple(Color(0xFFE2E8F0), InkSecondary, "Cancelled")
    }
    Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PrescriptionStatusChip(status: PrescriptionStatus) {
    val (bg, fg, label) = when (status) {
        PrescriptionStatus.PENDING_REVIEW -> Triple(AmberRxBg, AmberRx, "Pending Pharmacist Review")
        PrescriptionStatus.APPROVED -> Triple(EmeraldMint, EmeraldDark, "Approved by Pharmacist")
        PrescriptionStatus.REJECTED -> Triple(CoralAlertBg, CoralAlert, "Rejected")
    }
    Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
