package com.app.stocks_service.service

import com.app.stocks_service.CleanDatabaseTest
import com.app.stocks_service.client.dto.CompanyStockPrice
import com.app.stocks_service.repository.StocksRepository
import com.app.stocks_service.repository.entity.CompanyStatusEntity
import com.app.stocks_service.repository.entity.CompanyStockPriceEntity
import com.app.stocks_service.util.toCompanyStockPriceEntity
import com.app.stocks_service.util.toCompanyStockPriceForCompare
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.DecimalFormat
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class StocksServiceTest @Autowired constructor(
    @SpyBean val stocksService: StocksService,
    val stocksRepository: StocksRepository
) : CleanDatabaseTest() {

    /**
     * Тест проверяет, что записи компаний сравниваются корректно
     * Given: Две компании, у каждой есть по одной записи
     * When: Вызываем метод сравнения записей, у первой компании акции изменились, а у второй нет
     * Then: Получаем одну запись от первой компании
     */
    @Test
    fun compareActualCompaniesInfoWithPreviousCompaniesInfoTest(): Unit = runBlocking {
        // --- Given ---
        val firstCompany = random.nextObject(CompanyStatusEntity::class.java).copy(symbol = "A")
        val secondCompany = random.nextObject(CompanyStatusEntity::class.java).copy(symbol = "B")

        stocksRepository.saveCompaniesStatus(listOf(firstCompany, secondCompany))

        val firstStockPrice =
            random.nextObject(CompanyStockPriceEntity::class.java)
                .copy(
                    companySymbol = firstCompany.symbol,
                    latestPrice = BigDecimal.valueOf(50.0),
                    createdAt = Timestamp.valueOf(LocalDateTime.now().minusDays(1))
                )

        val secondStockPrice = CompanyStockPriceEntity(
            companySymbol = secondCompany.symbol,
            latestPrice = BigDecimal.valueOf(100.0),
            change = BigDecimal.valueOf(100.0),
            previousVolume = BigDecimal.valueOf(100.0),
            previousClose = BigDecimal.valueOf(100.0),
            volume = BigDecimal.valueOf(100.0),
            createdAt = Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
            delta = BigDecimal.ZERO
        )

        stocksRepository.saveCompaniesStockPrice(listOf(firstStockPrice, secondStockPrice))

        val newCompanyStockPrice =
            random.nextObject(CompanyStockPrice::class.java).copy(latestPrice = 100.0).copy(firstCompany.symbol)

        val oldCompanyStockPrice = CompanyStockPrice(
            symbol = secondCompany.symbol,
            companyName = secondCompany.name,
            latestPrice = secondStockPrice.latestPrice?.toDouble(),
            change = secondStockPrice.change?.toDouble(),
            previousVolume = secondStockPrice.previousVolume?.toDouble(),
            previousClose = secondStockPrice.previousClose?.toDouble(),
            volume = secondStockPrice.volume?.toDouble()
        )

        doReturn(listOf(newCompanyStockPrice, oldCompanyStockPrice)).`when`(stocksService)
            .getCompanyInfoBySymbols(setOf(firstCompany.symbol, secondCompany.symbol))

        // --- When ---
        val actual = stocksService.compareActualCompaniesInfoWithPreviousCompaniesInfo(
            setOf(
                firstCompany.symbol,
                secondCompany.symbol
            )
        )

        val expected = listOf(
            newCompanyStockPrice.toCompanyStockPriceEntity(BigDecimal.valueOf(100))
        )

        // --- Then ---
        Assertions.assertEquals(expected.size, actual.size)
        Assertions.assertEquals(
            expected.first().toCompanyStockPriceForCompare(),
            actual.first().toCompanyStockPriceForCompare()
        )
    }

    /**
     * Тест проверяет, что записываются актуальные данные, если в БД нет записей
     * Given: Одна компания, у которой нет записей
     * When: Вызываем метод сравнения записей
     * Then: Получаем одну запись
     */
    @Test
    fun compareActualCompanyInfoWithPreviousCompanyInfoTest(): Unit = runBlocking {
        // --- Given ---
        val firstCompany = random.nextObject(CompanyStatusEntity::class.java).copy(symbol = "A")

        stocksRepository.saveCompaniesStatus(listOf(firstCompany))

        val newCompanyStockPrice = random.nextObject(CompanyStockPrice::class.java).copy(firstCompany.symbol)

        doReturn(listOf(newCompanyStockPrice)).`when`(stocksService).getCompanyInfoBySymbols(setOf(firstCompany.symbol))

        // --- When ---
        val expected = listOf(newCompanyStockPrice.toCompanyStockPriceEntity(BigDecimal.ZERO))

        val actual = stocksService.compareActualCompaniesInfoWithPreviousCompaniesInfo(setOf(firstCompany.symbol))

        // --- Then ---
        Assertions.assertEquals(expected.size, actual.size)
        Assertions.assertEquals(expected.first().companySymbol, actual.first().companySymbol)
    }


    /**
     * Тест проверяет, что корректно возвращается положительное число
     */
    @Test
    fun positiveCountDeltaChangeTest() {
        val actual = stocksService.countDeltaChange(BigDecimal.valueOf(42.63), BigDecimal.valueOf(75.56))
        Assertions.assertEquals(BigDecimal.valueOf(77.25), actual)
    }

    /**
     * Тест проверяет, что корректно возвращается отрицательное число
     */
    @Test
    fun negativeCountDeltaChangeTest() {
        val actual = stocksService.countDeltaChange(BigDecimal.valueOf(99.56), BigDecimal.valueOf(43.23))
        Assertions.assertEquals(DecimalFormat("#.##").format(-56.58).toBigDecimal(), actual)
    }

    /**
     * Тест проверяет, что метод возвращает 0.0 если передаём previousPrice 0.0
     */
    @Test
    fun whenPreviousValueIsZero() {
        val actual = stocksService.countDeltaChange(BigDecimal.ZERO, BigDecimal.valueOf(100.0))
        Assertions.assertEquals(BigDecimal.ZERO, actual)
    }

    /**
     * Тест проверяет, что метод возвращает 0.0 если передаём previousPrice null
     */
    @Test
    fun whenPreviousValueIsNull() {
        val actual = stocksService.countDeltaChange(null, BigDecimal.valueOf(100.0))
        Assertions.assertEquals(BigDecimal.ZERO, actual)
    }

    /**
     * Тест проверяет, что метод возвращает 0.0 если передаём currentPrice null
     */
    @Test
    fun whenCurrentValueIsNull() {
        val actual = stocksService.countDeltaChange(BigDecimal.valueOf(100.0), null)
        Assertions.assertEquals(BigDecimal.ZERO, actual)
    }
}