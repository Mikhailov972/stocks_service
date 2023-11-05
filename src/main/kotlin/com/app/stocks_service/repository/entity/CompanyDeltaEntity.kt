package com.app.stocks_service.repository.entity

import java.math.BigDecimal

data class CompanyDeltaEntity(
    val name: String,
    val delta: BigDecimal
)
