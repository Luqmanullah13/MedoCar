package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrderStatus
import com.example.data.model.PharmacyOrder
import com.example.data.model.PrescriptionRecord
import com.example.data.model.UiState
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PrescriptionStatusChip
import com.example.ui.components.RegulatoryNoticeCard
import com.example.ui.components.formatPkr
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.StorefrontViewModel
import com.example.util.ImageCodec

@Composable
fun OrdersAndRxScreen(
    viewModel: StorefrontViewModel,
    initialTab: Int = 0
) {
    val strings = LocalDocMedsStrings.current
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    val ordersState by viewModel.userOrdersState.collectAsStateWithLifecycle()
    val rxState by viewModel.userPrescriptionsState.collectAsStateWithLifecycle()
    val busy by viewModel.actionBusy.collectAsStateWithLifecycle()

    var showRxUploadDialog by remember { mutableStateOf(false) }
    var pendingRxBase64 by remember { mutableStateOf<String?>(null) }
    var pendingRxNote by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingRxBase64 = ImageCodec.uriToCompressedDataUri(context, uri)
            if (pendingRxBase64 != null) showRxUploadDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CrispWhite, shadowElevation = 2.dp) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) strings.ordersTitle else strings.prescriptionsTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("orders_upload_rx_btn")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.uploadStandaloneRx, style = MaterialTheme.typography.labelMedium)
                    }
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = CrispWhite) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.ordersTitle) },
                        icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.prescriptionsTitle) },
                        icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            when (val st = ordersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
                is UiState.Error -> Box(Modifier.padding(16.dp)) {
                    RegulatoryNoticeCard(text = st.message, isWarning = true)
                }
                is UiState.Success -> {
                    if (st.data.isEmpty()) {
                        EmptyOrdersView(strings.noOrdersYet)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(st.data, key = { it.id }) { order ->
                                OrderCard(
                                    order = order,
                                    canCancel = !busy && (
                                        order.parsedStatus == OrderStatus.AWAITING_RX_VERIFICATION ||
                                            order.parsedStatus == OrderStatus.CONFIRMED
                                        ),
                                    cancelLabel = strings.cancelOrder,
                                    onCancel = { viewModel.cancelOrder(order.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when (val st = rxState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
                is UiState.Error -> Box(Modifier.padding(16.dp)) {
                    RegulatoryNoticeCard(text = st.message, isWarning = true)
                }
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            RegulatoryNoticeCard(text = strings.pharmacistVerifiedNote)
                        }
                        if (st.data.isEmpty()) {
                            item {
                                EmptyOrdersView(strings.noPrescriptionsYet)
                            }
                        } else {
                            items(st.data, key = { it.id }) { rx ->
                                PrescriptionCard(rx)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRxUploadDialog && pendingRxBase64 != null) {
        AlertDialog(
            onDismissRequest = { showRxUploadDialog = false },
            title = { Text(strings.uploadStandaloneRx) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Base64ImagePreview(
                        base64 = pendingRxBase64 ?: "",
                        contentDescription = strings.uploadStandaloneRx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    OutlinedTextField(
                        value = pendingRxNote,
                        onValueChange = { pendingRxNote = it.take(300) },
                        label = { Text(strings.patientNoteHint) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val b64 = pendingRxBase64 ?: return@Button
                        viewModel.submitStandalonePrescription(b64, pendingRxNote) {
                            showRxUploadDialog = false
                            pendingRxBase64 = null
                            pendingRxNote = ""
                            selectedTab = 1
                        }
                    },
                    enabled = !busy
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRxUploadDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: PharmacyOrder,
    canCancel: Boolean,
    cancelLabel: String,
    onCancel: () -> Unit
) {
    val parsedItems = order.parsedItems()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrispWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.addressLabel} • ${order.deliveryCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                OrderStatusChip(order.parsedStatus)
            }

            // Tracking Timeline Steps
            if (order.parsedStatus != OrderStatus.CANCELLED && order.parsedStatus != OrderStatus.REJECTED) {
                OrderTimeline(order.parsedStatus, order.requiresPrescription)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            parsedItems.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${line.qty}x ${line.nameEn}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatPkr(line.unitPricePkr * line.qty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!order.pharmacistNote.isNullOrBlank()) {
                RegulatoryNoticeCard(
                    text = "Pharmacist Note: ${order.pharmacistNote}",
                    isWarning = order.parsedStatus == OrderStatus.REJECTED
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("COD Total", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                    Text(
                        text = formatPkr(order.totalPkr),
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldDeep,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert)
                    ) {
                        Text(cancelLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTimeline(status: OrderStatus, requiresRx: Boolean) {
    val steps = if (requiresRx) {
        listOf(
            OrderStatus.AWAITING_RX_VERIFICATION to "Rx Review",
            OrderStatus.CONFIRMED to "Confirmed",
            OrderStatus.PACKED to "Packed",
            OrderStatus.OUT_FOR_DELIVERY to "On Way",
            OrderStatus.DELIVERED to "Delivered"
        )
    } else {
        listOf(
            OrderStatus.CONFIRMED to "Confirmed",
            OrderStatus.PACKED to "Packed",
            OrderStatus.OUT_FOR_DELIVERY to "On Way",
            OrderStatus.DELIVERED to "Delivered"
        )
    }
    val currentIndex = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, (_, label) ->
            val reached = idx <= currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (reached) EmeraldPrimary else EmeraldMint)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) EmeraldDeep else Color(0xFF94A3B8),
                    fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun PrescriptionCard(rx: PrescriptionRecord) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CrispWhite),
        modifier = Modifier.fillMaxWidth()
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrescriptionStatusChip(rx.parsedStatus)
                if (!rx.patientNote.isNullOrBlank()) {
                    Text(
                        text = rx.patientNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                if (!rx.pharmacistNotes.isNullOrBlank()) {
                    Text(
                        text = "Pharmacist: ${rx.pharmacistNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldDeep,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ReceiptLong,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(56.dp)
        )
        Text(text = message, style = MaterialTheme.typography.titleMedium, color = InkSecondary)
    }
}
