package io.github.mobdev.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoginRequest(
    val name: String,
    val pwd: String,
)

sealed interface MessageData : Serializable {
    data class Text(val text: String) : MessageData
    data class Image(val link: String) : MessageData
}

class MessageDataAdapter : JsonDeserializer<MessageData?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): MessageData? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject
        return when {
            obj.has("Text") -> {
                val textElem = obj.get("Text")
                if (textElem.isJsonObject) {
                    val textObj = textElem.asJsonObject
                    MessageData.Text(textObj.get("text")?.asString.orEmpty())
                } else {
                    MessageData.Text(textElem.asString)
                }
            }
            obj.has("Image") -> {
                val imgElem = obj.get("Image")
                if (imgElem.isJsonObject) {
                    val imgObj = imgElem.asJsonObject
                    MessageData.Image(imgObj.get("link")?.asString.orEmpty())
                } else {
                    MessageData.Image(imgElem.asString)
                }
            }
            else -> null
        }
    }
}

data class ChatMessage(
    val id: Long = 0,
    val from: String = "",
    val to: String? = null,
    @JsonAdapter(MessageDataAdapter::class)
    val data: MessageData? = null,
    val time: Long? = null,
    val isPending: Boolean = false,
) : Serializable {

    val formattedTime: String
        get() {
            if (time == null || time <= 0) return ""
            val millis = if (time < 100_000_000_000L) time * 1000L else time
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(millis))
        }

    val textContent: String?
        get() = (data as? MessageData.Text)?.text

    val imageUrl: String?
        get() = (data as? MessageData.Image)?.link

    val thumbnailUrl: String?
        get() {
            val link = imageUrl ?: return null
            return if (link.startsWith("http://") || link.startsWith("https://")) {
                link
            } else {
                "https://faerytea.name/thumb/" + link.removePrefix("/")
            }
        }

    val fullImageUrl: String?
        get() {
            val link = imageUrl ?: return null
            return if (link.startsWith("http://") || link.startsWith("https://")) {
                link
            } else {
                "https://faerytea.name/img/" + link.removePrefix("/")
            }
        }
}

data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: JsonObject,
) {
    companion object {
        fun createText(from: String, channel: String, text: String): SendMessageRequest {
            val textObj = JsonObject().apply {
                addProperty("text", text)
            }
            val dataObj = JsonObject().apply {
                add("Text", textObj)
            }
            val target = if (channel.contains("@")) channel else "$channel@channel"
            return SendMessageRequest(
                from = from,
                to = target,
                data = dataObj,
            )
        }
    }
}
