package com.tzp

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalTime

data class Address(val street: String,
                   val number: String,
                   val zip: String,
                   val city: String,
                   val country: String)

data class Time(val startDate: LocalDate,
                val endDate: LocalDate,
                val startTime: LocalTime,
                val endTime: LocalTime)

data class Signature(val status: Status,
                     val assignee: String?,
                     val details: String)

data class Zone(val id: String?,
                val location: JsonNode,
                val address: Address,
                val length: Int,
                val reason: String,
                val doubleSided: Boolean,
                val time: Time,
                val signature: Signature? = null,
                val author: String?
)

data class Subscription(val id: String?,
                        val author: String?,
                        val location: JsonNode)