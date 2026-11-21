package com.example.ui.screens.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MedicineCategory
import com.example.data.model.PharmacyOrder
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthSessionRepository
import com.example.ui.components.ProductVisual
import com.example.ui.components.RegulatoryNoticeCard
import com.example.ui.components.formatPkr
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.AmberRx
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.AdminViewModel
import com.example.util.ImageCodec

@Composable
fun AdminProductsTab(adminViewModel: AdminViewModel) {
    val strings = LocalDocMedsStrings.current
    val productsState by adminViewModel.productsState.collectAsStateWithLifecycle()
    val categoriesState by adminViewModel.categoriesState.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()

    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Catalog Inventory", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    editingProduct = null
                    showEditor = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("admin_add_product_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addProduct)
            }
        }

        when (val st = productsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                RegulatoryNoticeCard(text = st.message, isWarning = true)
            }
            is UiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(st.data, key = { it.id }) { prod ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.nameEn, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${formatPkr(prod.discountedPricePkr)} (-${prod.discountPercent}%) • Stock: ${prod.stock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (prod.stock <= 5L) CoralAlert else EmeraldDeep
                                    )
                                    Text(
                                        text = if (prod.prescriptionRequired) "Rx Required" else "OTC",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (prod.prescriptionRequired) AmberRx else InkSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingProduct = prod
                                        showEditor = true
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = strings.edit, tint = EmeraldPrimary)
                                }
                                IconButton(
                                    onClick = { adminViewModel.deleteProduct(prod.id) },
                                    enabled = !busy
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            initial = editingProduct,
            categories = categories,
            busy = busy,
            onDismiss = { showEditor = false },
            onSave = { product ->
                adminViewModel.saveProduct(product) { showEditor = false }
            }
        )
    }
}

@Composable
private fun ProductEditorDialog(
    initial: Product?,
    categories: List<MedicineCategory>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val strings = LocalDocMedsStrings.current
    val context = LocalContext.current

    var nameEn by remember { mutableStateOf(initial?.nameEn ?: "") }
    var nameUr by remember { mutableStateOf(initial?.nameUr ?: "") }
    var genericName by remember { mutableStateOf(initial?.genericName ?: "") }
    var manufacturer by remember { mutableStateOf(initial?.manufacturer ?: "") }
    var packSize by remember { mutableStateOf(initial?.packSize ?: "10 Tablets") }
    var descEn by remember { mutableStateOf(initial?.descriptionEn ?: "") }
    var descUr by remember { mutableStateOf(initial?.descriptionUr ?: "") }
    var priceStr by remember { mutableStateOf(initial?.pricePkr?.toInt()?.toString() ?: "250") }
    var discountStr by remember { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var stockStr by remember { mutableStateOf(initial?.stock?.toString() ?: "20") }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "cat_pain") }
    var rxRequired by remember { mutableStateOf(initial?.prescriptionRequired ?: false) }
    var popular by remember { mutableStateOf(initial?.popular ?: false) }
    var imageBase64 by remember { mutableStateOf(initial?.imageBase64) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageBase64 = ImageCodec.uriToCompressedDataUri(context, uri)
        }
    }

    val price = priceStr.toDoubleOrNull() ?: 0.0
    val discount = (discountStr.toLongOrNull() ?: 0L).coerceIn(0L, 90L)
    val stock = (stockStr.toLongOrNull() ?: 0L).coerceAtLeast(0L)
    val valid = nameEn.trim().length >= 2 &&
        nameUr.trim().length >= 2 &&
        genericName.trim().length >= 2 &&
        manufacturer.trim().length >= 2 &&
        price > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addProduct else strings.editProduct) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imageBase64.isNullOrBlank()) strings.uploadProductImage else "Image Selected ✓")
                }

                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Medicine Name (EN)") })
                OutlinedTextField(value = nameUr, onValueChange = { nameUr = it }, label = { Text("Medicine Name (Urdu)") })
                OutlinedTextField(value = genericName, onValueChange = { genericName = it }, label = { Text("Generic Formula") })
                OutlinedTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = { Text("Manufacturer") })
                OutlinedTextField(value = packSize, onValueChange = { packSize = it }, label = { Text("Pack Size") })

                if (categories.isNotEmpty()) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(cat.nameEn) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Price PKR") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Disc %") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stock") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Overview (EN)") })
                OutlinedTextField(value = descUr, onValueChange = { descUr = it }, label = { Text("Overview (Urdu)") })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rxRequired, onCheckedChange = { rxRequired = it })
                    Text("Prescription Required (Rx)", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = popular, onCheckedChange = { popular = it })
                    Text("Featured Popular Product", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Product(
                            id = initial?.id ?: "",
                            nameEn = nameEn.trim(),
                            nameUr = nameUr.trim(),
                            genericName = genericName.trim(),
                            manufacturer = manufacturer.trim(),
                            categoryId = categoryId,
                            descriptionEn = descEn.trim().ifBlank { "Sealed manufacturer pharmacy pack." },
                            descriptionUr = descUr.trim().ifBlank { "مستند فارمیسی پیکنگ۔" },
                            packSize = packSize.trim().ifBlank { "1 Pack" },
                            pricePkr = price,
                            discountPercent = discount,
                            stock = stock,
                            prescriptionRequired = rxRequired,
                            popular = popular,
                            active = true,
                            imageUrl = initial?.imageUrl,
                            imageBase64 = imageBase64
                        )
                    )
                },
                enabled = !busy && valid
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun AdminCategoriesTab(adminViewModel: AdminViewModel) {
    val strings = LocalDocMedsStrings.current
    val categoriesState by adminViewModel.categoriesState.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()
    var editingCat by remember { mutableStateOf<MedicineCategory?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Medicine Categories", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    editingCat = null
                    showDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addCategory)
            }
        }

        when (val st = categoriesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                RegulatoryNoticeCard(text = st.message, isWarning = true)
            }
            is UiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(st.data, key = { it.id }) { cat ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CrispWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${cat.nameEn} / ${cat.nameUr}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Order #${cat.sortOrder} • Icon: ${cat.iconKey}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingCat = cat
                                        showDialog = true
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = strings.edit, tint = EmeraldPrimary)
                                }
                                IconButton(
                                    onClick = { adminViewModel.deleteCategory(cat.id) },
                                    enabled = !busy
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val initial = editingCat
        var nameEn by remember { mutableStateOf(initial?.nameEn ?: "") }
        var nameUr by remember { mutableStateOf(initial?.nameUr ?: "") }
        var iconKey by remember { mutableStateOf(initial?.iconKey ?: "medication") }
        var sortStr by remember { mutableStateOf(initial?.sortOrder?.toString() ?: "10") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (initial == null) strings.addCategory else strings.editCategory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Category Name (EN)") })
                    OutlinedTextField(value = nameUr, onValueChange = { nameUr = it }, label = { Text("Category Name (Urdu)") })
                    OutlinedTextField(
                        value = sortStr,
                        onValueChange = { sortStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Sort Order") }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("medication", "healing", "wellness", "heart", "monitor", "derma", "baby", "firstaid")) { key ->
                            FilterChip(
                                selected = iconKey == key,
                                onClick = { iconKey = key },
                                label = { Text(key) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.saveCategory(
                            MedicineCategory(
                                id = initial?.id ?: "",
                                nameEn = nameEn.trim(),
                                nameUr = nameUr.trim(),
                                iconKey = iconKey,
                                sortOrder = sortStr.toLongOrNull() ?: 10L,
                                active = true
                            )
                        ) { showDialog = false }
                    },
                    enabled = !busy && nameEn.trim().length >= 2 && nameUr.trim().length >= 2
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun AdminCustomersTab(
    adminViewModel: AdminViewModel,
    currentUserId: String,
    orders: List<PharmacyOrder>
) {
    val strings = LocalDocMedsStrings.current
    val customersState by adminViewModel.allCustomersState.collectAsStateWithLifecycle()
    val adminsList by adminViewModel.adminsList.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()

    val adminUids = adminsList.map { it.uid }.toSet()
    val orderCountByUid = orders.groupingBy { it.userId }.eachCount()

    when (val st = customersState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        is UiState.Error -> Box(Modifier.padding(16.dp)) {
            RegulatoryNoticeCard(text = st.message, isWarning = true)
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(st.data, key = { it.userId }) { user: UserProfile ->
                    val isUserAdmin = adminUids.contains(user.userId) ||
                        user.email.equals(AuthSessionRepository.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
                    val ordersCount = orderCountByUid[user.userId] ?: 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                    if (isUserAdmin) {
                                        Surface(color = EmeraldMint, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "ADMIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldDark,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                                Text(
                                    text = "${strings.customerOrdersCount(ordersCount)} • Lang: ${user.preferredLanguage.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDeep
                                )
                            }
                            if (user.userId != currentUserId &&
                                !user.email.equals(AuthSessionRepository.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
                            ) {
                                if (isUserAdmin) {
                                    OutlinedButton(
                                        onClick = { adminViewModel.revokeAdmin(user.userId) },
                                        enabled = !busy
                                    ) {
                                        Text(strings.revokeAdmin, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Button(
                                        onClick = { adminViewModel.grantAdmin(user) },
                                        enabled = !busy,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text(strings.grantAdmin, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
