package com.example.erp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.erp.data.PaymentProfile
import com.example.erp.data.PaymentProfileType
import com.example.erp.data.isKnownVenezuelanBank
import com.example.erp.data.venezuelanBanks
import com.example.erp.ui.theme.accentColor
import com.example.erp.ui.theme.cardBadgeColor
import com.example.erp.ui.theme.cardBorder
import com.example.erp.ui.theme.cardBorderColor
import com.example.erp.ui.theme.cardContainerColor
import java.util.UUID

/**
 * Manage the local payment profiles: one type at a time, with its own list and
 * form.
 *
 * The screen is a window over the app like Settings: it owns the back gesture
 * and keeps the form in the same composition so a validation error never costs
 * the reader the values already typed. Profiles are edited through the lifted
 * list in [profiles]; this screen stores nothing itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentProfilesScreen(
    profiles: List<PaymentProfile>,
    onSave: (PaymentProfile) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(PaymentProfileType.PAGO_MOVIL) }
    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PaymentProfile?>(null) }

    val editing = editingId?.let { id -> profiles.firstOrNull { it.id == id } }
    val formType = editing?.type ?: selectedType

    fun closeForm() {
        showForm = false
        editingId = null
    }

    fun handleBack() {
        if (showForm) closeForm() else onBack()
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (showForm) {
                            val action = if (editing == null) "Agregar" else "Editar"
                            "$action ${formType.typeLabel}"
                        } else {
                            "Perfiles de pago"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (showForm) {
            ProfileForm(
                type = formType,
                initial = editing,
                onSave = { profile ->
                    onSave(profile)
                    closeForm()
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            ProfileList(
                profiles = profiles,
                selectedType = selectedType,
                onSelectType = { selectedType = it },
                onAdd = {
                    editingId = null
                    showForm = true
                },
                onEdit = { profile ->
                    editingId = profile.id
                    showForm = true
                },
                onDelete = { profile -> deleteTarget = profile },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            modifier = Modifier.border(1.dp, cardBorderColor(), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(text = "Eliminar perfil", fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    text = "¿Eliminar «${target.nombre.ifBlank { target.typeLabel }}»?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProfileList(
    profiles: List<PaymentProfile>,
    selectedType: PaymentProfileType,
    onSelectType: (PaymentProfileType) -> Unit,
    onAdd: () -> Unit,
    onEdit: (PaymentProfile) -> Unit,
    onDelete: (PaymentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = profiles.filter { it.type == selectedType }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        TypePills(
            selected = selectedType,
            counts = profiles.groupingBy { it.type }.eachCount(),
            onSelect = onSelectType
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (visible.isEmpty()) {
            EmptyState(type = selectedType, onAdd = onAdd)
        } else {
            visible.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    onEdit = { onEdit(profile) },
                    onDelete = { onDelete(profile) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar ${selectedType.addLabel}")
            }
        }
    }
}

@Composable
private fun TypePills(
    selected: PaymentProfileType,
    counts: Map<PaymentProfileType, Int>,
    onSelect: (PaymentProfileType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentProfileType.entries.forEach { type ->
            val isSelected = selected == type
            val count = counts[type] ?: 0

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accentColor() else cardBadgeColor())
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else cardBorderColor(),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (count > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Color.Black.copy(alpha = 0.20f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    type: PaymentProfileType,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = type.emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar ${type.addLabel}")
        }
    }
}

@Composable
private fun ProfileCard(
    profile: PaymentProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = cardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.nombre.ifBlank { profile.typeLabel },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profile.typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (profile.summaryLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.summaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileForm(
    type: PaymentProfileType,
    initial: PaymentProfile?,
    onSave: (PaymentProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keyed by the edited profile id so opening a different profile starts from
    // its values instead of the previous draft.
    var nombre by remember(initial?.id) { mutableStateOf(initial?.nombre.orEmpty()) }
    var telefono by remember(initial?.id) { mutableStateOf(initial?.telefono.orEmpty()) }
    var documentoTipo by remember(initial?.id) {
        mutableStateOf(initial?.documentoTipo?.takeIf { it.isNotBlank() } ?: "V")
    }
    var documentoNumero by remember(initial?.id) { mutableStateOf(initial?.documentoNumero.orEmpty()) }
    var banco by remember(initial?.id) { mutableStateOf(initial?.banco.orEmpty()) }
    var numeroCuenta by remember(initial?.id) { mutableStateOf(initial?.numeroCuenta.orEmpty()) }
    var showErrors by remember(initial?.id) { mutableStateOf(false) }
    var bankMenuOpen by remember { mutableStateOf(false) }

    val nombreValid = nombre.isNotBlank()
    val telefonoValid = telefono.count { it.isDigit() } >= 10
    val documentoValid = documentoNumero.isNotBlank()
    // The bank has to be one of the real ones: the field is a picker, so
    // anything else is a name someone typed by hand.
    val bancoValid = isKnownVenezuelanBank(banco)
    val cuentaValid = numeroCuenta.isNotBlank()

    val formValid = when (type) {
        PaymentProfileType.PAGO_MOVIL -> nombreValid && telefonoValid && documentoValid && bancoValid
        PaymentProfileType.CUENTA_BANCO -> nombreValid && bancoValid && cuentaValid && documentoValid
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            isError = showErrors && !nombreValid,
            supportingText = supportingError(showErrors && !nombreValid, "Escribe un nombre."),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        when (type) {
            PaymentProfileType.PAGO_MOVIL -> {
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Número de teléfono") },
                    isError = showErrors && !telefonoValid,
                    supportingText = supportingError(
                        showErrors && !telefonoValid,
                        "Ingresa al menos 10 dígitos."
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                DocumentRow(
                    documentoTipo = documentoTipo,
                    onDocumentoTipoChange = { documentoTipo = it },
                    documentoNumero = documentoNumero,
                    onDocumentoNumeroChange = { documentoNumero = it },
                    showError = showErrors && !documentoValid
                )
                BankField(
                    banco = banco,
                    onBancoChange = { banco = it },
                    showError = showErrors && !bancoValid,
                    expanded = bankMenuOpen,
                    onExpandedChange = { bankMenuOpen = it }
                )
            }

            PaymentProfileType.CUENTA_BANCO -> {
                BankField(
                    banco = banco,
                    onBancoChange = { banco = it },
                    showError = showErrors && !bancoValid,
                    expanded = bankMenuOpen,
                    onExpandedChange = { bankMenuOpen = it }
                )
                OutlinedTextField(
                    value = numeroCuenta,
                    onValueChange = { numeroCuenta = it },
                    label = { Text("Número de cuenta") },
                    isError = showErrors && !cuentaValid,
                    supportingText = supportingError(
                        showErrors && !cuentaValid,
                        "Ingresa el número de cuenta."
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                DocumentRow(
                    documentoTipo = documentoTipo,
                    onDocumentoTipoChange = { documentoTipo = it },
                    documentoNumero = documentoNumero,
                    onDocumentoNumeroChange = { documentoNumero = it },
                    showError = showErrors && !documentoValid
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (!formValid) {
                    showErrors = true
                    return@Button
                }
                val base = initial ?: PaymentProfile(
                    id = UUID.randomUUID().toString(),
                    type = type
                )
                onSave(
                    when (type) {
                        PaymentProfileType.PAGO_MOVIL -> base.copy(
                            nombre = nombre.trim(),
                            telefono = telefono.trim(),
                            documentoTipo = documentoTipo,
                            documentoNumero = documentoNumero.trim(),
                            banco = banco.trim()
                        )

                        PaymentProfileType.CUENTA_BANCO -> base.copy(
                            nombre = nombre.trim(),
                            banco = banco.trim(),
                            numeroCuenta = numeroCuenta.trim(),
                            documentoTipo = documentoTipo,
                            documentoNumero = documentoNumero.trim()
                        )
                    }
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Guardar")
        }
    }
}

@Composable
private fun DocumentRow(
    documentoTipo: String,
    onDocumentoTipoChange: (String) -> Unit,
    documentoNumero: String,
    onDocumentoNumeroChange: (String) -> Unit,
    showError: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("V", "E", "J").forEach { tipo ->
                val isSelected = documentoTipo == tipo
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor() else cardBadgeColor())
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else cardBorderColor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDocumentoTipoChange(tipo) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tipo,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedTextField(
                value = documentoNumero,
                onValueChange = { entered -> onDocumentoNumeroChange(entered.filter { it.isDigit() }) },
                label = { Text("Documento") },
                isError = showError,
                supportingText = supportingError(showError, "Ingresa el número de documento."),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "V Venezolano · E Extranjero · J Jurídico",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BankField(
    banco: String,
    onBancoChange: (String) -> Unit,
    showError: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    BoxWithConstraints {
        val menuWidth = maxWidth
        OutlinedTextField(
            value = banco,
            // The value only ever comes from the catalog below. The field
            // cannot be typed into: a half-written bank name is a payment
            // that never arrives.
            onValueChange = {},
            enabled = false,
            label = { Text("Banco") },
            isError = showError,
            supportingText = supportingError(showError, "Elegí un banco de la lista."),
            singleLine = true,
            // Disabled is what keeps the keyboard away and the cursor out of
            // the field. The colors are restored so it reads as a normal
            // field that happens to be filled by a picker, not as something
            // broken.
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (showError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                },
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                // The arrow is the only way in: it opens the catalog.
                IconButton(onClick = { onExpandedChange(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Elegir banco"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            // Six visible banks plus the menu padding. The menu scrolls
            // internally, so the rest of the catalog stays reachable without
            // covering the whole form. Matching the field width also keeps
            // the form's buttons from peeking out beside the menu.
            modifier = Modifier
                .width(menuWidth)
                .heightIn(max = 300.dp)
        ) {
            venezuelanBanks.forEach { bank ->
                DropdownMenuItem(
                    text = { Text(bank.label) },
                    onClick = {
                        onBancoChange(bank.label)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

/** Null when there is nothing to report, so the field keeps its normal height. */
@Composable
private fun supportingError(visible: Boolean, message: String): (@Composable () -> Unit)? =
    if (visible) {
        { Text(message) }
    } else {
        null
    }

private val PaymentProfileType.typeLabel: String
    get() = when (this) {
        PaymentProfileType.PAGO_MOVIL -> "Pago móvil"
        PaymentProfileType.CUENTA_BANCO -> "Cuenta de banco"
    }

/** Lowercase fragment used in "Agregar …". */
private val PaymentProfileType.addLabel: String
    get() = when (this) {
        PaymentProfileType.PAGO_MOVIL -> "pago móvil"
        PaymentProfileType.CUENTA_BANCO -> "cuenta de banco"
    }

private val PaymentProfileType.emptyMessage: String
    get() = when (this) {
        PaymentProfileType.PAGO_MOVIL -> "No tienes perfiles de pago móvil todavía."
        PaymentProfileType.CUENTA_BANCO -> "No tienes perfiles de cuenta de banco todavía."
    }
