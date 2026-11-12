package com.example.macronomocon.ui.comment

import com.example.macronomocon.data.repository.CommentImageStore
import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.ui.common.UiMessage
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val published = mutableListOf<Comment>()
    private val repository = object : CommentRepository {
        override suspend fun findByRecipe(recipeId: String): List<Comment> = emptyList()
        override suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment =
            Comment("c${published.size}", recipeId, "me", text, imageUrl).also { published += it }
        override suspend fun delete(commentId: String) = Unit
        override suspend fun like(commentId: String): Comment = error("unused")
    }
    private val uploaded = mutableListOf<String>()
    private var upload: suspend (String) -> String = {
        uploaded += it
        "https://cdn.example.com/comment-images/me/1.jpg"
    }
    private val images = object : CommentImageStore {
        override suspend fun upload(localUri: String): String = upload.invoke(localUri)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun loaded(store: CommentImageStore? = images) =
        CommentViewModel(repository, store).also { it.loadComments("recipe-1").join() }

    @Test
    fun photoIsUploadedBeforeTheCommentIsPublishedWithItsUrl() = runTest(dispatcher) {
        val vm = loaded()
        vm.publishComment("  Toast the spices  ", "content://media/picker/1").join()
        assertEquals(listOf("content://media/picker/1"), uploaded)
        val comment = vm.uiState.value.data.comments.single()
        assertEquals("Toast the spices", comment.text)
        assertEquals("https://cdn.example.com/comment-images/me/1.jpg", comment.imageUrl)
    }

    @Test
    fun textOnlyCommentsDoNotUpload() = runTest(dispatcher) {
        val vm = loaded()
        vm.publishComment("Add salt").join()
        assertTrue(uploaded.isEmpty())
        assertNull(published.single().imageUrl)
    }

    @Test
    fun failedUploadPublishesNothingAndReportsTheError() = runTest(dispatcher) {
        upload = { throw IOException("offline") }
        val vm = loaded()
        vm.publishComment("Add salt", "content://media/picker/1").join()
        advanceUntilIdle()
        assertTrue(published.isEmpty())
        assertEquals(UiMessage.ERROR_PUBLISH_THIS_COMMENT, vm.uiState.value.errorMessage)
    }

    @Test
    fun photosAreOnlyOfferedWhenAStoreIsAvailable() = runTest(dispatcher) {
        assertTrue(loaded().canAttachPhotos)
        val withoutStore = loaded(store = null)
        assertFalse(withoutStore.canAttachPhotos)
        withoutStore.publishComment("Add salt", "content://media/picker/1").join()
        assertTrue(published.isEmpty())
    }
}
