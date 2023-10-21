package com.app.stocks_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StocksServiceApplication

fun main(args: Array<String>) {
	runApplication<StocksServiceApplication>(*args)
}
