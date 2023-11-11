package com.app.stocks_service

import com.app.stocks_service.client.ApiConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApiConfiguration::class)
class StocksServiceApplication

fun main(args: Array<String>) {
	runApplication<StocksServiceApplication>(*args)
}
