package io.github.mobdev

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Fetch emails mapped by CONTACT_ID
        val emails = HashMap<Long, String>()
        val emailProjection = arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
        )

        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                emailProjection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val emailCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    if (!emails.containsKey(id)) {
                        val email = if (emailCol >= 0) cursor.getString(emailCol) else null
                        if (!email.isNullOrBlank()) {
                            emails[id] = email
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // SecurityException or other if permission missing
        }

        // 2. Fetch contacts with phones
        val contactsMap = LinkedHashMap<Long, Contact>()
        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idCol)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val phone = if (phoneCol >= 0) cursor.getString(phoneCol) else null

                    if (!contactsMap.containsKey(contactId)) {
                        contactsMap[contactId] = Contact(
                            id = contactId,
                            name = name?.takeIf { it.isNotBlank() } ?: "Без имени",
                            phoneNumber = phone,
                            email = emails[contactId],
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // SecurityException or other
        }

        contactsMap.values.toList()
    }
}
