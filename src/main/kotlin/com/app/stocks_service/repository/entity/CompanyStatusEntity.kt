package com.app.stocks_service.repository.entity

data class CompanyStatusEntity(
    val id: Long? = null,
    val name: String,
    val symbol: String,
    val isEnabled: Boolean,
)