package com.example

import android.content.Context
import android.util.Log
import com.example.data.ApexDao
import com.example.data.SocialPost
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production-ready FeedRepository that integrates Firestore database as the cloud source of truth,
 * with active fallback to local Room data base for caching, offline support, and transactional consistency.
 */
class FeedRepository(
    private val apexDao: ApexDao
) {
    private val TAG = "FeedRepository"
    private var lastVisibleDocument: DocumentSnapshot? = null

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore is not initialized. Operating in local Room mode: ${e.message}")
            null
        }
    }

    private fun getFirestoreSafe(): FirebaseFirestore {
        return firestore ?: throw IllegalStateException("FirebaseFirestore is not initialized in this process")
    }

    // Observe local Room cached feed posts for instant responsive rendering
    val cachedPosts: Flow<List<SocialPost>> = apexDao.getAllPosts()

    /**
     * Resets pagination query cursors. Call this on pull-to-refresh.
     */
    fun resetPagination() {
        lastVisibleDocument = null
    }

    /**
     * Fetches a paginated slice of posts from Firestore.
     * Cascades down to local database (inserts fetched posts) to support offline previewing.
     */
    suspend fun fetchPaginatedFeed(limit: Long = 10): List<SocialPost> = withContext(Dispatchers.IO) {
        try {
            var query = getFirestoreSafe().collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            val currentCursor = lastVisibleDocument
            if (currentCursor != null) {
                query = query.startAfter(currentCursor)
            }

            val querySnapshot = query.get().await()
            if (!querySnapshot.isEmpty) {
                lastVisibleDocument = querySnapshot.documents.lastOrNull()
            }

            val fetchedPosts = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id.hashCode().toLong() // stable hashCode for Long PrimaryKey
                    val authorName = doc.getString("authorName") ?: "Anonymous Racer"
                    val authorTag = doc.getString("authorTag") ?: "@pilot"
                    val textContent = doc.getString("textContent") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                    val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                    val isLiked = doc.getBoolean("isLiked") ?: false
                    val hasVideoProof = doc.getBoolean("hasVideoProof") ?: false
                    val videoProofStats = doc.getString("videoProofStats") ?: ""

                    SocialPost(
                        id = id,
                        authorName = authorName,
                        authorTag = authorTag,
                        textContent = textContent,
                        timestamp = timestamp,
                        likesCount = likesCount,
                        commentsCount = commentsCount,
                        isLiked = isLiked,
                        hasVideoProof = hasVideoProof,
                        videoProofStats = videoProofStats
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Firestore post snapshot: ${e.message}")
                    null
                }
            }

            // Sync with local memory cache Database
            if (fetchedPosts.isNotEmpty()) {
                fetchedPosts.forEach { post ->
                    apexDao.insertPost(post)
                }
            }
            return@withContext fetchedPosts
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving cloud documents from Firestore, fallback to database state: ${e.message}")
            // Trigger offline exception or return empty list (ViewModel will flow Room data stream seamlessly)
            throw e
        }
    }

    /**
     * Creates a new social post inside Firestore and upserts to local cache.
     */
    suspend fun createPost(
        authorName: String,
        authorTag: String,
        text: String,
        telemetry: String = ""
    ): SocialPost = withContext(Dispatchers.IO) {
        val docId = "post_${System.currentTimeMillis()}"
        val newPostId = docId.hashCode().toLong()

        val post = SocialPost(
            id = newPostId,
            authorName = authorName,
            authorTag = authorTag,
            textContent = text,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            isLiked = false,
            hasVideoProof = telemetry.isNotEmpty(),
            videoProofStats = telemetry
        )

        // Write to Firestore first
        try {
            val firestoreMap = hashMapOf(
                "authorName" to post.authorName,
                "authorTag" to post.authorTag,
                "textContent" to post.textContent,
                "timestamp" to post.timestamp,
                "likesCount" to post.likesCount,
                "commentsCount" to post.commentsCount,
                "isLiked" to post.isLiked,
                "hasVideoProof" to post.hasVideoProof,
                "videoProofStats" to post.videoProofStats
            )
            getFirestoreSafe().collection("posts").document(docId).set(firestoreMap).await()
            Log.d(TAG, "Cloud sync complete for document path: posts/$docId")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore create operation failed. Saved locally. ${e.message}")
        }

        // Cache into local database
        apexDao.insertPost(post)
        return@withContext post
    }

    /**
     * Toggles local liked state and increments counter in Firestore.
     */
    suspend fun toggleLike(post: SocialPost): SocialPost = withContext(Dispatchers.IO) {
        val updatedPost = post.copy(
            isLiked = !post.isLiked,
            likesCount = if (post.isLiked) (post.likesCount - 1).coerceAtLeast(0) else (post.likesCount + 1)
        )

        // Perform local db updates first (instant UI update)
        apexDao.updatePost(updatedPost)

        // Perform transactional update on Firestore cloud
        try {
            val searchSnapshot = getFirestoreSafe().collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            // Find matching document matching specific attributes
            val matchedDoc = searchSnapshot.documents.firstOrNull {
                it.getString("textContent") == post.textContent && 
                it.getString("authorTag") == post.authorTag
            }

            if (matchedDoc != null) {
                getFirestoreSafe().runTransaction { transaction ->
                    val docRef = matchedDoc.reference
                    transaction.update(docRef, "isLiked", updatedPost.isLiked)
                    transaction.update(docRef, "likesCount", updatedPost.likesCount)
                }.await()
                Log.d(TAG, "Firestore like transaction resolved successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed synchronizing like action to backend: ${e.message}")
        }

        return@withContext updatedPost
    }

    /**
     * Appends a comment to the Firestore comments collection and increments comment counter.
     */
    suspend fun addComment(
        postId: Long,
        author: String,
        text: String,
        postToUpdate: SocialPost
    ): ResourceComment = withContext(Dispatchers.IO) {
        val commentId = "comment_${System.currentTimeMillis()}"
        val comment = ResourceComment(
            id = commentId,
            postId = postId,
            author = author,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Write directly into nested Firestore comment path
        try {
            val commentData = hashMapOf(
                "postId" to comment.postId,
                "author" to comment.author,
                "text" to comment.text,
                "timestamp" to comment.timestamp
            )
            getFirestoreSafe().collection("comments").document(commentId).set(commentData).await()

            // Update comment counter inside database & Firestore
            val updatedPost = postToUpdate.copy(commentsCount = postToUpdate.commentsCount + 1)
            apexDao.updatePost(updatedPost)

            val searchSnapshot = getFirestoreSafe().collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val matchedDoc = searchSnapshot.documents.firstOrNull {
                it.getString("textContent") == postToUpdate.textContent && 
                it.getString("authorTag") == postToUpdate.authorTag
            }

            if (matchedDoc != null) {
                getFirestoreSafe().runTransaction { tr ->
                    tr.update(matchedDoc.reference, "commentsCount", updatedPost.commentsCount)
                }.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing comment upload to remote cloud: ${e.message}")
        }

        return@withContext comment
    }

    /**
     * Pulls active list of comments registered for a specific post.
     */
    suspend fun fetchCommentsForPost(postId: Long): List<ResourceComment> = withContext(Dispatchers.IO) {
        try {
            val commentsSnapshot = getFirestoreSafe().collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            return@withContext commentsSnapshot.documents.map { doc ->
                ResourceComment(
                    id = doc.id,
                    postId = doc.getLong("postId") ?: postId,
                    author = doc.getString("author") ?: "Anonymous Pilot",
                    text = doc.getString("text") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving comments: ${e.message}")
            return@withContext emptyList<ResourceComment>()
        }
    }
}

/**
 * Static nested transfer object representing Comment updates
 */
data class ResourceComment(
    val id: String,
    val postId: Long,
    val author: String,
    val text: String,
    val timestamp: Long
)
