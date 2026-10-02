package io.github.mobdev

import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.MessageData
import io.github.mobdev.ui.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStateTest {

    @Test
    fun `test initial state is empty and unselected`() {
        val state = MainUiState()

        assertTrue(state.channels.isEmpty())
        assertFalse(state.isLoadingChannels)
        assertNull(state.selectedChannel)
        assertTrue(state.currentMessages.isEmpty())
        assertNull(state.fullScreenImageUrl)
        assertFalse(state.isLoggedOut)
    }

    @Test
    fun `test selecting channel retrieves cached messages for that channel`() {
        val msg1 = ChatMessage(id = 1, from = "alice", data = MessageData.Text("hi"))
        val msg2 = ChatMessage(id = 2, from = "bob", data = MessageData.Text("hey"))

        val state = MainUiState(
            channels = listOf("general", "random"),
            selectedChannel = "general",
            messagesByChannel = mapOf(
                "general" to listOf(msg1),
                "random" to listOf(msg2),
            ),
        )

        assertEquals("general", state.selectedChannel)
        assertEquals(1, state.currentMessages.size)
        assertEquals("alice", state.currentMessages[0].from)

        val switchedState = state.copy(selectedChannel = "random")
        assertEquals(1, switchedState.currentMessages.size)
        assertEquals("bob", switchedState.currentMessages[0].from)

        val unselectedState = switchedState.copy(selectedChannel = null)
        assertTrue(unselectedState.currentMessages.isEmpty())
    }

    @Test
    fun `test image viewer state open and close`() {
        val initialState = MainUiState()
        assertNull(initialState.fullScreenImageUrl)

        val openedState = initialState.copy(fullScreenImageUrl = "https://faerytea.name/img/cat.png")
        assertEquals("https://faerytea.name/img/cat.png", openedState.fullScreenImageUrl)

        val closedState = openedState.copy(fullScreenImageUrl = null)
        assertNull(closedState.fullScreenImageUrl)
    }

    @Test
    fun `test logout state transition`() {
        val activeState = MainUiState(
            selectedChannel = "general",
            fullScreenImageUrl = "https://faerytea.name/img/cat.png",
        )

        val loggedOutState = activeState.copy(
            isLoggedOut = true,
            selectedChannel = null,
            fullScreenImageUrl = null,
        )

        assertTrue(loggedOutState.isLoggedOut)
        assertNull(loggedOutState.selectedChannel)
        assertNull(loggedOutState.fullScreenImageUrl)
    }
}
