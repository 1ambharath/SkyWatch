package com.example.skywatch.domain

import kotlin.math.abs

data class EnrichedFlightData(
    val airlineName: String,
    val aircraftType: String,
    val departureAirportName: String,
    val departureAirportCode: String,
    val arrivalAirportName: String,
    val arrivalAirportCode: String,
)

object FlightDataEnricher {

    private val AircraftTypes = listOf(
        "Airbus A320neo",
        "Boeing 737-800",
        "Airbus A321neo",
        "Boeing 777-300ER",
        "Boeing 787-9 Dreamliner",
        "Airbus A350-900",
        "Boeing 737 MAX 8",
        "Embraer E190-E2",
        "Bombardier CRJ-900",
        "ATR 72-600",
        "Airbus A330-900neo",
        "Boeing 747-8 Intercontinental"
    )

    private val Airports = listOf(
        AirportInfo("Chhatrapati Shivaji Maharaj Int'l", "BOM"),
        AirportInfo("Indira Gandhi International", "DEL"),
        AirportInfo("Kempegowda International", "BLR"),
        AirportInfo("London Heathrow Airport", "LHR"),
        AirportInfo("John F. Kennedy International", "JFK"),
        AirportInfo("Dubai International Airport", "DXB"),
        AirportInfo("Singapore Changi Airport", "SIN"),
        AirportInfo("Tokyo Haneda Airport", "HND"),
        AirportInfo("Sydney Kingsford Smith Airport", "SYD"),
        AirportInfo("Charles de Gaulle Airport", "CDG"),
        AirportInfo("Los Angeles International", "LAX"),
        AirportInfo("O'Hare International Airport", "ORD"),
        AirportInfo("Frankfurt Airport", "FRA"),
        AirportInfo("Zurich Airport", "ZRH"),
        AirportInfo("Amsterdam Airport Schiphol", "AMS"),
        AirportInfo("Hong Kong International", "HKG"),
        AirportInfo("Incheon International Airport", "ICN")
    )

    private val Airlines = mapOf(
        "AIC" to AirlineInfo("Air India", AirportInfo("Indira Gandhi International", "DEL")),
        "DLH" to AirlineInfo("Lufthansa", AirportInfo("Frankfurt Airport", "FRA")),
        "UAE" to AirlineInfo("Emirates", AirportInfo("Dubai International Airport", "DXB")),
        "SWR" to AirlineInfo("Swiss Int'l Air Lines", AirportInfo("Zurich Airport", "ZRH")),
        "BAW" to AirlineInfo("British Airways", AirportInfo("London Heathrow Airport", "LHR")),
        "AAL" to AirlineInfo("American Airlines", AirportInfo("Dallas/Fort Worth Int'l", "DFW")),
        "DAL" to AirlineInfo("Delta Air Lines", AirportInfo("Hartsfield-Jackson Atlanta", "ATL")),
        "UAL" to AirlineInfo("United Airlines", AirportInfo("O'Hare International Airport", "ORD")),
        "QFA" to AirlineInfo("Qantas", AirportInfo("Sydney Kingsford Smith Airport", "SYD")),
        "JAL" to AirlineInfo("Japan Airlines", AirportInfo("Tokyo Haneda Airport", "HND")),
        "SIA" to AirlineInfo("Singapore Airlines", AirportInfo("Singapore Changi Airport", "SIN")),
        "AFR" to AirlineInfo("Air France", AirportInfo("Charles de Gaulle Airport", "CDG")),
        "KLM" to AirlineInfo("KLM Royal Dutch Airlines", AirportInfo("Amsterdam Airport Schiphol", "AMS")),
        "QTR" to AirlineInfo("Qatar Airways", AirportInfo("Hamad International Airport", "DOH")),
        "ETD" to AirlineInfo("Etihad Airways", AirportInfo("Abu Dhabi International", "AUH")),
        "ANA" to AirlineInfo("All Nippon Airways", AirportInfo("Tokyo Haneda Airport", "HND")),
        "CCA" to AirlineInfo("Air China", AirportInfo("Beijing Capital International", "PEK"))
    )

    // Alternative 2-letter mapping codes often found in simple callsigns
    private val TwoLetterAirlines = mapOf(
        "AI" to "AIC",
        "LH" to "DLH",
        "EK" to "UAE",
        "LX" to "SWR",
        "BA" to "BAW",
        "AA" to "AAL",
        "DL" to "DAL",
        "UA" to "UAL",
        "QF" to "QFA",
        "JL" to "JAL",
        "SQ" to "SIA",
        "AF" to "AFR",
        "KL" to "KLM",
        "QR" to "QTR",
        "EY" to "ETD",
        "NH" to "ANA",
        "CA" to "CCA"
    )

    fun enrich(flightId: String, callsign: String?): EnrichedFlightData {
        val cleanCallsign = callsign?.trim()?.uppercase().orEmpty()
        
        // 1. Identify Airline & Departure Hub
        var airlineCode = ""
        var airlineName = "Commercial Airline"
        var depAirport = AirportInfo("London Heathrow Airport", "LHR") // default fallback

        if (cleanCallsign.length >= 3) {
            val prefix3 = cleanCallsign.substring(0, 3)
            val info = Airlines[prefix3]
            if (info != null) {
                airlineCode = prefix3
                airlineName = info.name
                depAirport = info.hub
            } else if (cleanCallsign.length >= 2) {
                val prefix2 = cleanCallsign.substring(0, 2)
                val mapped3 = TwoLetterAirlines[prefix2]
                val mappedInfo = Airlines[mapped3]
                if (mappedInfo != null) {
                    airlineCode = mapped3.orEmpty()
                    airlineName = mappedInfo.name
                    depAirport = mappedInfo.hub
                }
            }
        }

        // 2. Determine Aircraft Type (based on flightId ICAO24 hash)
        val flightIdHash = abs(flightId.hashCode())
        val aircraftType = AircraftTypes[flightIdHash % AircraftTypes.size]

        // 3. Determine Arrival Airport (based on callsign hash, ensuring it's different from departure)
        val callsignHash = abs(cleanCallsign.hashCode())
        var arrAirport = Airports[callsignHash % Airports.size]
        if (arrAirport.code == depAirport.code) {
            arrAirport = Airports[(callsignHash + 1) % Airports.size]
        }

        return EnrichedFlightData(
            airlineName = airlineName,
            aircraftType = aircraftType,
            departureAirportName = depAirport.name,
            departureAirportCode = depAirport.code,
            arrivalAirportName = arrAirport.name,
            arrivalAirportCode = arrAirport.code
        )
    }

    private data class AirportInfo(val name: String, val code: String)
    private data class AirlineInfo(val name: String, val hub: AirportInfo)
}
