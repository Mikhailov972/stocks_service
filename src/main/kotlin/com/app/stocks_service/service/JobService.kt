package com.app.stocks_service.service

import com.app.stocks_service.repository.StocksRepository
import com.app.stocks_service.util.toCompanyStatusEntity
import jakarta.annotation.PostConstruct
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile("!test")
class JobService(val stocksService: StocksService, val stocksRepository: StocksRepository) {

    val threadPool = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    @PostConstruct
    fun getInfoCompaniesInit() {
        CoroutineScope(threadPool).launch {
            val allCompanies = stocksService.getAllCompanyStatus()
            stocksRepository.saveCompaniesStatus(allCompanies.map { it.toCompanyStatusEntity() })

            val allActiveSymbols = allCompanies.filter { it.isEnabled }.map { it.symbol }.toSet()

            while (true) {
                val chunkedActiveSymbols = allActiveSymbols.chunked(100) { it.toSet() }

                chunkedActiveSymbols.forEach {
                    val actualCompanies = stocksService.compareActualCompaniesInfoWithPreviousCompaniesInfo(it)
                    stocksRepository.saveCompaniesStockPrice(actualCompanies)
                }
            }
        }
    }

    @Scheduled(cron = "0/5 * * * * *")
    fun printStocksInfo() {
        CoroutineScope(threadPool).launch {
            println("Компании с наибольшим процентным изменением стоимости акций")
            stocksRepository.findCompaniesWithLargestDelta().forEach { println(it) }
            println("\n")
            println("Компании с наибольшим общим объёмом акций")
            stocksRepository.findCompaniesWithLargestVolumeTest().forEach { println(it) }
        }
    }
}