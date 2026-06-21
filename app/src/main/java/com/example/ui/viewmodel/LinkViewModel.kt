package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Categorizer
import com.example.data.api.ApiService
import com.example.data.model.CategoryRule
import com.example.data.model.LinkRecord
import com.example.data.repository.LinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val error: String) : UiState
}

enum class NavigationTarget {
    ALL,
    JOBS,
    SOCIALS,
    VIDEOS,
    ARTICLES,
    UNCATEGORIZED,
    PINNED
}

data class SearchState(
    val query: String = "",
    val isAskMode: Boolean = false,
    val aiMatches: Map<String, String> = emptyMap(), // linkId -> AI reasoning message
    val isSearchingAI: Boolean = false,
    val aiSearchError: String? = null
)

data class GoogleUserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String
)

class LinkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LinkRepository(application)
    private val sharedPref = application.getSharedPreferences("linkhive_preferences", Context.MODE_PRIVATE)

    // Google Simulation Authentication State
    private val _currentUser = MutableStateFlow<GoogleUserProfile?>(null)
    val currentUser: StateFlow<GoogleUserProfile?> = _currentUser.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    // Observables
    val allLinks: StateFlow<List<LinkRecord>> = repository.allLinks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRules: StateFlow<List<CategoryRule>> = repository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic Custom Categories
    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _selectedCustomCategory = MutableStateFlow<String?>(null)
    val selectedCustomCategory: StateFlow<String?> = _selectedCustomCategory.asStateFlow()

    init {
        // Load custom categories from shared pref
        val storedSet = sharedPref.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val sortedList = storedSet.sorted().toList()
        _customCategories.value = sortedList
        
        // Register stored custom category characteristics with CategoryColors helper
        sortedList.forEach { trimmed ->
            val emoji = sharedPref.getString("custom_cat_emoji_$trimmed", "📂") ?: "📂"
            val colorHex = sharedPref.getString("custom_cat_color_$trimmed", "#9CA3AF") ?: "#9CA3AF"
            com.example.ui.theme.CategoryColors.registerCustomCategory(trimmed, colorHex, emoji)
        }

        // Initialize Firebase manual client or default client
        try {
            val existingApp = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                null
            }

            if (existingApp == null) {
                val apiKey = try { com.example.BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
                val appId = try { com.example.BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Exception) { "" }
                val projectId = try { com.example.BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }

                if (apiKey.isNotBlank() && appId.isNotBlank() && projectId.isNotBlank()) {
                    Log.d("LinkViewModel", "Initializing Firebase manually from BuildConfig keys.")
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(application, options)
                } else {
                    Log.w("LinkViewModel", "No Firebase automatic config found, nor manual properties in BuildConfig.")
                }
            }

            firebaseAuth = FirebaseAuth.getInstance()
            Log.d("LinkViewModel", "FirebaseAuth initialised successfully.")
        } catch (e: Exception) {
            Log.e("LinkViewModel", "Firebase authentication startup failure: ${e.message}")
        }

        // Load saved Google User Profile - first check active Firebase session, then local simulated session fallback
        val activeFirebaseUser = firebaseAuth?.currentUser
        if (activeFirebaseUser != null) {
            Log.d("LinkViewModel", "Discovered existing active Firebase user session.")
            _currentUser.value = GoogleUserProfile(
                email = activeFirebaseUser.email ?: "unknown@gmail.com",
                displayName = activeFirebaseUser.displayName ?: "Firebase User",
                photoUrl = activeFirebaseUser.photoUrl?.toString(),
                idToken = sharedPref.getString("google_user_id", "firebase_active") ?: "firebase_active"
            )
        } else {
            val savedEmail = sharedPref.getString("google_user_email", null)
            val savedName = sharedPref.getString("google_user_name", null)
            val savedPhoto = sharedPref.getString("google_user_photo", null)
            val savedId = sharedPref.getString("google_user_id", null)
            if (savedEmail != null && savedName != null && savedId != null) {
                _currentUser.value = GoogleUserProfile(
                    email = savedEmail,
                    displayName = savedName,
                    photoUrl = savedPhoto,
                    idToken = savedId
                )
            }
        }
    }

    fun signInWithGoogleSimulated(email: String, name: String, photoUrl: String?) {
        val user = GoogleUserProfile(
            email = email,
            displayName = name,
            photoUrl = photoUrl,
            idToken = "sim_token_${System.currentTimeMillis()}"
        )
        _currentUser.value = user
        sharedPref.edit()
            .putString("google_user_email", email)
            .putString("google_user_name", name)
            .putString("google_user_photo", photoUrl)
            .putString("google_user_id", user.idToken)
            .apply()
        showToast("Welcome back, $name! 🐝")
    }

    fun isFirebaseAvailable(): Boolean {
        return firebaseAuth != null
    }

    fun getGoogleWebClientId(): String? {
        val id = try { com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID } catch (e: Exception) { "" }
        return id.ifBlank { null }
    }

    fun signInWithFirebaseGoogleToken(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(false, "Firebase auth is not initialized or configured.")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val email = firebaseUser.email ?: "unknown@gmail.com"
                        val displayName = firebaseUser.displayName ?: "Firebase User"
                        val photoUrl = firebaseUser.photoUrl?.toString()

                        val user = GoogleUserProfile(
                            email = email,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            idToken = idToken
                        )
                        _currentUser.value = user
                        sharedPref.edit()
                            .putString("google_user_email", email)
                            .putString("google_user_name", displayName)
                            .putString("google_user_photo", photoUrl)
                            .putString("google_user_id", idToken)
                            .apply()
                        onComplete(true, null)
                    } else {
                        onComplete(false, "Succeeded firebase authentication but user profile is null.")
                    }
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Unknown authentication exception.")
                }
            }
    }

    fun signOutGoogle() {
        val currentName = _currentUser.value?.displayName ?: "User"
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("LinkViewModel", "Error signing out of Firebase Auth: ${e.message}")
        }
        _currentUser.value = null
        sharedPref.edit()
            .remove("google_user_email")
            .remove("google_user_name")
            .remove("google_user_photo")
            .remove("google_user_id")
            .apply()
        showToast("Signed out. Goodbye, $currentName!")
    }

    fun addCustomCategory(newCategory: String, emoji: String, colorHex: String) {
        val trimmed = newCategory.trim().lowercase()
        if (trimmed.isNotBlank() && trimmed !in listOf("jobs", "socials", "videos", "articles", "uncategorized", "all", "pinned")) {
            val currentSet = sharedPref.getStringSet("custom_categories", emptySet()) ?: emptySet()
            val newSet = currentSet.toMutableSet()
            newSet.add(trimmed)
            
            sharedPref.edit()
                .putStringSet("custom_categories", newSet)
                .putString("custom_cat_emoji_$trimmed", emoji)
                .putString("custom_cat_color_$trimmed", colorHex)
                .apply()
                
            com.example.ui.theme.CategoryColors.registerCustomCategory(trimmed, colorHex, emoji)
            
            _customCategories.value = newSet.sorted().toList()
        }
    }

    fun selectCustomCategory(category: String?) {
        _selectedCustomCategory.value = category
        if (category != null) {
            _currentTab.value = NavigationTarget.ALL
        }
    }

    // Screen navigation
    private val _currentTab = MutableStateFlow(NavigationTarget.ALL)
    val currentTab: StateFlow<NavigationTarget> = _currentTab.asStateFlow()

    // Add sheet inputs
    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _derivedName = MutableStateFlow("")
    val derivedName: StateFlow<String> = _derivedName.asStateFlow()

    private val _derivedCategory = MutableStateFlow("uncategorized")
    val derivedCategory: StateFlow<String> = _derivedCategory.asStateFlow()

    // Search state
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    // UI Feedback Message (e.g. single-turn toast)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Filter "Unread Only" toggle (Triage / Anti-Graveyard feature)
    private val _unreadOnlyFilter = MutableStateFlow(false)
    val unreadOnlyFilter: StateFlow<Boolean> = _unreadOnlyFilter.asStateFlow()

    // Filtered / Searched links displayed in UI
    val displayedLinks: StateFlow<List<LinkRecord>> = combine(
        allLinks,
        _currentTab,
        _searchState,
        _unreadOnlyFilter,
        _selectedCustomCategory
    ) { links, tab, search, unreadOnly, customCat ->
        var result = links

        // Apply Tab Filter (priority given to selected custom category)
        if (customCat != null) {
            result = result.filter { it.category == customCat }
        } else {
            result = when (tab) {
                NavigationTarget.ALL -> result
                NavigationTarget.JOBS -> result.filter { it.category == "jobs" }
                NavigationTarget.SOCIALS -> result.filter { it.category == "socials" }
                NavigationTarget.VIDEOS -> result.filter { it.category == "videos" }
                NavigationTarget.ARTICLES -> result.filter { it.category == "articles" }
                NavigationTarget.UNCATEGORIZED -> result.filter { it.category == "uncategorized" || it.category.isBlank() }
                NavigationTarget.PINNED -> result.filter { it.pinned }
            }
        }

        // Apply Unread triage filter
        if (unreadOnly) {
            result = result.filter { it.readState == "unread" }
        }

        // Apply Substring or AI Search
        if (search.query.isNotBlank()) {
            if (search.isAskMode) {
                // If AI mode search is enabled, filter using the matched IDs
                result = if (search.aiMatches.isNotEmpty()) {
                    result.filter { search.aiMatches.containsKey(it.id) }
                } else {
                    // if searching but no matches yet, or search failed, return empty to prevent wrong fallback
                    emptyList()
                }
            } else {
                val q = search.query.lowercase().trim()
                result = result.filter {
                    it.searchBlob.contains(q)
                }
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(target: NavigationTarget) {
        _currentTab.value = target
        _selectedCustomCategory.value = null
    }

    fun toggleUnreadOnly() {
        _unreadOnlyFilter.value = !_unreadOnlyFilter.value
    }

    // Input URL change triggers instant heuristic detection
    fun onUrlInputChanged(url: String) {
        _inputUrl.value = url
        viewModelScope.launch {
            if (url.isNotBlank()) {
                val rules = allRules.value
                val info = Categorizer.categorize(url, rules)
                _derivedName.value = info.name
                _derivedCategory.value = info.category
            } else {
                _derivedName.value = ""
                _derivedCategory.value = "uncategorized"
            }
        }
    }

    fun onDerivedNameInputChanged(name: String) {
        _derivedName.value = name
    }

    fun onCategoryChipSelected(cat: String) {
        if (_derivedCategory.value == cat) {
            _derivedCategory.value = "uncategorized"
        } else {
            _derivedCategory.value = cat
        }
    }

    fun clearInputs() {
        _inputUrl.value = ""
        _derivedName.value = ""
        _derivedCategory.value = "uncategorized"
    }

    // Main instant save with background enrich flow
    fun saveLink() {
        val url = _inputUrl.value
        val manualName = _derivedName.value
        val manualCategory = _derivedCategory.value

        if (url.isBlank()) return

        viewModelScope.launch {
            try {
                // 1. Save optimistically
                val savedRecord = repository.saveLinkOptimistic(url)
                
                // If user customized name or category before saving, update that first
                val refinedRecord = savedRecord.copy(
                    name = if (manualName.isNotBlank()) manualName else savedRecord.name,
                    category = manualCategory
                )
                repository.updateLink(refinedRecord)
                
                showToast("Saving \"${refinedRecord.name}\"...")
                clearInputs()

                // 2. Perform secondary enrichment in background
                launch {
                    repository.enrichLink(refinedRecord)
                    showToast("Enriched: \"${refinedRecord.name}\" with tags & summary")
                }
            } catch (e: Exception) {
                showToast("Failed to save link: ${e.message}")
            }
        }
    }

    // Inline status change cycling
    fun cycleJobStatus(link: LinkRecord) {
        viewModelScope.launch {
            val nextStatus = when (link.status) {
                "not_applied" -> "applied"
                "applied" -> "rejected"
                else -> "not_applied"
            }
            repository.updateLink(link.copy(status = nextStatus))
        }
    }

    // Set exact job status
    fun setJobStatus(link: LinkRecord, status: String) {
        viewModelScope.launch {
            repository.updateLink(link.copy(status = status))
        }
    }

    // Toggle Pin status
    fun togglePin(link: LinkRecord) {
        viewModelScope.launch {
            repository.updateLink(link.copy(pinned = !link.pinned))
        }
    }

    // Cycle Read state
    fun cycleReadState(link: LinkRecord) {
        viewModelScope.launch {
            val nextReadState = when (link.readState) {
                "unread" -> "reading"
                "reading" -> "done"
                else -> "unread"
            }
            repository.updateLink(link.copy(readState = nextReadState))
        }
    }

    fun markAsRead(link: LinkRecord) {
        if (link.readState == "unread") {
            viewModelScope.launch {
                repository.updateLink(link.copy(readState = "done"))
            }
        }
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            repository.deleteLink(id)
            showToast("Link deleted")
        }
    }

    fun updateLinkFields(linkId: String, name: String, url: String, summary: String, category: String) {
        viewModelScope.launch {
            val existing = allLinks.value.find { it.id == linkId } ?: return@launch
            val updated = existing.copy(
                name = name.trim(),
                url = url.trim(),
                summary = summary.trim(),
                category = category.trim().lowercase()
            )
            repository.updateLink(updated)
            showToast("Link updated successfully")
        }
    }

    // --- Search Logic ---

    fun onSearchQueryChanged(q: String) {
        val current = _searchState.value
        _searchState.value = current.copy(query = q)
        if (q.isBlank() || !current.isAskMode) {
            // clear matches
            _searchState.value = _searchState.value.copy(aiMatches = emptyMap(), isSearchingAI = false)
        }
    }

    fun toggleAskMode() {
        val current = _searchState.value
        val enabled = !current.isAskMode
        _searchState.value = current.copy(
            isAskMode = enabled,
            aiMatches = emptyMap(),
            isSearchingAI = false,
            aiSearchError = null
        )
        if (enabled && current.query.isNotBlank()) {
            triggerAiSearch()
        }
    }

    fun triggerAiSearch() {
        val current = _searchState.value
        if (current.query.isBlank()) return

        _searchState.value = _searchState.value.copy(isSearchingAI = true, aiSearchError = null)

        viewModelScope.launch {
            try {
                if (!ApiService.isApiConfigured()) {
                    _searchState.value = _searchState.value.copy(
                        isSearchingAI = false,
                        aiSearchError = "Gemini API key is not configured. Register it in Secrets Panel."
                    )
                    return@launch
                }

                val links = allLinks.value
                val compactText = links.joinToString("\n") {
                    "ID: ${it.id} | Title: ${it.name} | Source: ${it.source} | Summary: ${it.summary ?: "No summary"} | Tags: ${it.tags} | Category: ${it.category}"
                }

                val results = ApiService.searchWithAI(current.query, compactText)
                val matchesMap = results.associate { it.id to it.reason }

                _searchState.value = _searchState.value.copy(
                    isSearchingAI = false,
                    aiMatches = matchesMap,
                    aiSearchError = if (matchesMap.isEmpty()) "No matches found by AI" else null
                )
            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    isSearchingAI = false,
                    aiSearchError = "AI search failed: ${e.message}"
                )
            }
        }
    }

    // --- Rule Table Management ---

    fun addRule(domain: String, category: String) {
        if (domain.isBlank() || category.isBlank()) return
        viewModelScope.launch {
            repository.insertRule(domain, category)
            showToast("Added rule: $domain → $category")
        }
    }

    fun deleteRule(domain: String) {
        viewModelScope.launch {
            repository.deleteRuleByDomain(domain)
            showToast("Deleted rule for $domain")
        }
    }

    // --- Helper feedback toast ---

    fun showToast(msg: String) {
        _feedbackMessage.value = msg
    }

    fun clearToast() {
        _feedbackMessage.value = null
    }

    // Extraction helper for shared Intent text
    fun extractUrl(text: String): String? {
        val regex = "(https?://[^\\s]+)".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.value
    }

    // Direct automated save and background enrichment of shared links
    fun saveLinkFromShare(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            try {
                // Heuristically categorize and name
                val rules = allRules.value
                val info = Categorizer.categorize(url, rules)
                
                val savedRecord = repository.saveLinkOptimistic(url)
                val refinedRecord = savedRecord.copy(
                    name = if (info.name.isNotBlank()) info.name else savedRecord.name,
                    category = info.category
                )
                repository.updateLink(refinedRecord)
                
                showToast("Saved shared link in ${refinedRecord.category.uppercase()}")
                
                // Secondary AI enrichment in background
                launch {
                    repository.enrichLink(refinedRecord)
                    showToast("Enriched: \"${refinedRecord.name}\"!")
                }
            } catch (e: Exception) {
                showToast("Failed to save shared link: ${e.message}")
            }
        }
    }
}
