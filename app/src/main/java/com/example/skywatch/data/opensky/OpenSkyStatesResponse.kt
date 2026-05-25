package com.example.skywatch.data.opensky

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class OpenSkyStatesResponse(
    val time: Long?,
    val states: List<List<Any?>>?,
)

