package example.com.entities

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object ContactEntity : Table<Nothing>("contact") {
    val id = int("id").primaryKey()
    val userName = varchar("username")
    val phoneNumber = varchar("phone_number")
}
