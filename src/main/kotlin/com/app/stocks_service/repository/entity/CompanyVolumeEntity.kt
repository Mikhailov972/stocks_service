package com.app.stocks_service.repository.entity

import java.math.BigDecimal

data class CompanyVolumeEntity(
    val name: String,
    val volume: BigDecimal
)
