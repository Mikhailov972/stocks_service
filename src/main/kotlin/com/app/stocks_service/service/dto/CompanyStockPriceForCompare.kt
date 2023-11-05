package com.app.stocks_service.service.dto

import java.math.BigDecimal

data class CompanyStockPriceForCompare(
    val symbol: String,
    val latestPrice: BigDecimal?,
    val change: BigDecimal?,
    val previousVolume: BigDecimal?,
    val previousClose: BigDecimal?,
    val volume: BigDecimal?
)
