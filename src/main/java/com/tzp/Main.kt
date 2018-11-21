package com.tzp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.jackson.jackson
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.channels.consumesAll
import org.locationtech.spatial4j.context.SpatialContext
import org.locationtech.spatial4j.context.jts.JtsSpatialContext
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory
import org.locationtech.spatial4j.shape.Shape
import org.locationtech.spatial4j.shape.SpatialRelation
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.simplejavamail.mailer.config.TransportStrategy
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.math.sign

class Main {
    companion object {
        val zones = mutableListOf<Zone>()
        val subscriptions = mutableListOf<Subscription>()

        fun addZone(zone: Zone, author: String): String {
            val id = UUID.randomUUID().toString()
            zones.add(zone.copy(id = id, author = author, signature = null))
            return id
        }

        fun addSubscription(subscription: Subscription, author: String): String {
            val id = UUID.randomUUID().toString()
            subscriptions.add(subscription.copy(id = id, author = author))
            return id
        }

        fun changeStatus(zone: Zone, signature: Signature, assignee: String) {
            zones.remove(zone)
            zones.add(zone.copy(signature = signature.copy(assignee = assignee)))
            if (signature.status == Status.APPROVED) {
                triggerSubscribers(zone)
            }
        }

        fun triggerSubscribers(zone: Zone) {
            subscriptions.forEach {
                println(zone.location)
                println(ObjectMapper().writeValueAsString(it.location))
                println(zone.location.toShape())
                println(it.location.toShape())
                if (zone.location.toShape().relate(it.location.toShape()) != SpatialRelation.DISJOINT) {
                    val mailer = MailerBuilder
                            .withSMTPServer("mail.gmx.net", 587, "info.tzp@gmx.de", "smartcountryhacks#2018")
                            .withTransportStrategy(TransportStrategy.SMTP_TLS)
                            .buildMailer()
                    mailer.sendMail(
                            EmailBuilder.startingBlank()
                                    .from("info.tzp@gmx.de")
                                    .to(it.author as String)
                                    .withSubject("Neues Halteverbot in Ihrer Nähe!")
                                    .withPlainText("Wir haben ein neues Halteverbot in Ihrer Nähe gefunden. Gehen Sie in die App, um es zu sehen.")
                                    .buildEmail()
                    )
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            embeddedServer(Netty, 8080) {
                install(CORS) {
                    anyHost()
                    header("user")
                }

                install(AutoHeadResponse) {}

                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        registerModule(Jdk8Module())
                        dateFormat = DateFormat.getDateTimeInstance()
                    }
                }

                routing {
                    post("/api/zones") {
                        call.response.header(HttpHeaders.Location, addZone(call.receive(), call.request.header("user")!!))
                        call.respond(HttpStatusCode.Created)
                    }
                    get("/api/zones") {
                        call.respond(zones.filter { it.signature != null && it.signature.status == Status.APPROVED })
                    }
                    get("/api/zones/all") {
                        call.respond(zones)
                    }
                    get("/api/zones/mine") {
                        call.respond(zones.filter { it.author == call.request.header("user") })
                    }
                    get("/api/zones/{id}") {
                        val zone = zones.firstOrNull { it.id == call.parameters.get("id") }
                        if (zone == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                        call.respond(zone)
                    }
                    post("/api/zones/{id}") {
                        val zone = zones.firstOrNull { it.id == call.parameters.get("id") }
                        if (zone == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@post
                        }
                        changeStatus(zone, call.receive(), call.request.header("user")!!)
                        call.respond(HttpStatusCode.Accepted)
                    }
                    get("/api/subscriptions") {
                        call.respond(subscriptions.filter { it.author == call.request.header("user")!! })
                    }
                    post("/api/subscriptions") {
                        val subscription = call.receive<Subscription>()
                        call.response.header(HttpHeaders.Location, addSubscription(subscription, call.request.header("user")!!))
                        call.respond(HttpStatusCode.Created)
                    }
                }
            }.start(wait = true)
        }
        fun Location.toShape(): Shape = JtsSpatialContext.GEO.formats.geoJsonReader.read(ObjectMapper().writeValueAsString(this).reader())
        fun JsonNode.toShape(): Shape = JtsSpatialContext.GEO.formats.geoJsonReader.read(this.toString().reader())
    }
}