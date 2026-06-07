package io.github.ntufar.babyonboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ntufar.babyonboard.domain.model.ContactRole
import io.github.ntufar.babyonboard.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var autoStart by remember(settings) { mutableStateOf(settings?.autoStart ?: false) }
    var dndEnabled by remember(settings) { mutableStateOf(settings?.dndInTrip ?: true) }
    var emergencyNumber by remember(settings) { mutableStateOf(settings?.emergencyNumber ?: "112") }
    var units by remember(settings) { mutableStateOf(settings?.units ?: "km") }
    var retentionDays by remember(settings) { mutableStateOf(settings?.retentionDays ?: 30) }
    var reminderEscalation by remember(settings) { mutableStateOf(settings?.reminderEscalation ?: 1) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    var showAddContactDialog by remember { mutableStateOf(false) }

    val unitOptions = listOf("km", "mi")
    val retentionOptions = listOf(7, 14, 30, 60, 90)
    val escalationOptions = listOf(1 to "Once", 2 to "Two alerts", 3 to "Three alerts")

    LaunchedEffect(settings) {
        settings?.let {
            autoStart = it.autoStart
            dndEnabled = it.dndInTrip
            emergencyNumber = it.emergencyNumber
            units = it.units
            retentionDays = it.retentionDays
            reminderEscalation = it.reminderEscalation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingHeader("Trip & Recording") }

            item {
                SettingsSwitchCard(
                    title = "Auto-Start Trip",
                    description = "Automatically start recording when driving is detected",
                    checked = autoStart,
                    onCheckedChange = { autoStart = it }
                )
            }

            item {
                SettingsSwitchCard(
                    title = "Do Not Disturb",
                    description = "Silence notifications during trips",
                    checked = dndEnabled,
                    onCheckedChange = { dndEnabled = it }
                )
            }

            item {
                SettingsSelectorCard(
                    title = "Units",
                    description = "Distance unit for display",
                    options = unitOptions,
                    selected = units,
                    onSelected = { units = it }
                )
            }

            item { SettingHeader("Safety & Alerts") }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Emergency Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emergencyNumber,
                            onValueChange = { emergencyNumber = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SettingsSelectorCard(
                    title = "Reminder Escalation",
                    description = "How many times to repeat the back-seat reminder",
                    options = escalationOptions.map { it.second },
                    selected = escalationOptions.first { it.first == reminderEscalation }.second,
                    onSelected = { label ->
                        reminderEscalation = escalationOptions.first { it.second == label }.first
                    }
                )
            }

            item { SettingHeader("Data Management") }

            item {
                SettingsSelectorCard(
                    title = "Retention Period",
                    description = "Days to keep trip history",
                    options = retentionOptions.map { "$it days" },
                    selected = "${retentionDays} days",
                    onSelected = { label ->
                        retentionDays = retentionOptions.first { "$it days" == label }
                    }
                )
            }

            item { SettingHeader("Emergency Contacts") }

            items(contacts, key = { it.id }) { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${contact.phone} · ${contact.role.name.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteContact(contact.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete contact",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showAddContactDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Contact")
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                savedMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            settings?.copy(
                                autoStart = autoStart,
                                dndInTrip = dndEnabled,
                                emergencyNumber = emergencyNumber,
                                units = units,
                                retentionDays = retentionDays,
                                reminderEscalation = reminderEscalation
                            ) ?: return@Button
                        )
                        savedMessage = "Settings saved"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Save Settings")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, phone, role ->
                viewModel.addContact(name, phone, role)
                showAddContactDialog = false
            }
        )
    }
}

@Composable
private fun SettingHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSelectorCard(
    title: String,
    description: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, ContactRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(ContactRole.EMERGENCY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == ContactRole.EMERGENCY,
                        onClick = { role = ContactRole.EMERGENCY },
                        label = { Text("Emergency") }
                    )
                    FilterChip(
                        selected = role == ContactRole.ARRIVAL,
                        onClick = { role = ContactRole.ARRIVAL },
                        label = { Text("Arrival") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, phone, role) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
