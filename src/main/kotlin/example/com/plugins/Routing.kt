package example.com.plugins

import example.com.entities.ContactEntity
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.dsl.delete

@Serializable
data class Contact(val username: String, @SerialName("phone_number") val phoneNumber: String)

@Serializable
data class ContactResponse(val id: Int, val username: String, @SerialName("phone_number") val phoneNumber: String)

fun Application.configureRouting() {

    val dbService = Database.connect(
        url = "jdbc:mysql://localhost:8889/contacts",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root"
    )

    install(ContentNegotiation) {
        json()
    }

    routing {
        route("contacts") {
            get {
                val contacts = dbService.from(ContactEntity).select()
                val result = contacts.map {
                    ContactResponse(
                        id = it[ContactEntity.id] ?: 0,
                        username = it[ContactEntity.userName] ?: "",
                        phoneNumber = it[ContactEntity.phoneNumber] ?: ""
                    )
                }
                call.respond(result)
            }

            post {
                val contact = call.receive<Contact>()
                val generatedKey = dbService.insertAndGenerateKey(ContactEntity) {
                    set(it.userName, contact.username)
                    set(it.phoneNumber, contact.phoneNumber)
                } as Int
                val createdContact = ContactResponse(
                    id = generatedKey,
                    username = contact.username,
                    phoneNumber = contact.phoneNumber
                )
                call.respond(HttpStatusCode.Created, createdContact)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondText(
                    "Invalid or missing id",
                    status = HttpStatusCode.BadRequest
                )
                val contact = call.receive<Contact>()
                val affectedRows = dbService.update(ContactEntity) {
                    set(it.userName, contact.username)
                    set(it.phoneNumber, contact.phoneNumber)
                    where {
                        it.id eq id
                    }
                }
                if (affectedRows > 0) {
                    call.respondText("Contact updated successfully")
                } else {
                    call.respondText("Contact not found", status = HttpStatusCode.NotFound)
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respondText(
                    "Invalid or missing id",
                    status = HttpStatusCode.BadRequest
                )
                val affectedRows = dbService.delete(ContactEntity) {
                    it.id eq id
                }
                if (affectedRows > 0) {
                    call.respondText("Contact deleted successfully")
                } else {
                    call.respondText("Contact not found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}
