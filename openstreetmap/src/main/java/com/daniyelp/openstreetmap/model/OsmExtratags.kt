package com.daniyelp.openstreetmap.model

internal data class OsmExtratags(
    val capital: Boolean?,
    val website: String?,
    val wikidata: String?,
    val wikipedia: String?,
    val population: Long?
)