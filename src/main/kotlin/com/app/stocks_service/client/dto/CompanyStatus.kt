package com.app.stocks_service.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class CompanyStatus(
    val name: String,
    val symbol: String,
    val isEnabled: Boolean,
)
