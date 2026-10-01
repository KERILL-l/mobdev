package io.github.mobdev

import java.io.Serializable

data class Contact(
    val id: Long = 0,
    val name: String = "",
    val phoneNumber: String? = null,
    val email: String? = null,
) : Serializable {

    val initials: String
        get() {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(1).uppercase()
                else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
            }
        }
}
