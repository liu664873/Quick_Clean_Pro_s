package com.quickcleanpro.phonecleaner.use.feature.toolbox.domain

data class NetworkInfo(
    val type: String = "--",
    val ssid: String = "--",
    val ip: String = "--",
)

fun interface NetworkInfoReader {
    fun read(): NetworkInfo
}
