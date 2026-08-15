package com.magictap.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magictap.R
import com.magictap.appContainer
import com.magictap.data.model.Pc
import com.magictap.ui.AppIcons
import com.magictap.data.model.Profile
import com.magictap.net.WakeOutcome
import com.magictap.ui.components.AppLogo
import com.magictap.ui.components.ConfirmDialog
import com.magictap.ui.components.EmptyState
import com.magictap.ui.pressScale
import com.magictap.ui.rememberHaptics
import com.magictap.ui.theme.BrandGradient
import com.magictap.ui.theme.brandWash
import com.magictap.ui.toUiMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onAddPc: (String) -> Unit,
    onEditPc: (profileId: String, pcId: String) -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val container = remember { context.appContainer }
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(container))

    val data by viewModel.data.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val selectedId by viewModel.selectedProfileId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(data.profiles.map { it.id }) { viewModel.syncSelection(data.profiles) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.outcome is WakeOutcome.Success) haptics.confirm() else haptics.reject()
            snackbarHostState.showSnackbar(event.outcome.toUiMessage(context))
        }
    }

    val selectedProfile = data.profiles.firstOrNull { it.id == selectedId }
        ?: data.profiles.firstOrNull()

    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    var pcToDelete by remember { mutableStateOf<Pc?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppLogo() },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(AppIcons.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = ready && selectedProfile != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(onClick = { haptics.click(); selectedProfile?.id?.let(onAddPc) }) {
                    Icon(AppIcons.Add, contentDescription = stringResource(R.string.main_add_pc))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .brandWash()
                .padding(padding),
        ) {
            when {
                !ready -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                data.profiles.isEmpty() -> EmptyState(
                    icon = AppIcons.Power,
                    title = stringResource(R.string.main_empty_title),
                    description = stringResource(R.string.main_empty_desc),
                    actionLabel = stringResource(R.string.main_empty_action),
                    onAction = onAddProfile,
                    actionIcon = AppIcons.Add,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Column(Modifier.fillMaxSize()) {
                    ProfileSelector(
                        profiles = data.profiles,
                        selectedId = selectedProfile?.id,
                        onSelect = viewModel::selectProfile,
                        onAddProfile = onAddProfile,
                        onEditProfile = { selectedProfile?.let { onEditProfile(it.id) } },
                        onDeleteProfile = { profileToDelete = selectedProfile },
                    )
                    PcList(
                        profile = selectedProfile,
                        onWake = { pc -> selectedProfile?.let { viewModel.wake(it, pc) } },
                        onEditPc = { pc -> selectedProfile?.let { onEditPc(it.id, pc.id) } },
                        onDeletePc = { pc -> pcToDelete = pc },
                    )
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.delete_profile_title),
            message = stringResource(R.string.delete_profile_message, profile.alias, profile.pcs.size),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteProfile(profile.id)
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null },
        )
    }
    pcToDelete?.let { pc ->
        val profileId = selectedProfile?.id
        ConfirmDialog(
            title = stringResource(R.string.delete_pc_title),
            message = stringResource(R.string.delete_pc_message, pc.alias),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                if (profileId != null) viewModel.deletePc(profileId, pc.id)
                pcToDelete = null
            },
            onDismiss = { pcToDelete = null },
        )
    }
}

@Composable
private fun ProfileSelector(
    profiles: List<Profile>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
) {
    val haptics = rememberHaptics()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(profiles, key = { it.id }) { profile ->
                FilterChip(
                    selected = profile.id == selectedId,
                    onClick = { haptics.click(); onSelect(profile.id) },
                    label = { Text(profile.alias) },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                AssistChip(
                    onClick = onAddProfile,
                    label = { Text(stringResource(R.string.profile_add)) },
                    leadingIcon = {
                        Icon(AppIcons.Add, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
        }

        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(AppIcons.More, contentDescription = stringResource(R.string.profile_menu_edit))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_menu_edit)) },
                    onClick = { menuOpen = false; onEditProfile() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_menu_delete)) },
                    onClick = { menuOpen = false; onDeleteProfile() },
                )
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun PcList(
    profile: Profile?,
    onWake: (Pc) -> Unit,
    onEditPc: (Pc) -> Unit,
    onDeletePc: (Pc) -> Unit,
) {
    if (profile == null) return
    if (profile.pcs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.main_no_pc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(profile.pcs, key = { it.id }) { pc ->
            PcRow(
                pc = pc,
                onWake = { onWake(pc) },
                onEdit = { onEditPc(pc) },
                onDelete = { onDeletePc(pc) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PcRow(
    pc: Pc,
    onWake: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { haptics.longPress(); menuOpen = true })
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.Power,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pc.alias,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pc.mac,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            val wakeInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { haptics.click(); onWake() },
                interactionSource = wakeInteraction,
                modifier = Modifier.pressScale(wakeInteraction),
            ) {
                Icon(AppIcons.Wake, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.main_wake))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(AppIcons.More, contentDescription = stringResource(R.string.pc_menu_edit))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pc_menu_edit)) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pc_menu_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
