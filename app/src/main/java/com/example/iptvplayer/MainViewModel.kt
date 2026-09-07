package com.example.iptvplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Category
import com.example.iptvplayer.data.Channel
import com.example.iptvplayer.data.ContentType
import com.example.iptvplayer.data.PlaylistRepository
import com.example.iptvplayer.data.Profile
import com.example.iptvplayer.data.xtream.XtreamSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    sealed class LoginState {
        data object Idle : LoginState()
        data object Loading : LoginState()
        data object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private var repository: PlaylistRepository? = null

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _seriesList = MutableStateFlow<List<XtreamSeries>>(emptyList())
    val seriesList: StateFlow<List<XtreamSeries>> = _seriesList

    private val _episodes = MutableStateFlow<List<Channel>>(emptyList())
    val episodes: StateFlow<List<Channel>> = _episodes

    // Search has its own state so it never overwrites what a category/episode
    // screen already loaded further back in the nav stack.
    private val _searchChannels = MutableStateFlow<List<Channel>>(emptyList())
    val searchChannels: StateFlow<List<Channel>> = _searchChannels

    private val _searchSeries = MutableStateFlow<List<XtreamSeries>>(emptyList())
    val searchSeries: StateFlow<List<XtreamSeries>> = _searchSeries

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading

    // Lets MainActivity know whether the player screen is currently on top,
    // so it knows whether to enter Picture-in-Picture when the user leaves.
    private val _isPlayerActive = MutableStateFlow(false)
    val isPlayerActive: StateFlow<Boolean> = _isPlayerActive
    fun setPlayerActive(active: Boolean) { _isPlayerActive.value = active }

    // Set by MainActivity when Picture-in-Picture mode is entered/exited, so
    // the player screen can hide its normal controls in the tiny PiP window.
    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode
    fun setInPipMode(inPip: Boolean) { _isInPipMode.value = inPip }

    fun supportsMoviesAndSeries(): Boolean = repository?.supportsMoviesAndSeries() ?: false

    fun currentProfile(): Profile? = repository?.currentProfile()

    fun refreshPlaylist() {
        repository?.clearCache()
    }

    fun resetLogin() {
        _loginState.value = LoginState.Idle
        repository = null
        _categories.value = emptyList()
        _channels.value = emptyList()
        _seriesList.value = emptyList()
        _episodes.value = emptyList()
        _searchChannels.value = emptyList()
        _searchSeries.value = emptyList()
    }

    fun login(profile: Profile) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val repo = PlaylistRepository(profile)
                val ok = repo.validateLogin()
                if (ok) {
                    repository = repo
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Invalid username or password")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Couldn't connect to server")
            }
        }
    }

    fun loadCategories(type: ContentType) {
        viewModelScope.launch {
            _categories.value = try {
                repository?.getCategories(type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun loadChannels(type: ContentType, categoryId: String?) {
        viewModelScope.launch {
            _channels.value = try {
                repository?.getChannels(type, categoryId) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun loadSeriesList(categoryId: String?) {
        viewModelScope.launch {
            _seriesList.value = try {
                repository?.getSeriesList(categoryId) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun loadEpisodes(seriesId: Int) {
        viewModelScope.launch {
            _episodes.value = try {
                repository?.getSeriesEpisodes(seriesId) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Loads every item of the given type across ALL categories (categoryId =
    // null), so search isn't limited to whatever category the user last opened.
    fun loadAllChannelsForSearch(type: ContentType) {
        viewModelScope.launch {
            _searchLoading.value = true
            _searchChannels.value = try {
                repository?.getChannels(type, null) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            _searchLoading.value = false
        }
    }

    fun loadAllSeriesForSearch() {
        viewModelScope.launch {
            _searchLoading.value = true
            _searchSeries.value = try {
                repository?.getSeriesList(null) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            _searchLoading.value = false
        }
    }

    // Lets MainActivity ask the currently-open player screen to close itself
    // (and run its normal cleanup) when the Activity actually stops — e.g. when
    // the user closes the Picture-in-Picture window's X button.
    private var playerReleaseCallback: (() -> Unit)? = null

    fun registerPlayerReleaseCallback(callback: (() -> Unit)?) {
        playerReleaseCallback = callback
    }

    fun releaseActivePlayer() {
        playerReleaseCallback?.invoke()
    }
}
