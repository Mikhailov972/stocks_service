package com.app.stocks_service.repository.entity

import java.math.BigDecimal
import java.sql.Timestamp

data class CompanyStockPriceEntity(
    val id: Long? = null,
    val latestPrice: BigDecimal?,
    val change: BigDecimal?,
    val previousVolume: BigDecimal?,
    val previousClose: BigDecimal?,
    val volume: BigDecimal?,
    val createdAt: Timestamp,
    val delta: BigDecimal,
    val companySymbol: String
)