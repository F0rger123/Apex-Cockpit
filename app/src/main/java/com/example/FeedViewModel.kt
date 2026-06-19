package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SocialPost
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    object Loading : FeedUiState
    data class Success(
        val posts: List<SocialPost>,
        val isRefreshing: Boolean = false,
        val isPaginating: Boolean = false,
        val endOfFeedReached: Boolean = false
    ) : FeedUiState
    data class Error(val errorMessage: String) : FeedUiState
}

class FeedViewModel(
    private val repository: FeedRepository
) : ViewModel() {

    private val TAG = "FeedViewModel"

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Temporary storage for active post details comments overlay list
    private val _postComments = MutableStateFlow<List<ResourceComment>>(emptyList())
    val postComments: StateFlow<List<ResourceComment>> = _postComments.asStateFlow()

    private val _isCommentsLoading = MutableStateFlow(false)
    val isCommentsLoading: StateFlow<Boolean> = _isCommentsLoading.asStateFlow()

    private val fullPostList = mutableListOf<SocialPost>()

    init {
        observeRoomCache()
        loadInitialPage()
    }

    /**
     * Start observing persistent local database as immediate viewport flow
     */
    private fun observeRoomCache() {
        viewModelScope.launch {
            repository.cachedPosts.collect { localList ->
                // Maintain list layout from database if background tasks are running
                if (_uiState.value is FeedUiState.Success) {
                    val current = _uiState.value as FeedUiState.Success
                    // Merge local cache lists gracefully with what we have
                    val mergedList = if (fullPostList.isEmpty()) localList else {
                        // Blend and secure unique items
                        (fullPostList + localList).distinctBy { it.id }.sortedByDescending { it.timestamp }
                    }
                    _uiState.value = current.copy(posts = mergedList)
                }
            }
        }
    }

    /**
     * Pulls the first page from Firestore cloud document store.
     */
    fun loadInitialPage() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            repository.resetPagination()
            fullPostList.clear()

            try {
                val initList = repository.fetchPaginatedFeed(limit = 6)
                fullPostList.addAll(initList)

                _uiState.value = FeedUiState.Success(
                    posts = fullPostList.toList(),
                    isRefreshing = false,
                    isPaginating = false,
                    endOfFeedReached = initList.size < 6
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed rendering first page from cloud: ${e.message}")
                // Look to see if we have Room records before throwing hard error state
                val cached = repository.cachedPosts.first()
                if (cached.isNotEmpty()) {
                    fullPostList.clear()
                    fullPostList.addAll(cached)
                    _uiState.value = FeedUiState.Success(
                        posts = fullPostList.toList(),
                        endOfFeedReached = true
                    )
                } else {
                    _uiState.value = FeedUiState.Error("Failed to synchronize cloud telemetry feed. Verify device link network.")
                }
            }
        }
    }

    /**
     * Triggered on infinite scroll threshold. Pulls next page with database cursors.
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState !is FeedUiState.Success || currentState.isPaginating || currentState.endOfFeedReached) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isPaginating = true)
            try {
                val nextList = repository.fetchPaginatedFeed(limit = 6)
                if (nextList.isEmpty()) {
                    _uiState.value = currentState.copy(
                        isPaginating = false,
                        endOfFeedReached = true
                    )
                } else {
                    fullPostList.addAll(nextList)
                    _uiState.value = currentState.copy(
                        posts = fullPostList.toList().distinctBy { it.id }.sortedByDescending { it.timestamp },
                        isPaginating = false,
                        endOfFeedReached = nextList.size < 6
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pagination handshake stalled: ${e.message}")
                _uiState.value = currentState.copy(isPaginating = false)
            }
        }
    }

    /**
     * Perform immediate pull refresh action on list views
     */
    fun refreshFeed() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FeedUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            repository.resetPagination()
            fullPostList.clear()

            try {
                val freshList = repository.fetchPaginatedFeed(limit = 6)
                fullPostList.addAll(freshList)
                _uiState.value = FeedUiState.Success(
                    posts = fullPostList.toList(),
                    isRefreshing = false,
                    isPaginating = false,
                    endOfFeedReached = freshList.size < 6
                )
            } catch (e: Exception) {
                Log.e(TAG, "Pull Refresh error occurred: ${e.message}")
                if (currentState is FeedUiState.Success) {
                    _uiState.value = currentState.copy(isRefreshing = false)
                } else {
                    _uiState.value = FeedUiState.Error("Network handshake error.")
                }
            }
        }
    }

    /**
     * Adds a post directly utilizing remote and local layers
     */
    fun submitTelemetryPost(text: String, telemetryTag: String) {
        viewModelScope.launch {
            try {
                val newlyCreated = repository.createPost(
                    authorName = "Apex Pilot",
                    authorTag = "@pilot_me",
                    text = text,
                    telemetry = telemetryTag
                )
                // Instantly inject into current success state header
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val updatedList = (listOf(newlyCreated) + currentState.posts).distinctBy { it.id }
                    fullPostList.add(0, newlyCreated)
                    _uiState.value = currentState.copy(posts = updatedList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed inserting feed dispatch record: ${e.message}")
            }
        }
    }

    /**
     * Core functional interaction triggering increment likes inside firebase
     */
    fun likeSocialPost(post: SocialPost) {
        viewModelScope.launch {
            try {
                val updated = repository.toggleLike(post)
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val freshList = currentState.posts.map { item ->
                        if (item.id == post.id) updated else item
                    }
                    _uiState.value = currentState.copy(posts = freshList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Likes update action stalled: ${e.message}")
            }
        }
    }

    /**
     * Loads the interactive comment trail of targeted post
     */
    fun fetchCommentsTrail(postId: Long) {
        viewModelScope.launch {
            _isCommentsLoading.value = true
            _postComments.value = emptyList()
            try {
                val comments = repository.fetchCommentsForPost(postId)
                _postComments.value = comments
            } catch (e: Exception) {
                Log.e(TAG, "Comments extraction failed: ${e.message}")
            } finally {
                _isCommentsLoading.value = false
            }
        }
    }

    /**
     * Posts a comment directly inside Firestore comments trail module.
     */
    fun postChassisComment(postId: Long, commentText: String, activePost: SocialPost) {
        if (commentText.isBlank()) return
        viewModelScope.launch {
            try {
                val newComment = repository.addComment(
                    postId = postId,
                    author = "Pilot_Racer",
                    text = commentText,
                    postToUpdate = activePost
                )
                val currentCommentsList = _postComments.value.toMutableList()
                currentCommentsList.add(newComment)
                _postComments.value = currentCommentsList

                // Update comments count on main view representation
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    val freshList = currentState.posts.map { item ->
                        if (item.id == postId) item.copy(commentsCount = item.commentsCount + 1) else item
                    }
                    _uiState.value = currentState.copy(posts = freshList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed posting chassis handshake comment index: ${e.message}")
            }
        }
    }
}
