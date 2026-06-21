package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.api.ApiService
import com.example.data.model.CategoryRule
import com.example.data.model.LinkRecord
import com.example.ui.theme.CategoryColors
import com.example.ui.theme.JobStatusColors
import com.example.ui.viewmodel.LinkViewModel
import com.example.ui.viewmodel.NavigationTarget
import java.util.Locale

// Neo-Brutalist shadow helper extension
fun Modifier.neoBrutalShadow(isDark: Boolean, cornerRadiusDp: Float = 8f): Modifier = this.drawBehind {
    val shadowColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFF0D0D0D)
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
        size = size,
        cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx(), cornerRadiusDp.dp.toPx())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LinkViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val displayedLinks by viewModel.displayedLinks.collectAsState()
    val allRules by viewModel.allRules.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val unreadOnlyFilter by viewModel.unreadOnlyFilter.collectAsState()

    // Add Sheet / Dialog state
    var isAddSheetOpen by remember { mutableStateOf(false) }

    // Display localized feedback alerts (like brief Toast)
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val neoBrutalBackgroundModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)

    BoxWithConstraints(modifier = neoBrutalBackgroundModifier) {
        val isDesktop = maxWidth >= 768.dp

        if (isDesktop) {
            // --- DESKTOP VIEW LAYOUT ---
            Row(modifier = Modifier.fillMaxSize()) {
                // Fixed Sidebar Drawer (240dp)
                DesktopSidebar(
                    currentTab = currentTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    onAddClick = { isAddSheetOpen = true },
                    onToggleTheme = onToggleTheme,
                    isDarkTheme = isDarkTheme
                )

                // Main Workspace Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    DesktopTopBar(
                        searchQuery = searchState.query,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        isAskMode = searchState.isAskMode,
                        onToggleAskMode = { viewModel.toggleAskMode() },
                        onTriggerSearch = { viewModel.triggerAiSearch() },
                        isSearchingAI = searchState.isSearchingAI,
                        unreadOnly = unreadOnlyFilter,
                        onToggleUnread = { viewModel.toggleUnreadOnly() },
                        isDark = isDarkTheme
                    )

                    // Error warning if AI Key is missing or search failed
                    if (searchState.isAskMode && searchState.aiSearchError != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = searchState.aiSearchError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        if (displayedLinks.isEmpty()) {
                            EmptyHiveState(
                                onAddLinkClick = { isAddSheetOpen = true },
                                isSearching = searchState.query.isNotBlank()
                            )
                        } else {
                            DesktopLinkGrid(
                                links = displayedLinks,
                                isDark = isDarkTheme,
                                onCycleStatus = { viewModel.cycleJobStatus(it) },
                                onSetStatus = { link, st -> viewModel.setJobStatus(link, st) },
                                onTogglePin = { viewModel.togglePin(it) },
                                onCycleRead = { viewModel.cycleReadState(it) },
                                onDelete = { viewModel.deleteLink(it.id) },
                                aiReasons = searchState.aiMatches,
                                onInteract = { viewModel.markAsRead(it) }
                            )
                        }
                    }
                }
            }
        } else {
            // --- MOBILE VIEW LAYOUT ---
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                floatingActionButton = {
                    val fabBgColor = if (currentTab != NavigationTarget.ALL && currentTab != NavigationTarget.PINNED && currentTab != NavigationTarget.UNCATEGORIZED) {
                        CategoryColors.getAccent(currentTab.name.lowercase(), isDarkTheme)
                    } else {
                        if (isDarkTheme) Color(0xFFF5F5F5) else Color(0xFF0D0D0D)
                    }
                    val fabIconColor = if (isDarkTheme) {
                        Color(0xFF0D0D0D)
                    } else {
                        if (currentTab != NavigationTarget.ALL && currentTab != NavigationTarget.PINNED && currentTab != NavigationTarget.UNCATEGORIZED) Color(0xFF0D0D0D) else Color.White
                    }
                    val shadowColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFF0D0D0D)
                    val borderColor = if (isDarkTheme) Color.White else Color.Black

                    Box(
                        modifier = Modifier
                            .testTag("add_link_fab")
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, end = 8.dp)
                            .size(56.dp)
                            .drawBehind {
                                drawRoundRect(
                                    color = shadowColor,
                                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                    size = size,
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                                )
                            }
                            .background(
                                color = fabBgColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isAddSheetOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Link",
                            tint = fabIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(padding)
                ) {
                    // Mobile Header Custom Navigation
                    MobileHeader(
                        currentTab = currentTab,
                        onToggleTheme = onToggleTheme,
                        isDark = isDarkTheme,
                        onSelectTab = { viewModel.selectTab(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal scrolling rail of categories
                    CategoryScrollRail(
                        activeTab = currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        customCategories = viewModel.customCategories.collectAsState().value,
                        selectedCustomCategory = viewModel.selectedCustomCategory.collectAsState().value,
                        onSelectCustomCategory = { viewModel.selectCustomCategory(it) },
                        isDark = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search details if on standard search target
                    MobileSearchBarSection(
                        searchQuery = searchState.query,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        isAskMode = searchState.isAskMode,
                        onToggleAskMode = { viewModel.toggleAskMode() },
                        onTriggerSearch = { viewModel.triggerAiSearch() },
                        isSearchingAI = searchState.isSearchingAI,
                        isDark = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Warning / Error banners
                    if (searchState.isAskMode && searchState.query.isNotBlank() && searchState.aiSearchError != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = searchState.aiSearchError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Content View
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (displayedLinks.isEmpty()) {
                            EmptyHiveState(
                                onAddLinkClick = { isAddSheetOpen = true },
                                isSearching = searchState.query.isNotBlank()
                            )
                        } else {
                            MobileLinkList(
                                links = displayedLinks,
                                isDark = isDarkTheme,
                                onCycleStatus = { viewModel.cycleJobStatus(it) },
                                onTogglePin = { viewModel.togglePin(it) },
                                onCycleRead = { viewModel.cycleReadState(it) },
                                onDelete = { viewModel.deleteLink(it.id) },
                                aiReasons = searchState.aiMatches,
                                onInteract = { viewModel.markAsRead(it) }
                            )
                        }
                    }
                }
            }
        }

        // --- ADD LINK SHEETS / DIALOGS ---
        if (isAddSheetOpen) {
            if (isDesktop) {
                // Centered modal dialog on Desktop
                AddLinkDialog(
                    urlInput = viewModel.inputUrl.collectAsState().value,
                    onUrlChange = { viewModel.onUrlInputChanged(it) },
                    derivedName = viewModel.derivedName.collectAsState().value,
                    onNameChange = { viewModel.onDerivedNameInputChanged(it) },
                    derivedCategory = viewModel.derivedCategory.collectAsState().value,
                    onCategorySelect = { viewModel.onCategoryChipSelected(it) },
                    customCategories = viewModel.customCategories.collectAsState().value,
                    onAddCustomCategory = { name, emoji, col -> viewModel.addCustomCategory(name, emoji, col) },
                    onDismiss = {
                        isAddSheetOpen = false
                        viewModel.clearInputs()
                    },
                    onSave = {
                        viewModel.saveLink()
                        isAddSheetOpen = false
                    },
                    isDark = isDarkTheme
                )
            } else {
                // Bottom sheet on Mobile
                AddLinkBottomSheet(
                    urlInput = viewModel.inputUrl.collectAsState().value,
                    onUrlChange = { viewModel.onUrlInputChanged(it) },
                    derivedName = viewModel.derivedName.collectAsState().value,
                    onNameChange = { viewModel.onDerivedNameInputChanged(it) },
                    derivedCategory = viewModel.derivedCategory.collectAsState().value,
                    onCategorySelect = { viewModel.onCategoryChipSelected(it) },
                    customCategories = viewModel.customCategories.collectAsState().value,
                    onAddCustomCategory = { name, emoji, col -> viewModel.addCustomCategory(name, emoji, col) },
                    onDismiss = {
                        isAddSheetOpen = false
                        viewModel.clearInputs()
                    },
                    onSave = {
                        viewModel.saveLink()
                        isAddSheetOpen = false
                    },
                    isDark = isDarkTheme
                )
            }
        }
    }
}

// Helper relative time formatter
fun getRelativeTimeString(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "$minutes m ago"
    val hours = minutes / 60
    if (hours < 24) return "$hours h ago"
    val days = hours / 24
    if (days < 30) return "$days d ago"
    return "${days / 30} mo ago"
}

// --- DESKTOP COMPONENTS ---

@Composable
fun DesktopSidebar(
    currentTab: NavigationTarget,
    onSelectTab: (NavigationTarget) -> Unit,
    onAddClick: () -> Unit,
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.65f else 0.85f),
        tonalElevation = 0.dp,
        border = BorderStroke(width = 1.dp, color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Brand Header with hexagonal style hive circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFBBF24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "LinkHive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA Plus save
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("desktop_add_link_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Link", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Target list
                Text(
                    text = "CATEGORIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                val targets = listOf(
                    NavigationTarget.ALL to "🌐 All Workspace",
                    NavigationTarget.JOBS to "💼 Jobs Pipeline",
                    NavigationTarget.SOCIALS to "💬 Social Hub",
                    NavigationTarget.VIDEOS to "🎥 Videos & Clips",
                    NavigationTarget.ARTICLES to "📝 Readings",
                    NavigationTarget.UNCATEGORIZED to "📂 Uncategorized",
                    NavigationTarget.PINNED to "🌟 Pinned Links"
                )

                targets.forEach { (target, label) ->
                    val isActive = currentTab == target
                    val tintColor = if (isActive) {
                        if (target == NavigationTarget.ALL || target == NavigationTarget.PINNED) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            CategoryColors.getAccent(target.name.lowercase(), isDarkTheme)
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) tintColor.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { onSelectTab(target) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Segmented Theme Toggle Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { if (isDarkTheme) onToggleTheme() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☀️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Light", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (!isDarkTheme) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { if (!isDarkTheme) onToggleTheme() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dark", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// Minimal border outline wrapper


@Composable
fun DesktopTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isAskMode: Boolean,
    onToggleAskMode: () -> Unit,
    onTriggerSearch: () -> Unit,
    isSearchingAI: Boolean,
    unreadOnly: Boolean,
    onToggleUnread: () -> Unit,
    isDark: Boolean
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search text field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .height(48.dp)
                    .testTag("desktop_search_input"),
                placeholder = { Text("Search link, web, tags, category...", fontSize = 14.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (isAskMode) {
                        onTriggerSearch()
                    }
                    focusManager.clearFocus()
                })
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Ask Mode toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAskMode) Color(0xFFFBBF24).copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isAskMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleAskMode() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ask AI (Gemini)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAskMode) Color(0xFFD97706) else MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isSearchingAI) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Search AI",
                        tint = if (isAskMode) Color(0xFFD97706) else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                if (isAskMode) onTriggerSearch()
                            }
                    )
                }
            }
        }

        // Triage Inbox Filter (Anti-Graveyard toggle)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (unreadOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                .border(
                    1.dp,
                    if (unreadOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onToggleUnread() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (unreadOnly) Color(0xFFD97706) else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Unread Queue Only",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}



@Composable
fun DesktopLinkGrid(
    links: List<LinkRecord>,
    isDark: Boolean,
    onCycleStatus: (LinkRecord) -> Unit,
    onSetStatus: (LinkRecord, String) -> Unit,
    onTogglePin: (LinkRecord) -> Unit,
    onCycleRead: (LinkRecord) -> Unit,
    onDelete: (LinkRecord) -> Unit,
    aiReasons: Map<String, String>,
    onInteract: (LinkRecord) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(links, key = { it.id }) { link ->
            LinkCardItem(
                link = link,
                isDark = isDark,
                onCycleStatus = onCycleStatus,
                onSetStatus = { onSetStatus(link, it) },
                onTogglePin = onTogglePin,
                onCycleRead = onCycleRead,
                onDelete = { onDelete(link) },
                aiReason = aiReasons[link.id],
                isMobile = false,
                onInteract = onInteract
            )
        }
    }
}

// --- MOBILE COMPONENTS ---

@Composable
fun MobileHeader(
    currentTab: NavigationTarget,
    onToggleTheme: () -> Unit,
    isDark: Boolean,
    onSelectTab: (NavigationTarget) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF131314) else Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFBBF24))
                        .border(1.5.dp, if (isDark) Color.White else Color.Black, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🐝",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LinkHive",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin toggle button in top bar (Requirement 3)
                IconButton(onClick = {
                    if (currentTab == NavigationTarget.PINNED) {
                        onSelectTab(NavigationTarget.ALL)
                    } else {
                        onSelectTab(NavigationTarget.PINNED)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pinned links filter",
                        tint = if (currentTab == NavigationTarget.PINNED) Color(0xFFFBBF24) else (if (isDark) Color.White else Color.Black),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onToggleTheme) {
                    Text(
                        text = if (isDark) "☀️" else "🌙",
                        fontSize = 20.sp
                    )
                }
            }
        }
        
        // Crisp Neo-Brutalist bottom border line separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isDark) Color(0xFF2A2A2D) else Color.Black)
        )
    }
}

@Composable
fun CategoryScrollRail(
    activeTab: NavigationTarget,
    onSelectTab: (NavigationTarget) -> Unit,
    customCategories: List<String>,
    selectedCustomCategory: String?,
    onSelectCustomCategory: (String?) -> Unit,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()
    val categories = listOf(
        NavigationTarget.ALL to "🌐 All",
        NavigationTarget.JOBS to "💼 Jobs",
        NavigationTarget.SOCIALS to "💬 Socials",
        NavigationTarget.VIDEOS to "🎥 Videos",
        NavigationTarget.ARTICLES to "📝 Readings",
        NavigationTarget.UNCATEGORIZED to "📂 Uncategorized"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (target, label) ->
            val isActive = activeTab == target && selectedCustomCategory == null
            val accentColor = if (target == NavigationTarget.ALL) {
                if (isDark) Color.White else Color.Black
            } else {
                CategoryColors.getAccent(target.name.lowercase(), isDark)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isActive) {
                            if (isDark && target == NavigationTarget.ALL) Color.White else accentColor
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        }
                    )
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                    .clickable { 
                        onSelectCustomCategory(null)
                        onSelectTab(target) 
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (isActive) {
                        if (isDark && target == NavigationTarget.ALL) Color.Black else Color.White
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Now render custom categories dynamically
        customCategories.forEach { customCat ->
            val isActive = selectedCustomCategory == customCat
            val accentColor = CategoryColors.getAccent(customCat, isDark)
            val emoji = CategoryColors.getEmoji(customCat)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isActive) accentColor else {
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        }
                    )
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                    .clickable { 
                        onSelectCustomCategory(customCat)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$emoji ${customCat.replaceFirstChar { it.uppercase() }}",
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MobileSearchBarSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isAskMode: Boolean,
    onToggleAskMode: () -> Unit,
    onTriggerSearch: () -> Unit,
    isSearchingAI: Boolean,
    isDark: Boolean
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .neoBrutalShadow(isDark, cornerRadiusDp = 8f)
                    .testTag("mobile_search_input"),
                placeholder = { Text("Search tags, url, notes...", fontSize = 12.sp, color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isDark) Color.White else Color.Black) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF1E1E20) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF1E1E20) else Color.White,
                    focusedTextColor = if (isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (isDark) Color.White else Color.Black,
                    focusedBorderColor = if (isDark) Color.White else Color.Black,
                    unfocusedBorderColor = if (isDark) Color.White else Color.Black,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (isAskMode) {
                        onTriggerSearch()
                    }
                    focusManager.clearFocus()
                })
            )

            // AI Toggle (aligned perfectly with search component height)
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .neoBrutalShadow(isDark, cornerRadiusDp = 8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAskMode) Color(0xFFFBBF24) else if (isDark) Color(0xFF1E1E20) else Color.White)
                    .border(
                        2.dp,
                        if (isDark) Color.White else Color.Black,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleAskMode() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Ask ✨",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAskMode) Color.Black else if (isDark) Color.White else Color.Black
                    )
                    if (isSearchingAI) {
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = if (isAskMode) Color.Black else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MobileLinkList(
    links: List<LinkRecord>,
    isDark: Boolean,
    onCycleStatus: (LinkRecord) -> Unit,
    onTogglePin: (LinkRecord) -> Unit,
    onCycleRead: (LinkRecord) -> Unit,
    onDelete: (LinkRecord) -> Unit,
    aiReasons: Map<String, String>,
    onInteract: (LinkRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(links, key = { it.id }) { link ->
            LinkCardItem(
                link = link,
                isDark = isDark,
                onCycleStatus = onCycleStatus,
                onSetStatus = {},
                onTogglePin = onTogglePin,
                onCycleRead = onCycleRead,
                onDelete = { onDelete(link) },
                aiReason = aiReasons[link.id],
                isMobile = true,
                onInteract = onInteract
            )
        }
    }
}

// --- ELEVATED CUSTOM ITEM CARD COMPONENT ---

@Composable
fun LinkCardItem(
    link: LinkRecord,
    isDark: Boolean,
    onCycleStatus: (LinkRecord) -> Unit,
    onSetStatus: (String) -> Unit,
    onTogglePin: (LinkRecord) -> Unit,
    onCycleRead: (LinkRecord) -> Unit,
    onDelete: () -> Unit,
    aiReason: String?,
    isMobile: Boolean,
    onInteract: (LinkRecord) -> Unit
) {
    val context = LocalContext.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    val categoryColor = CategoryColors.getAccent(link.category, isDark)
    val backgroundTint = CategoryColors.getBackgroundTint(link.category, isDark)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neoBrutalShadow(isDark, cornerRadiusDp = 8f)
            .animateContentSize()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                    context.startActivity(intent)
                    onInteract(link)
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid URL format", Toast.LENGTH_SHORT).show()
                }
            }
            .testTag("link_record_card_${link.id}"),
        colors = CardDefaults.cardColors(
            containerColor = CategoryColors.getCardBackground(link.category, isDark),
            contentColor = if (isDark) Color(0xFFF1F1F1) else Color(0xFF0D0D0D)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isDark) Color(0xFFF5F5F5) else Color(0xFF0D0D0D)
        )
    ) {
        // Aesthetic 4px Left Category bar on details
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .background(categoryColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Header details: Source domain/handle + readState badge + Pin + Action overflow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Category Chip pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(categoryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = link.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Custom Mono formatted Handle source string
                        Text(
                            text = link.source,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (link.readState == "unread") {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Read status indicator (Unread indicator)
                            TriageStateBadge(state = link.readState, onClick = { onCycleRead(link) })
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin button
                        IconButton(
                            onClick = { onTogglePin(link) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Pin Link",
                                tint = if (link.pinned) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Overflow menu key
                        Box {
                            IconButton(
                                onClick = { isMenuExpanded = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open Link 🌐") },
                                    onClick = {
                                        isMenuExpanded = false
                                        try {
                                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                            context.startActivity(i)
                                            onInteract(link)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Invalid URL format", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mark Unread") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCycleRead(link) // Or custom cycle setting
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete Link 🗑️",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Optional URL thumbnail (YouTube, oEmbed)
                if (!link.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = link.thumbnail,
                        contentDescription = "Thumbnail for ${link.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = 6.dp)
                    )
                }

                // Main Title display (clean display styles)
                Text(
                    text = link.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // AI Generated summary subtitle
                if (!link.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = link.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // AI Suggested tags (list of pills)
                val tagList = if (link.tags.isNotBlank()) link.tags.split(",") else emptyList()
                if (tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // AI reasoning matches (Ask Mode display)
                if (aiReason != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFBBF24).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 $aiReason",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(6.dp))
                // Timestamp info
                Text(
                    text = "Added " + getRelativeTimeString(link.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Badge for Inbox Triaging (unread state)
@Composable
fun TriageStateBadge(state: String, onClick: () -> Unit) {
    val bgColor = when (state) {
        "reading" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
        "done" -> Color(0xFF10B981).copy(alpha = 0.15f)
        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
    }
    val contentColor = when (state) {
        "reading" -> Color(0xFF2563EB)
        "done" -> Color(0xFF059669)
        else -> Color(0xFFDC2626)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = state.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// Status badge for jobs
@Composable
fun JobStatusPill(status: String, onClick: () -> Unit) {
    val (label, color) = when (status) {
        "applied" -> "Joined/Applied ✓" to JobStatusColors.Applied
        "rejected" -> "Rejected ✕" to JobStatusColors.Rejected
        else -> "Not Applied ⏳" to JobStatusColors.NotApplied
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DesktopJobStatusPicker(
    currentStatus: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val activeLabel = when (currentStatus) {
        "applied" -> "Applied ✓"
        "rejected" -> "Rejected"
        else -> "Not Applied"
    }
    val color = when (currentStatus) {
        "applied" -> JobStatusColors.Applied
        "rejected" -> JobStatusColors.Rejected
        else -> JobStatusColors.NotApplied
    }

    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .border(1.dp, color, RoundedCornerShape(6.dp)),
            color = color.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeLabel,
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Not Applied") },
                onClick = {
                    onSelect("not_applied")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Applied") },
                onClick = {
                    onSelect("applied")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Rejected") },
                onClick = {
                    onSelect("rejected")
                    expanded = false
                }
            )
        }
    }
}

// --- FIRST RUN EMPTY STATE ---

@Composable
fun EmptyHiveState(onAddLinkClick: () -> Unit, isSearching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hexagon honey hive design in Canvas
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    // Draw stylish yellow geometric vector hexagon
                    val hexSize = size.width / 2.5f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    for (i in 0..5) {
                        val angle = Math.toRadians((i * 60 - 30).toDouble())
                        points.add(
                            androidx.compose.ui.geometry.Offset(
                                (cx + hexSize * Math.cos(angle)).toFloat(),
                                (cy + hexSize * Math.sin(angle)).toFloat()
                            )
                        )
                    }
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1..5) {
                            lineTo(points[i].x, points[i].y)
                        }
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFBBF24), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                    // Draw outer larger faint hive
                    val largeHexSize = size.width / 1.8f
                    val lpoints = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    for (i in 0..5) {
                        val angle = Math.toRadians((i * 60 + 30).toDouble())
                        lpoints.add(
                            androidx.compose.ui.geometry.Offset(
                                (cx + largeHexSize * Math.cos(angle)).toFloat(),
                                (cy + largeHexSize * Math.sin(angle)).toFloat()
                            )
                        )
                    }
                    val lpath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(lpoints[0].x, lpoints[0].y)
                        for (i in 1..5) {
                            lineTo(lpoints[i].x, lpoints[i].y)
                        }
                        close()
                    }
                    drawPath(path = lpath, color = Color(0xFFFBBF24).copy(alpha = 0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSearching) "No matches found" else "Your hive is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearching) "Try searching for another keyword or check spelling." else "Store links, readings, bookmarks in clean automated tabs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )

        if (!isSearching) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddLinkClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Save First Link", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- RULE MANAGEMENT SCREEN VIEW ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesManagementView(
    rules: List<CategoryRule>,
    onAddRule: (String, String) -> Unit,
    onDeleteRule: (String) -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("jobs") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Domain Rule Table",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure instant automatic matching. Links will be categorized as they are being saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Add rule panel card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create Heuristic Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. news.ycombinator.com") },
                        label = { Text("Domain Prefix") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Assign Category:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    val categories = listOf("jobs", "socials", "videos", "articles")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedCategory == cat) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.uppercase(),
                                    color = if (selectedCategory == cat) Color.White else MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (domainInput.isNotBlank()) {
                                onAddRule(domainInput, selectedCategory)
                                domainInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Rule Matrix", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Seeded / Active Rules Mapping", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(rules) { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = rule.domain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = rule.category.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelSmall, color = CategoryColors.getAccent(rule.category, isSystemInDarkTheme()))
                }
                IconButton(onClick = { onDeleteRule(rule.domain) }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// --- ADD LINK SHEETS & MODALS (Optimistic instant save, background enrich) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLinkFormContent(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    derivedName: String,
    onNameChange: (String) -> Unit,
    derivedCategory: String,
    onCategorySelect: (String) -> Unit,
    customCategories: List<String>,
    onAddCustomCategory: (String, String, String) -> Unit,
    onSave: () -> Unit,
    isDark: Boolean
) {
    val clipboard = LocalClipboardManager.current
    var selectedEmoji by remember { mutableStateOf("📂") }
    var selectedColorHex by remember { mutableStateOf("#9CA3AF") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Save New Link URL",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            // Clipboard Paste helper triggers instant parsing
            Button(
                onClick = {
                    clipboard.getText()?.text?.let { onUrlChange(it) }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Paste Clipboard 📋", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_link_url_input"),
            label = { Text("URL Address") },
            placeholder = { Text("https://example.com/...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        OutlinedTextField(
            value = derivedName,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_link_title_input"),
            label = { Text("Title (Auto-detected / Editable)") },
            placeholder = { Text("Loading page details...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Column {
            Text(
                text = "Category (Auto-assigned):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val categories = listOf("jobs", "socials", "videos", "articles") + customCategories
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isActive = derivedCategory == cat
                    val col = CategoryColors.getAccent(cat, isDark)
                    val emoji = CategoryColors.getEmoji(cat)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) col else col.copy(alpha = 0.08f))
                            .border(1.dp, col, RoundedCornerShape(20.dp))
                            .clickable { onCategorySelect(cat) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$emoji ${cat.uppercase()}",
                            color = if (isActive) Color.White else col,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "New Bucket / Category Name:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            var newCustomCategoryInput by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCustomCategoryInput,
                    onValueChange = { newCustomCategoryInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("e.g. recipes, design, tech") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Button(
                    onClick = {
                        val trimmed = newCustomCategoryInput.trim().lowercase()
                        if (trimmed.isNotBlank()) {
                            onAddCustomCategory(trimmed, selectedEmoji, selectedColorHex)
                            onCategorySelect(trimmed)
                            newCustomCategoryInput = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Emoji Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emoji: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(70.dp)
                )
                val emojis = listOf("📂", "🚀", "💡", "🎨", "🔬", "🍿", "🎧", "🎮", "🍔", "📈", "✈️", "❤️")
                val scrollStateEmoji = rememberScrollState()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollStateEmoji),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { selectedEmoji = emoji }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(70.dp)
                )
                val colorsMap = listOf(
                    "Blue" to "#60A5FA",
                    "Pink" to "#F472B6",
                    "Red" to "#F87171",
                    "Amber" to "#FBBF24",
                    "Green" to "#34D399",
                    "Purple" to "#C084FC",
                    "Orange" to "#FB923C"
                )
                val scrollStateColor = rememberScrollState()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollStateColor),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorsMap.forEach { (colorName, colorHex) ->
                        val isSelected = selectedColorHex == colorHex
                        val swatchColor = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    2.dp,
                                    if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorHex = colorHex }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_link_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save Link Instantly", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkBottomSheet(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    derivedName: String,
    onNameChange: (String) -> Unit,
    derivedCategory: String,
    onCategorySelect: (String) -> Unit,
    customCategories: List<String>,
    onAddCustomCategory: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isDark: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {}
    ) {
        AddLinkFormContent(
            urlInput = urlInput,
            onUrlChange = onUrlChange,
            derivedName = derivedName,
            onNameChange = onNameChange,
            derivedCategory = derivedCategory,
            onCategorySelect = onCategorySelect,
            customCategories = customCategories,
            onAddCustomCategory = onAddCustomCategory,
            onSave = onSave,
            isDark = isDark
        )
    }
}

@Composable
fun AddLinkDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    derivedName: String,
    onNameChange: (String) -> Unit,
    derivedCategory: String,
    onCategorySelect: (String) -> Unit,
    customCategories: List<String>,
    onAddCustomCategory: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isDark: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp
        ) {
            AddLinkFormContent(
                urlInput = urlInput,
                onUrlChange = onUrlChange,
                derivedName = derivedName,
                onNameChange = onNameChange,
                derivedCategory = derivedCategory,
                onCategorySelect = onCategorySelect,
                customCategories = customCategories,
                onAddCustomCategory = onAddCustomCategory,
                onSave = onSave,
                isDark = isDark
            )
        }
    }
}
