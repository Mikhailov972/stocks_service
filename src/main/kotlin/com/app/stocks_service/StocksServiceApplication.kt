package com.app.stocks_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class StocksServiceApplication

fun main(args: Array<String>) {
	runApplication<StocksServiceApplication>(*args)
}
