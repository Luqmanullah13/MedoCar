package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrderStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.PrescriptionRecord
import com.example.data.model.PrescriptionStatus
import com.example.data.model.UiState
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PrescriptionStatusChip
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
import com.example.ui.theme.SoftBackground
import com.example.ui.viewmodel.AdminDashboardMetrics
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    currentUserId: String,
    onBackToStorefront: () -> Unit
) {
    BackHandler(onBack = onBackToStorefront)

    val strings = LocalDocMedsStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val statusMessage by adminViewModel.statusMessage.collectAsStateWithLifecycle()
    val metrics by adminViewModel.metrics.collectAsStateWithLifecycle()
    val ordersState by adminViewModel.allOrdersState.collectAsStateWithLifecycle()
    val rxState by adminViewModel.allPrescriptionsState.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            snackbarHostState.showSnackbar(statusMessage!!)
            adminViewModel.clearMessage()
        }
    }

    val tabs = listOf(
        strings.adminTabOverview to Icons.Filled.Analytics,
        strings.adminTabProducts to Icons.Filled.Medication,
        strings.adminTabCategories to Icons.Filled.Category,
        strings.adminTabOrders to Icons.Filled.LocalShipping,
        strings.adminTabRx to Icons.Filled.DocumentScanner,
        strings.adminTabCustomers to Icons.Filled.Group
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftBackground,
        topBar = {
            Surface(color = EmeraldDeep, shadowElevation = 4.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBackToStorefront,
                                modifier = Modifier.testTag("admin_back_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.backToStorefront,
                                    tint = CrispWhite
                                )
                            }
                            Column {
                                Text(
                                    text = strings.adminConsoleTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CrispWhite
                                )
                                Text(
                                    text = strings.adminBadgeTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldMint
                                )
                            }
                        }
                        TextButton(onClick = onBackToStorefront) {
                            Text(strings.backToStorefront, color = CrispWhite)
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CrispWhite,
                        contentColor = EmeraldDeep,
                        edgePadding = 12.dp
                    ) {
                        tabs.forEachIndexed { idx, (label, icon) ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(label) },
                                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("admin_tab_$idx")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when (selectedTab) {
                0 -> AdminOverviewTab(
                    metrics = metrics,
                    onJumpToTab = { selectedTab = it }
                )
                1 -> AdminProductsTab(adminViewModel = adminViewModel)
                2 -> AdminCategoriesTab(adminViewModel = adminViewModel)
                3 -> AdminOrdersTab(
                    ordersState = ordersState,
                    busy = busy,
                    onUpdateStatus = { order, status ->
                        adminViewModel.updateOrderStatus(order, status)
                    }
                )
                4 -> AdminRxReviewTab(
                    rxState = rxState,
                    busy = busy,
                    onReview = { rx, approved, notes, onDone ->
                        adminViewModel.reviewPrescription(rx, approved, notes, onDone)
                    }
                )
                5 -> AdminCustomersTab(
                    adminViewModel = adminViewModel,
                    currentUserId = currentUserId,
                    orders = (ordersState as? UiState.Success)?.data ?: emptyList()
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    metrics: AdminDashboardMetrics,
    onJumpToTab: (Int) -> Unit
) {
    val strings = LocalDocMedsStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RegulatoryNoticeCard(text = strings.pharmacistVerifiedNote)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    label = strings.totalRevenue,
                    value = formatPkr(metrics.totalRevenuePkr),
                    accent = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    label = strings.totalOrdersCount,
                    value = metrics.totalOrders.toString(),
                    accent = EmeraldDeep,
                    modifier = Modifier.weight(1f),
                    onClick = { onJumpToTab(3) }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    label = strings.pendingRxCount,
                    value = metrics.pendingRxCount.toString(),
                    accent = AmberRx,
                    modifier = Modifier.weight(1f),
                    onClick = { onJumpToTab(4) }
                )
                KpiCard(
                    label = "${strings.lowStockCount} / Out",
                    value = "${metrics.lowStockCount} / ${metrics.outOfStockCount}",
                    accent = CoralAlert,
                    modifier = Modifier.weight(1f),
                    onClick = { onJumpToTab(1) }
                )
            }
        }
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
                    Text("Order Pipeline Breakdown", style = MaterialTheme.typography.titleMedium)
                    val total = metrics.totalOrders.coerceAtLeast(1)
                    OrderStatus.entries.forEach { st ->
                        val count = metrics.statusBreakdown[st] ?: 0
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(st.name.replace('_', ' '), style = MaterialTheme.typography.bodySmall)
                                Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { count.toFloat() / total.toFloat() },
                                color = EmeraldPrimary,
                                trackColor = EmeraldMint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrispWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdminOrdersTab(
    ordersState: UiState<List<PharmacyOrder>>,
    busy: Boolean,
    onUpdateStatus: (PharmacyOrder, OrderStatus) -> Unit
) {
    val strings = LocalDocMedsStrings.current
    var filterStatus by remember { mutableStateOf<OrderStatus?>(null) }

    when (ordersState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        is UiState.Error -> Box(Modifier.padding(16.dp)) {
            RegulatoryNoticeCard(text = ordersState.message, isWarning = true)
        }
        is UiState.Success -> {
            val orders = ordersState.data.filter { filterStatus == null || it.parsedStatus == filterStatus }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterStatus == null,
                                onClick = { filterStatus = null },
                                label = { Text("All (${ordersState.data.size})") }
                            )
                        }
                        items(OrderStatus.entries) { st ->
                            FilterChip(
                                selected = filterStatus == st,
                                onClick = { filterStatus = st },
                                label = { Text(st.name.replace('_', ' ')) }
                            )
                        }
                    }
                }
                items(orders, key = { it.id }) { order ->
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${order.customerName} (${order.deliveryPhone})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                                OrderStatusChip(order.parsedStatus)
                            }
                            Text(
                                text = "${order.deliveryStreet}, ${order.deliveryCity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Text(
                                text = "COD: ${formatPkr(order.totalPkr)} • ${order.itemCount} items",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldDeep
                            )
                            if (order.requiresPrescription && order.prescriptionStatus != PrescriptionStatus.APPROVED.name) {
                                RegulatoryNoticeCard(
                                    text = "${strings.rxApprovalRequiredMsg} (Rx: ${order.prescriptionStatus})",
                                    isWarning = true
                                )
                            }
                            val rxBlocked = order.requiresPrescription &&
                                order.prescriptionStatus != PrescriptionStatus.APPROVED.name
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        OrderStatus.CONFIRMED,
                                        OrderStatus.PACKED,
                                        OrderStatus.OUT_FOR_DELIVERY,
                                        OrderStatus.DELIVERED,
                                        OrderStatus.CANCELLED
                                    )
                                ) { targetStatus ->
                                    val enabled = !busy &&
                                        order.parsedStatus != targetStatus &&
                                        !(rxBlocked && targetStatus != OrderStatus.CANCELLED)
                                    OutlinedButton(
                                        onClick = { onUpdateStatus(order, targetStatus) },
                                        enabled = enabled,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(targetStatus.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
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

@Composable
private fun AdminRxReviewTab(
    rxState: UiState<List<PrescriptionRecord>>,
    busy: Boolean,
    onReview: (PrescriptionRecord, Boolean, String, () -> Unit) -> Unit
) {
    val strings = LocalDocMedsStrings.current
    var inspectingRx by remember { mutableStateOf<PrescriptionRecord?>(null) }

    when (rxState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        is UiState.Error -> Box(Modifier.padding(16.dp)) {
            RegulatoryNoticeCard(text = rxState.message, isWarning = true)
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RegulatoryNoticeCard(text = strings.pharmacistVerifiedNote)
                }
                items(rxState.data, key = { it.id }) { rx ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CrispWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectingRx = rx }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Base64ImagePreview(
                                base64 = rx.imageBase64,
                                contentDescription = "Prescription",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(rx.customerName, style = MaterialTheme.typography.titleMedium)
                                Text(rx.customerEmail, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                                PrescriptionStatusChip(rx.parsedStatus)
                                if (!rx.pharmacistNotes.isNullOrBlank()) {
                                    Text(
                                        text = rx.pharmacistNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldDark
                                    )
                                }
                            }
                            Button(
                                onClick = { inspectingRx = rx },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Review")
                            }
                        }
                    }
                }
            }
        }
    }

    val target = inspectingRx
    if (target != null) {
        var notes by remember(target.id) { mutableStateOf(target.pharmacistNotes ?: "") }
        AlertDialog(
            onDismissRequest = { inspectingRx = null },
            title = { Text("Pharmacist Rx Review — ${target.customerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Base64ImagePreview(
                        base64 = target.imageBase64,
                        contentDescription = "Uploaded Prescription",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    if (!target.patientNote.isNullOrBlank()) {
                        Text(
                            text = "Patient Note: ${target.patientNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(500) },
                        label = { Text(strings.pharmacistNotesHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onReview(target, false, notes) { inspectingRx = null }
                        },
                        enabled = !busy && notes.trim().length >= 3,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.rejectRx)
                    }
                    Button(
                        onClick = {
                            onReview(target, true, notes) { inspectingRx = null }
                        },
                        enabled = !busy && notes.trim().length >= 3,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.approveRx)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectingRx = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
