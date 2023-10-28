package com.app.stocks_service.repository.entity

import java.sql.Timestamp

data class CompanyStockPriceEntity(
    val id: Long? = null,
    val latestPrice: Double?,
    val change: Double?,
    val previousVolume: Double?,
    val previousClose: Double?,
    val volume: Double?,
    val createdAt: Timestamp,
    val delta: Double,
    val companySymbol: String
)