package com.app.stocks_service.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "iexcloud.api")
data class ApiConfiguration(
    var key: String
)
