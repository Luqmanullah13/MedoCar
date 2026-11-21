package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DeliveryAddress
import com.example.ui.auth.signOut
import com.example.ui.components.RegulatoryNoticeCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalDocMedsStrings
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoftSurface
import com.example.ui.theme.InkSecondary
import com.example.ui.viewmodel.StorefrontViewModel

@Composable
fun ProfileScreen(
    viewModel: StorefrontViewModel,
    onOpenAdminConsole: () -> Unit,
    onSignedOut: () -> Unit
) {
    val strings = LocalDocMedsStrings.current
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val busy by viewModel.actionBusy.collectAsStateWithLifecycle()

    var editingName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var editingPhone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
    var addressDialogTarget by remember { mutableStateOf<DeliveryAddress?>(null) }
    var showNewAddressDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CrispWhite, shadowElevation = 2.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(text = strings.profileTitle, style = MaterialTheme.typography.headlineSmall)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Info & Profile Edit Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CrispWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = EmeraldSoftSurface,
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName?.ifBlank { "DocMeds User" } ?: "DocMeds User",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                                if (isAdmin) {
                                    Surface(
                                        color = EmeraldMint,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = strings.adminBadgeTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text(strings.recipientNameHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editingPhone,
                            onValueChange = { editingPhone = it },
                            label = { Text(strings.phoneHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.updateProfileDetails(editingName, editingPhone) },
                            enabled = !busy && editingName.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.save)
                        }
                    }
                }
            }

            // Role-Gated Admin Console Entry (ONLY visible to verified Admins!)
            if (isAdmin) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSoftSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_pharmacist_avatar),
                                contentDescription = strings.adminBadgeTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.adminBadgeTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "Manage products, categories, Rx approvals, orders & customers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                            Button(
                                onClick = onOpenAdminConsole,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                                modifier = Modifier.testTag("profile_open_admin_btn")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin")
                            }
                        }
                    }
                }
            }

            // Bilingual Language Switcher
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CrispWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = EmeraldPrimary)
                            Column {
                                Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (lang == AppLanguage.EN) "English (LTR)" else "اردو (RTL)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.toggleLanguage() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("profile_language_toggle")
                        ) {
                            Text(if (lang == AppLanguage.EN) "Switch to اردو" else "Switch to English")
                        }
                    }
                }
            }

            // Saved Delivery Addresses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.savedAddressesTitle, style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = { showNewAddressDialog = true },
                        modifier = Modifier.testTag("profile_add_address_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addAddress)
                    }
                }
            }

            items(addresses, key = { it.id }) { addr ->
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(addr.label, style = MaterialTheme.typography.titleMedium)
                                if (addr.defaultAddress) {
                                    Surface(color = EmeraldMint, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${addr.recipientName} • ${addr.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Text(
                                text = "${addr.street}, ${addr.city}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        IconButton(onClick = { addressDialogTarget = addr }) {
                            Icon(Icons.Filled.Edit, contentDescription = strings.edit, tint = EmeraldPrimary)
                        }
                        IconButton(onClick = { viewModel.deleteAddress(addr.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CoralAlert)
                        }
                    }
                }
            }

            item {
                RegulatoryNoticeCard(text = strings.noMedicalAdviceDisclaimer)
            }

            // Sign Out
            item {
                OutlinedButton(
                    onClick = {
                        signOut(credentialManager, scope) {
                            onSignedOut()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut)
                }
            }
        }
    }

    if (showNewAddressDialog) {
        AddressEditorDialog(
            initial = null,
            onDismiss = { showNewAddressDialog = false },
            onSave = { addr -> viewModel.saveAddress(addr) { showNewAddressDialog = false } }
        )
    }

    val targetEdit = addressDialogTarget
    if (targetEdit != null) {
        AddressEditorDialog(
            initial = targetEdit,
            onDismiss = { addressDialogTarget = null },
            onSave = { addr -> viewModel.saveAddress(addr) { addressDialogTarget = null } }
        )
    }
}
