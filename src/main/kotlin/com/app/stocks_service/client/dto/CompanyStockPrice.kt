package com.app.stocks_service.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class CompanyStockPrice(
    val symbol: String,
    val companyName: String,
    val latestPrice: Double?,
    val change: Double?,
    val previousVolume: Double?,
    val previousClose: Double?,
    val volume: Double?
)
