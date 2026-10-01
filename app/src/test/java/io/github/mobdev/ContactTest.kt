package io.github.mobdev

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactTest {

    @Test
    fun `test initials generation with two names`() {
        val contact = Contact(name = "Иван Иванов")
        assertEquals("ИИ", contact.initials)
    }

    @Test
    fun `test initials generation with single name`() {
        val contact = Contact(name = "Александр")
        assertEquals("А", contact.initials)
    }

    @Test
    fun `test initials generation with multiple words`() {
        val contact = Contact(name = "John Ronald Reuel Tolkien")
        assertEquals("JR", contact.initials)
    }

    @Test
    fun `test initials generation with empty or whitespace name`() {
        val contact = Contact(name = "   ")
        assertEquals("?", contact.initials)
    }

    @Test
    fun `test contact properties preservation`() {
        val contact = Contact(
            id = 42L,
            name = "Елена Смирнова",
            phoneNumber = "+79991234567",
            email = "elena@example.com",
        )
        assertEquals(42L, contact.id)
        assertEquals("Елена Смирнова", contact.name)
        assertEquals("+79991234567", contact.phoneNumber)
        assertEquals("elena@example.com", contact.email)
        assertEquals("ЕС", contact.initials)
    }
}
