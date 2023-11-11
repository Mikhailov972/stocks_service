package com.app.stocks_service.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component

@Component
class StocksClient(val apiConfiguration: ApiConfiguration) {
    val client = HttpClient(Java) {
        install(HttpRequestRetry) {
            retryOnServerErrors()
            exponentialDelay()
            retryIf { _, response ->
                response.status == TooManyRequests
            }
            retryOnExceptionIf { _, cause ->
                cause is ConnectTimeoutException
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        defaultRequest {
            url {
                url("https://api.iex.cloud")
                parameters.append("token", apiConfiguration.key)
            }
        }

        install(ContentNegotiation)
        {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })

        }
    }

    suspend fun getAllCompanyStatus(): HttpResponse {
        return client.get("/v1/data/core/ref_data_iex_symbols")
    }

    suspend fun getCompanyInfo(symbol: String): HttpResponse {
        val encodeSymbol = symbol.encodeURLParameter()
        return client.get("v1/data/core/quote/$encodeSymbol")
    }
}