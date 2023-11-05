package com.app.stocks_service.util

import com.app.stocks_service.client.dto.CompanyStatus
import com.app.stocks_service.client.dto.CompanyStockPrice
import com.app.stocks_service.repository.entity.CompanyStatusEntity
import com.app.stocks_service.repository.entity.CompanyStockPriceEntity
import com.app.stocks_service.service.dto.CompanyStockPriceForCompare
import java.math.BigDecimal
import java.sql.Timestamp

fun CompanyStatus.toCompanyStatusEntity() = CompanyStatusEntity(
    name = name,
    symbol = symbol,
    isEnabled = isEnabled
)

fun CompanyStockPrice.toCompanyStockPriceEntity(delta: BigDecimal) = CompanyStockPriceEntity(
    latestPrice = latestPrice.toBigDecimalOrNull(),
    change = change.toBigDecimalOrNull(),
    previousVolume = previousVolume.toBigDecimalOrNull(),
    previousClose = previousClose.toBigDecimalOrNull(),
    volume = volume.toBigDecimalOrNull(),
    createdAt = Timestamp(System.currentTimeMillis()),
    delta = delta,
    companySymbol = symbol
)

fun CompanyStockPrice.toCompanyStockPriceForCompare() = CompanyStockPriceForCompare(
    latestPrice = latestPrice.toBigDecimalOrNull(),
    change = change.toBigDecimalOrNull(),
    previousVolume = previousVolume.toBigDecimalOrNull(),
    previousClose = previousClose.toBigDecimalOrNull(),
    volume = volume.toBigDecimalOrNull(),
    symbol = symbol,
)

fun CompanyStockPriceEntity.toCompanyStockPriceForCompare() = CompanyStockPriceForCompare(
    latestPrice = latestPrice,
    change = change,
    previousVolume = previousVolume,
    previousClose = previousClose,
    volume = volume,
    symbol = companySymbol,
)