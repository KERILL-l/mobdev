package io.github.mobdev

import com.google.gson.GsonBuilder
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.MessageData
import io.github.mobdev.data.MessageDataAdapter
import io.github.mobdev.data.SendMessageRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatModelsTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(MessageData::class.java, MessageDataAdapter())
        .create()

    @Test
    fun `test deserializing text message`() {
        val json = """
            {
                "id": 101,
                "from": "alice",
                "to": "general@channel",
                "data": {
                    "Text": {
                        "text": "Hello, world!"
                    }
                },
                "time": 1717632000
            }
        """.trimIndent()

        val message = gson.fromJson(json, ChatMessage::class.java)

        assertEquals(101L, message.id)
        assertEquals("alice", message.from)
        assertEquals("general@channel", message.to)
        assertNotNull(message.data)
        assertTrue(message.data is MessageData.Text)
        assertEquals("Hello, world!", message.textContent)
        assertTrue(message.formattedTime.isNotEmpty())
    }

    @Test
    fun `test deserializing image message`() {
        val json = """
            {
                "id": 102,
                "from": "bob",
                "to": "general@channel",
                "data": {
                    "Image": {
                        "link": "https://faerytea.name/static/cat.png"
                    }
                },
                "time": 1717632500000
            }
        """.trimIndent()

        val message = gson.fromJson(json, ChatMessage::class.java)

        assertEquals(102L, message.id)
        assertEquals("bob", message.from)
        assertNotNull(message.data)
        assertTrue(message.data is MessageData.Image)
        assertEquals("https://faerytea.name/static/cat.png", message.imageUrl)
    }

    @Test
    fun `test creating send text message request`() {
        val request = SendMessageRequest.createText(
            from = "john",
            channel = "general",
            text = "Testing message sending",
        )

        assertEquals("john", request.from)
        assertEquals("general@channel", request.to)
        assertNotNull(request.data.get("Text"))
        val textObj = request.data.getAsJsonObject("Text")
        assertEquals("Testing message sending", textObj.get("text").asString)
    }
}
