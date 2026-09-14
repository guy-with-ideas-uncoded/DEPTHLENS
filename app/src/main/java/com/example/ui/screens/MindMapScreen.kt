package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.HigherSelfView
import com.example.ui.components.MindMapVisualizer
import com.example.ui.theme.ThemeManager
import com.example.ui.viewmodel.IntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class MindMapViewMode {
    VISUAL_MAP,
    STRUCTURED_TREE,
    HIGHER_SELF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: IntelligenceViewModel,
    onNavigateBack: () -> Unit
) {
    val activeNodes by viewModel.allActiveIdentityNodes.collectAsStateWithLifecycle()
    val categoryGroups by viewModel.identityCategoryGroups.collectAsStateWithLifecycle()
    val summary by viewModel.userIdentitySummary.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizingMindMap.collectAsStateWithLifecycle()
    val synthesisStatus by viewModel.mindMapSynthesisStatus.collectAsStateWithLifecycle()
    val higherSelfProfile by viewModel.higherSelfProfile.collectAsStateWithLifecycle()
    val currentReflection by viewModel.currentSelfReflection.collectAsStateWithLifecycle()
    val isReflecting by viewModel.isReflecting.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(MindMapViewMode.VISUAL_MAP) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterClassification by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var activeModalTrait by remember { mutableStateOf<IdentityNodeEntity?>(null) }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var traitToEdit by remember { mutableStateOf<IdentityNodeEntity?>(null) }
    var traitToDelete by remember { mutableStateOf<IdentityNodeEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var categoryToClear by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val filteredNodes = remember(activeNodes, searchQuery, selectedFilterClassification, selectedCategoryFilter) {
        activeNodes.filter { node ->
            val matchesQuery = searchQuery.isBlank() ||
                    node.title.contains(searchQuery, ignoreCase = true) ||
                    node.detail.contains(searchQuery, ignoreCase = true) ||
                    node.category.contains(searchQuery, ignoreCase = true) ||
                    node.supportingEvidence.contains(searchQuery, ignoreCase = true)

            val matchesClassification = selectedFilterClassification == null ||
                    node.classification == selectedFilterClassification

            val matchesCategory = selectedCategoryFilter == null ||
                    node.category == selectedCategoryFilter

            matchesQuery && matchesClassification && matchesCategory
        }
    }

    val displayCategoryGroups = remember(filteredNodes) {
        val grouped = filteredNodes.groupBy { it.category }
        grouped.map { (cat, nodes) ->
            MindMapCategoryGroup(
                category = cat,
                nodes = nodes,
                subcategories = nodes.groupBy { it.subcategory ?: "General" }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Identity & Mind Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = ThemeManager.accentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Evolving Profile",
                                    color = ThemeManager.accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "DepthLens's long-term semantic understanding of you",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                        )
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(
                        onClick = {
                            viewMode = when (viewMode) {
                                MindMapViewMode.VISUAL_MAP -> MindMapViewMode.STRUCTURED_TREE
                                MindMapViewMode.STRUCTURED_TREE -> MindMapViewMode.HIGHER_SELF
                                MindMapViewMode.HIGHER_SELF -> MindMapViewMode.VISUAL_MAP
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (viewMode) {
                                MindMapViewMode.VISUAL_MAP -> Icons.Default.AccountTree
                                MindMapViewMode.STRUCTURED_TREE -> Icons.Default.Psychology
                                MindMapViewMode.HIGHER_SELF -> Icons.Default.Hub
                            },
                            contentDescription = "Toggle View",
                            tint = ThemeManager.accentColor
                        )
                    }

                    // More actions menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(
                                if (ThemeManager.isDarkTheme) Color(0xFF1E293B) else Color.White
                            )
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Manual Trait") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    traitToEdit = null
                                    showAddEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan Chat History") },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.triggerMindMapSynthesisFromHistory()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear All Traits", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    showClearAllConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    traitToEdit = null
                    showAddEditDialog = true
                },
                containerColor = ThemeManager.accentColor,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Trait", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        containerColor = if (ThemeManager.isDarkTheme) Color(0xFF0A0E1A) else Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Synthesis Progress Banner
            AnimatedVisibility(
                visible = isSynthesizing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = ThemeManager.accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ThemeManager.accentColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = synthesisStatus,
                                color = ThemeManager.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = ThemeManager.accentColor,
                            trackColor = ThemeManager.accentColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Summary Analytics Stats Strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(
                        label = "Total Traits",
                        value = "${summary.totalNodes}",
                        color = ThemeManager.accentColor
                    )
                    StatDivider()
                    StatBadge(
                        label = "Facts",
                        value = "${summary.explicitFactsCount}",
                        color = Color(0xFF00E5FF)
                    )
                    StatDivider()
                    StatBadge(
                        label = "Patterns",
                        value = "${summary.strongPatternsCount}",
                        color = Color(0xFFA855F7)
                    )
                    StatDivider()
                    StatBadge(
                        label = "Inferences",
                        value = "${summary.inferencesCount}",
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Mode Tab Selector Row (Visual Map, Tree, Higher Self)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeTabChip(
                    icon = Icons.Default.Hub,
                    label = "Visual Map",
                    selected = viewMode == MindMapViewMode.VISUAL_MAP,
                    onClick = { viewMode = MindMapViewMode.VISUAL_MAP },
                    modifier = Modifier.weight(1f)
                )
                ModeTabChip(
                    icon = Icons.Default.AccountTree,
                    label = "Tree",
                    selected = viewMode == MindMapViewMode.STRUCTURED_TREE,
                    onClick = { viewMode = MindMapViewMode.STRUCTURED_TREE },
                    modifier = Modifier.weight(1f)
                )
                ModeTabChip(
                    icon = Icons.Default.Psychology,
                    label = "Higher Self",
                    badge = "Guide",
                    selected = viewMode == MindMapViewMode.HIGHER_SELF,
                    onClick = { viewMode = MindMapViewMode.HIGHER_SELF },
                    modifier = Modifier.weight(1.25f)
                )
            }

            if (viewMode != MindMapViewMode.HIGHER_SELF) {
                // Search Bar & Filter Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search traits, evidence, categories...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeManager.accentColor,
                            unfocusedBorderColor = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                            focusedContainerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White,
                            unfocusedContainerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Classification and category filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilterClassification == null && selectedCategoryFilter == null,
                                onClick = {
                                    selectedFilterClassification = null
                                    selectedCategoryFilter = null
                                },
                                label = { Text("All (${activeNodes.size})", fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilterClassification == EpistemicClassification.EXPLICIT_FACT.id,
                                onClick = {
                                    selectedFilterClassification = if (selectedFilterClassification == EpistemicClassification.EXPLICIT_FACT.id) null else EpistemicClassification.EXPLICIT_FACT.id
                                },
                                label = { Text("Facts (${summary.explicitFactsCount})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFF00E5FF)
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilterClassification == EpistemicClassification.STRONG_PATTERN.id,
                                onClick = {
                                    selectedFilterClassification = if (selectedFilterClassification == EpistemicClassification.STRONG_PATTERN.id) null else EpistemicClassification.STRONG_PATTERN.id
                                },
                                label = { Text("Patterns (${summary.strongPatternsCount})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFA855F7).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFA855F7)
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilterClassification == EpistemicClassification.INFERENCE.id,
                                onClick = {
                                    selectedFilterClassification = if (selectedFilterClassification == EpistemicClassification.INFERENCE.id) null else EpistemicClassification.INFERENCE.id
                                },
                                label = { Text("Inferences (${summary.inferencesCount})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFF59E0B)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                when (viewMode) {
                    MindMapViewMode.HIGHER_SELF -> {
                        HigherSelfView(
                            profile = higherSelfProfile,
                            reflection = currentReflection,
                            isReflecting = isReflecting,
                            onReflectRequested = { dilemma ->
                                viewModel.requestSelfReflection(dilemma)
                            },
                            onDiscussInChat = { query ->
                                viewModel.sendQuery(query)
                                onNavigateBack()
                            },
                            onAddTrait = {
                                traitToEdit = null
                                showAddEditDialog = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MindMapViewMode.VISUAL_MAP -> {
                        MindMapVisualizer(
                            groups = displayCategoryGroups,
                            userName = userName,
                            selectedCategory = selectedCategoryFilter,
                            onCategorySelected = { selectedCategoryFilter = it },
                            onTraitSelected = { activeModalTrait = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MindMapViewMode.STRUCTURED_TREE -> {
                        if (displayCategoryGroups.isEmpty()) {
                            EmptyMindMapState(onAddTrait = { showAddEditDialog = true })
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(displayCategoryGroups, key = { it.category }) { group ->
                                    CategoryTreeCard(
                                        group = group,
                                        onTraitClick = { activeModalTrait = it },
                                        onEditTrait = { traitToEdit = it; showAddEditDialog = true },
                                        onDeleteTrait = { traitToDelete = it },
                                        onClearCategory = { categoryToClear = group.category }
                                    )
                                }
                                item {
                                    Spacer(Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }

                // Trait Detail Modal Sheet / Floating Card
                activeModalTrait?.let { trait ->
                    TraitDetailCard(
                        trait = trait,
                        onDismiss = { activeModalTrait = null },
                        onEdit = {
                            traitToEdit = trait
                            activeModalTrait = null
                            showAddEditDialog = true
                        },
                        onDelete = {
                            traitToDelete = trait
                            activeModalTrait = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditTraitDialog(
            initialTrait = traitToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { category, subcategory, title, detail, classification, confidence ->
                if (traitToEdit == null) {
                    viewModel.addManualIdentityTrait(
                        category = category,
                        subcategory = subcategory,
                        title = title,
                        detail = detail,
                        classification = classification,
                        confidence = confidence
                    )
                } else {
                    val updated = traitToEdit!!.copy(
                        category = category,
                        subcategory = subcategory,
                        title = title,
                        detail = detail,
                        classification = classification.id,
                        confidence = confidence
                    )
                    viewModel.updateIdentityTrait(updated)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete confirmation dialog
    traitToDelete?.let { trait ->
        AlertDialog(
            onDismissRequest = { traitToDelete = null },
            title = { Text("Delete Trait?") },
            text = { Text("Are you sure you want to remove \"${trait.title}\" from your identity profile?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIdentityTrait(trait.id)
                        traitToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { traitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Category confirmation dialog
    categoryToClear?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToClear = null },
            title = { Text("Clear Category?") },
            text = { Text("Remove all traits under \"$category\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearIdentityCategory(category)
                        categoryToClear = null
                    }
                ) {
                    Text("Clear", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToClear = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All confirmation dialog
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("Reset Entire Mind Map?") },
            text = { Text("This will erase all persistent identity traits, patterns, and insights learned about you across all conversations. DepthLens will start understanding you from scratch.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllIdentityTraits()
                        showClearAllConfirmDialog = false
                    }
                ) {
                    Text("Reset All", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
        )
    }
}

@Composable
fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(
                if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
            )
    )
}

@Composable
fun CategoryTreeCard(
    group: MindMapCategoryGroup,
    onTraitClick: (IdentityNodeEntity) -> Unit,
    onEditTrait: (IdentityNodeEntity) -> Unit,
    onDeleteTrait: (IdentityNodeEntity) -> Unit,
    onClearCategory: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val categoryColor = Color(IdentityCategories.getCategoryColorHex(group.category))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.category,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        text = "${group.nodes.size} traits • Avg ${group.averageConfidence}% confidence",
                        fontSize = 11.sp,
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                }

                IconButton(
                    onClick = onClearCategory,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear category",
                        tint = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.35f) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    group.nodes.forEach { trait ->
                        TraitRowItem(
                            trait = trait,
                            onClick = { onTraitClick(trait) },
                            onEdit = { onEditTrait(trait) },
                            onDelete = { onDeleteTrait(trait) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TraitRowItem(
    trait: IdentityNodeEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val epistemic = trait.getEpistemic()
    val badgeColor = when (epistemic) {
        EpistemicClassification.EXPLICIT_FACT -> Color(0xFF00E5FF)
        EpistemicClassification.STRONG_PATTERN -> Color(0xFFA855F7)
        EpistemicClassification.INFERENCE -> Color(0xFFF59E0B)
        EpistemicClassification.TEMPORARY_STATE -> Color(0xFF94A3B8)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (ThemeManager.isDarkTheme) Color(0xFF1A233A) else Color(0xFFF8FAFC),
        border = BorderStroke(
            0.5.dp,
            if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Epistemic pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.18f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = epistemic.displayName,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!trait.subcategory.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = trait.subcategory!!,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.DarkGray,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${trait.confidence}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(13.dp),
                        tint = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(13.dp),
                        tint = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = trait.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
            )

            if (trait.detail.isNotBlank() && trait.detail != trait.title) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = trait.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TraitDetailCard(
    trait: IdentityNodeEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val epistemic = trait.getEpistemic()
    val badgeColor = when (epistemic) {
        EpistemicClassification.EXPLICIT_FACT -> Color(0xFF00E5FF)
        EpistemicClassification.STRONG_PATTERN -> Color(0xFFA855F7)
        EpistemicClassification.INFERENCE -> Color(0xFFF59E0B)
        EpistemicClassification.TEMPORARY_STATE -> Color(0xFF94A3B8)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeManager.isDarkTheme) Color(0xFF182238) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = epistemic.displayName,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = trait.category,
                    color = ThemeManager.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = trait.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
            )

            if (trait.detail.isNotBlank() && trait.detail != trait.title) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = trait.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Explainability Section: "Tumhe kaise pata?"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (ThemeManager.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "How DepthLens learned this (Explainability):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (trait.supportingEvidence.isNotBlank()) trait.supportingEvidence else "Learned from ongoing conversation context.",
                        fontSize = 11.sp,
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Metadata row: Confidence + Confirmations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Confidence: ${trait.confidence}% (${trait.confidenceLevel})",
                    fontSize = 11.sp,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Confirmed ${trait.confirmationCount}x",
                    fontSize = 11.sp,
                    color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTraitDialog(
    initialTrait: IdentityNodeEntity?,
    onDismiss: () -> Unit,
    onSave: (category: String, subcategory: String?, title: String, detail: String, classification: EpistemicClassification, confidence: Int) -> Unit
) {
    var category by remember { mutableStateOf(initialTrait?.category ?: IdentityCategories.PERSONALITY) }
    var subcategory by remember { mutableStateOf(initialTrait?.subcategory ?: "") }
    var title by remember { mutableStateOf(initialTrait?.title ?: "") }
    var detail by remember { mutableStateOf(initialTrait?.detail ?: "") }
    var classification by remember {
        mutableStateOf(initialTrait?.getEpistemic() ?: EpistemicClassification.EXPLICIT_FACT)
    }
    var confidence by remember { mutableFloatStateOf((initialTrait?.confidence ?: 85).toFloat()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTrait == null) "Add Identity Trait" else "Edit Trait",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Picker
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        IdentityCategories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subcategory (Optional)
                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategory (Optional)") },
                    placeholder = { Text("e.g. Short-term, Tools, Routine") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Trait Title") },
                    placeholder = { Text("e.g. Analytical & Systems-oriented") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Detail
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Context & Detail") },
                    placeholder = { Text("Elaborate on how this shapes reasoning") },
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Epistemic Classification Picker
                Text("Epistemic Status (Facts vs Inferences)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        EpistemicClassification.EXPLICIT_FACT,
                        EpistemicClassification.STRONG_PATTERN,
                        EpistemicClassification.INFERENCE
                    ).forEach { ep ->
                        val isSelected = classification == ep
                        FilterChip(
                            selected = isSelected,
                            onClick = { classification = ep },
                            label = { Text(ep.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                // Confidence Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confidence: ${confidence.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Slider(
                    value = confidence,
                    onValueChange = { confidence = it },
                    valueRange = 20f..100f,
                    steps = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            category,
                            subcategory.takeIf { it.isNotBlank() },
                            title.trim(),
                            detail.trim().ifEmpty { title.trim() },
                            classification,
                            confidence.toInt()
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyMindMapState(onAddTrait: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = ThemeManager.accentColor.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your Mind Map is Forming",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Chat naturally with DepthLens or scan past conversations. Your personality, goals, thinking style, and preferences will evolve here over time.",
            style = MaterialTheme.typography.bodySmall,
            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddTrait,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add First Trait")
        }
    }
}

@Composable
private fun ModeTabChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ThemeManager.accentColor.copy(alpha = 0.18f) else if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(
            1.dp,
            if (selected) ThemeManager.accentColor else if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        ),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ThemeManager.accentColor else if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ThemeManager.accentColor else if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF334155),
                maxLines = 1
            )
            if (badge != null) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) ThemeManager.accentColor else Color(0xFFA855F7).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else Color(0xFFA855F7),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

