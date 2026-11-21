package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DeliveryAddress
import com.example.ui.components.Base64ImagePreview
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
import com.example.ui.theme.EmeraldSoftSurface
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.StorefrontViewModel
import com.example.util.ImageCodec

@Composable
fun CartAndCheckoutScreen(
    viewModel: StorefrontViewModel,
    onBrowseMedicines: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current
    val context = LocalContext.current

    val resolvedCart by viewModel.resolvedCart.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val busy by viewModel.actionBusy.collectAsStateWithLifecycle()

    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var rxBase64 by remember { mutableStateOf<String?>(null) }
    var patientNote by remember { mutableStateOf("") }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(addresses) {
        if (selectedAddressId == null || addresses.none { it.id == selectedAddressId }) {
            selectedAddressId = addresses.firstOrNull { it.defaultAddress }?.id ?: addresses.firstOrNull()?.id
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            rxBase64 = ImageCodec.uriToCompressedDataUri(context, uri)
        }
    }

    val requiresRx = resolvedCart.any { it.product.prescriptionRequired }
    val hasOutOfStockItem = resolvedCart.any { it.product.isOutOfStock || it.cartItem.quantity > it.product.stock }
    val subtotal = resolvedCart.sumOf { it.product.discountedPricePkr * it.cartItem.quantity }
    val savings = resolvedCart.sumOf { (it.product.pricePkr - it.product.discountedPricePkr) * it.cartItem.quantity }
    val deliveryFee = if (subtotal >= 2000.0 || subtotal == 0.0) 0.0 else 150.0
    val totalCod = subtotal + deliveryFee
    val selectedAddress = addresses.firstOrNull { it.id == selectedAddressId }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(color = CrispWhite, shadowElevation = 2.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = strings.cartTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        if (resolvedCart.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_cart),
                    contentDescription = strings.emptyCartTitle,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.emptyCartTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.emptyCartSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBrowseMedicines,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("empty_cart_browse_btn")
                ) {
                    Text(strings.browseMedicines)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(resolvedCart, key = { it.cartItem.productId }) { item ->
                    val prod = item.product
                    val title = if (lang == AppLanguage.UR && prod.nameUr.isNotBlank()) prod.nameUr else prod.nameEn
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier.fillMaxWidth()
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
                                if (prod.prescriptionRequired) {
                                    Text(
                                        text = strings.rxRequiredBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberRx,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (prod.isOutOfStock) {
                                    Text(
                                        text = strings.outOfStock,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralAlert
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateCartQuantity(prod.id, item.cartItem.quantity - 1L, prod.stock)
                                    }
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = item.cartItem.quantity.toString(),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.updateCartQuantity(prod.id, item.cartItem.quantity + 1L, prod.stock)
                                    },
                                    enabled = item.cartItem.quantity < prod.stock
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                                IconButton(onClick = { viewModel.removeFromCart(prod.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CoralAlert)
                                }
                            }
                        }
                    }
                }

                // Saved Delivery Address Selector
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = EmeraldPrimary)
                                    Text(strings.deliveryAddressLabel, style = MaterialTheme.typography.titleMedium)
                                }
                                TextButton(
                                    onClick = { showAddAddressDialog = true },
                                    modifier = Modifier.testTag("checkout_add_address_btn")
                                ) {
                                    Text(strings.addAddress)
                                }
                            }

                            if (addresses.isEmpty()) {
                                Text(
                                    text = strings.selectAddressHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralAlert
                                )
                            } else {
                                addresses.forEach { addr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (addr.id == selectedAddressId) EmeraldSoftSurface else CrispWhite
                                            )
                                            .clickable { selectedAddressId = addr.id }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = addr.id == selectedAddressId,
                                            onClick = { selectedAddressId = addr.id }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${addr.label} • ${addr.recipientName}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${addr.street}, ${addr.city} (${addr.phone})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = InkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mandatory Prescription Upload when cart contains Rx medicine
                if (requiresRx) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CrispWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RegulatoryNoticeCard(
                                    text = strings.rxRequiredCheckoutDesc,
                                    isWarning = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_upload_rx_btn")
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.uploadPrescriptionBtn)
                                }
                                val previewB64 = rxBase64
                                if (!previewB64.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Base64ImagePreview(
                                            base64 = previewB64,
                                            contentDescription = strings.rxAttachedReady,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = strings.rxAttachedReady,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = EmeraldDeep
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = patientNote,
                                    onValueChange = { patientNote = it.take(300) },
                                    label = { Text(strings.patientNoteHint) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Cash on Delivery Summary
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = EmeraldPrimary)
                                Text(strings.orderSummary, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = strings.paymentMethodCod,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDeep,
                                fontWeight = FontWeight.SemiBold
                            )
                            SummaryRow(strings.subtotal, formatPkr(subtotal))
                            if (savings > 0.0) {
                                SummaryRow(strings.savings, "-${formatPkr(savings)}")
                            }
                            SummaryRow(
                                strings.deliveryFee,
                                if (deliveryFee == 0.0) strings.freeDelivery else formatPkr(deliveryFee)
                            )
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.totalCod, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = formatPkr(totalCod),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EmeraldDeep,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val canSubmit = selectedAddress != null &&
                                !hasOutOfStockItem &&
                                (!requiresRx || !rxBase64.isNullOrBlank()) &&
                                !busy

                            Button(
                                onClick = {
                                    val addr = selectedAddress ?: return@Button
                                    viewModel.placeCodOrder(
                                        address = addr,
                                        prescriptionImageBase64 = rxBase64,
                                        patientNote = patientNote
                                    ) {
                                        rxBase64 = null
                                        patientNote = ""
                                        onOrderPlaced()
                                    }
                                },
                                enabled = canSubmit,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("place_cod_order_btn")
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(
                                        color = CrispWhite,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(strings.placeCodOrder, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddressEditorDialog(
            initial = null,
            onDismiss = { showAddAddressDialog = false },
            onSave = { addr ->
                viewModel.saveAddress(addr) { showAddAddressDialog = false }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddressEditorDialog(
    initial: DeliveryAddress?,
    onDismiss: () -> Unit,
    onSave: (DeliveryAddress) -> Unit
) {
    val strings = LocalDocMedsStrings.current
    var label by remember { mutableStateOf(initial?.label ?: "Home") }
    var recipient by remember { mutableStateOf(initial?.recipientName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var street by remember { mutableStateOf(initial?.street ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "Lahore") }
    var landmark by remember { mutableStateOf(initial?.landmark ?: "") }
    var isDefault by remember { mutableStateOf(initial?.defaultAddress ?: true) }

    val valid = label.trim().isNotEmpty() &&
        recipient.trim().length >= 2 &&
        phone.trim().length >= 7 &&
        street.trim().length >= 4 &&
        city.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addAddress) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(strings.addressLabelHint) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(strings.recipientNameHint) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phoneHint) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text(strings.streetHint) }
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(strings.cityHint) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text(strings.landmarkHint) },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDefault = !isDefault }
                ) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text(strings.setDefaultAddress, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DeliveryAddress(
                            id = initial?.id ?: "",
                            label = label.trim(),
                            recipientName = recipient.trim(),
                            phone = phone.trim(),
                            street = street.trim(),
                            city = city.trim(),
                            landmark = landmark.trim().takeIf { it.isNotEmpty() },
                            defaultAddress = isDefault
                        )
                    )
                },
                enabled = valid
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
