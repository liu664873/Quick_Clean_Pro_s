package com.quickcleanpro.phonecleaner.feature.toolbox.shared.network

data class NetworkInfo(
    val type: String = "--",
    val ssid: String = "--",
    val ip: String = "--",
)

fun interface NetworkInfoReader {
    fun read(): NetworkInfo
}
