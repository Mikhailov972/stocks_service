package com.app.stocks_service.repository

import com.app.stocks_service.CleanDatabaseTest
import com.app.stocks_service.repository.entity.CompanyDeltaEntity
import com.app.stocks_service.repository.entity.CompanyStatusEntity
import com.app.stocks_service.repository.entity.CompanyStockPriceEntity
import com.app.stocks_service.repository.entity.CompanyVolumeEntity
import java.sql.Timestamp
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired


class StocksRepositoryTest @Autowired constructor(
    private val stocksRepository: StocksRepository,
) : CleanDatabaseTest() {

    /**
     * Проверяем сохранение компаний и поиск активных компаний
     *
     * Given: Две активных компании и одна не активная
     * When: Сохраняем в БД
     * Then: Получаем две активные компании
     */
    @Test
    fun getAllActiveCompaniesStatusTest() {
        // --- Given ---
        val firstCompany = random.nextObject(CompanyStatusEntity::class.java).copy(id = null, isEnabled = true)
        val secondCompany = random.nextObject(CompanyStatusEntity::class.java).copy(id = null, isEnabled = true)
        val thirdCompany = random.nextObject(CompanyStatusEntity::class.java).copy(id = null, isEnabled = false)

        // --- When ---
        stocksRepository.saveCompaniesStatus(listOf(firstCompany, secondCompany, thirdCompany))

        // --- Than ---
        assertEquals(2, stocksRepository.getAllActiveCompaniesStatus().size)
    }

    /**
     * Проверяем что компании корректно перезаписываются
     *
     * Given: Одна и та же компания, но с разным isEnabled
     * When: Сохраняем в БД две записи
     * Then: Проверяем, что не перезаписался id и isEnabled = true
     */
    @Test
    fun saveCompaniesStatusTest() {
        // --- Given ---
        val firstCompany =
            random.nextObject(CompanyStatusEntity::class.java).copy(id = null, isEnabled = false, symbol = "A")
        val changeFirstCompany =
            random.nextObject(CompanyStatusEntity::class.java).copy(id = null, isEnabled = true, symbol = "A")


        // --- When ---
        stocksRepository.saveCompaniesStatus(listOf(firstCompany))
        val firstCompanyEntity = stocksRepository.getAllCompaniesStatus().first()

        stocksRepository.saveCompaniesStatus(listOf(changeFirstCompany))
        val firstCompanyEntityAfterChange = stocksRepository.getAllActiveCompaniesStatus().first()

        // --- Than ---
        assertEquals(1, stocksRepository.getAllActiveCompaniesStatus().size)
        assertEquals(firstCompanyEntity.id, firstCompanyEntityAfterChange.id)
        assertEquals(true, firstCompanyEntityAfterChange.isEnabled)
    }

    /**
     * Тест проверяет:
     * Что выбираются компании с самой поздней датой
     * Что выбираются компании с наибольшей delta
     * Что соблюдается лимит выборки
     *
     * Given:
     * Первая компания имеет 2 записи вчерашнюю и сегодняшнюю, актуальной будет 5
     * Вторая компания имеет 1 вчерашнюю запись, актуальной будет 10
     * Третья компания имеет 1 сегодняшнюю запись, но отображаться не будет из-за лимита
     *
     * When: Ищем компании с самой поздней и наибольшей delta
     * Then: Получаем две самых больших дельты с поздней датой
     */
    @Test
    fun findCompaniesWithLargestDeltaTest() {
        // --- Given ---
        val yesterday = Timestamp.valueOf(LocalDate.now().atStartOfDay().minusDays(1))
        val today = Timestamp.valueOf(LocalDate.now().atStartOfDay())

        // Первая компания и её акции
        val firstCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "1")

        val firstStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = firstCompany.symbol, delta = 10.0, createdAt = yesterday)

        val secondStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = firstCompany.symbol, delta = 5.0, createdAt = today)

        // Вторая компания и её акции
        val secondCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "2")

        val thirdStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = secondCompany.symbol, delta = 10.0, createdAt = yesterday)

        // Третья компания и её акции
        val thirdCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "3")

        val fourthStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = thirdCompany.symbol, delta = -50.0, createdAt = today)

        stocksRepository.saveCompaniesStatus(listOf(firstCompany, secondCompany, thirdCompany))
        stocksRepository.saveCompaniesStockPrice(
            listOf(firstStockPrice, secondStockPrice, thirdStockPrice, fourthStockPrice)
        )

        val expected = listOf(CompanyDeltaEntity(name = "2", delta = 10.0), CompanyDeltaEntity(name = "1", delta = 5.0))

        // --- When ---
        val actual = stocksRepository.findCompaniesWithLargestDelta(2)

        // --- Then ---
        assertEquals(expected, actual)
    }

    /**
     * Тест проверяет:
     * Что компании сортируются по общему объёму акций
     * Что компании сортируются по имени
     * Что если volume is null, то берётся previous_volume
     * Что соблюдается лимит выборки
     * Что берутся записи с поздней датой
     *
     * Given:
     * Первая компания имеет 2 записи вчерашнюю и сегодняшнюю, актуальной будет 50.0
     * Вторая компания имеет 1 вчерашнюю запись, актуальной будет 49.0
     * Третья компания имеет 1 сегодняшнюю запись, актуальной будет 60.0
     * Четвёртая компания имеет 1 вчерашнюю запись, volume = null, поэтому берётся previous_volume = 50.0
     * Пятая компания имеет 1 сегодняшнюю запись, но отображаться не будет из-за лимита
     *
     * When: Ищем компании с наибольшим общим объёмом акций
     * Then: Получаем 4 записи
     */
    @Test
    fun findCompaniesWithLargestVolumeTest() {
        // --- Given ---
        val yesterday = Timestamp.valueOf(LocalDate.now().atStartOfDay().minusDays(1))
        val today = Timestamp.valueOf(LocalDate.now().atStartOfDay())

        // Первая компания и её акции
        val firstCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "1")

        val firstStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = firstCompany.symbol, volume = 100.0, createdAt = yesterday)

        val secondStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = firstCompany.symbol, volume = 50.0, createdAt = today)

        // Вторая компания и её акции
        val secondCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "2")

        val thirdStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = secondCompany.symbol, volume = 49.0, createdAt = yesterday)

        // Третья компания и её акции
        val thirdCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "3")

        val fourthStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = thirdCompany.symbol, volume = 60.0, createdAt = today)

        // Четвёртая компания и её акции
        val fourthCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "4")

        val fifthStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = fourthCompany.symbol, volume = null, previousVolume = 50.0, createdAt = yesterday)

        // Пятая компания и её акции
        val fifthCompany = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "5")

        val sixthStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = fifthCompany.symbol, volume = null, previousVolume = 40.0, createdAt = today)

        stocksRepository.saveCompaniesStatus(
            listOf(
                firstCompany,
                secondCompany,
                thirdCompany,
                fourthCompany,
                fifthCompany
            )
        )
        stocksRepository.saveCompaniesStockPrice(
            listOf(
                firstStockPrice,
                secondStockPrice,
                thirdStockPrice,
                fourthStockPrice,
                fifthStockPrice,
                sixthStockPrice
            )
        )

        val expected = listOf(
            CompanyVolumeEntity(name = "3", volume = 60.0),
            CompanyVolumeEntity(name = "1", volume = 50.0),
            CompanyVolumeEntity(name = "4", volume = 50.0),
            CompanyVolumeEntity(name = "2", volume = 49.0)
        )

        // --- When ---
        val actual = stocksRepository.findCompaniesWithLargestVolumeTest(4)

        // --- Then ---
        assertEquals(expected, actual)
    }

    /**
     * Тест проверяет, что отдаются актуальная информация об акциях
     *
     * Given: Одна компания, вчерашняя и сегодняшняя акция
     * When: Ищем актуальную информацию об акциях
     * Then: Проверяем, что совпадает дата
     */
    @Test
    fun findActualCompaniesStockPriceBySymbolsTest() {
        // --- Given ---
        val yesterday = Timestamp.valueOf(LocalDate.now().atStartOfDay().minusDays(1))
        val today = Timestamp.valueOf(LocalDate.now().atStartOfDay())

        val company = random.nextObject(CompanyStatusEntity::class.java).copy(isEnabled = true, name = "1")

        val firstStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = company.symbol, createdAt = yesterday)

        val secondStockPrice = random.nextObject(CompanyStockPriceEntity::class.java)
            .copy(companySymbol = company.symbol, createdAt = today)

        stocksRepository.saveCompaniesStatus(listOf(company))
        stocksRepository.saveCompaniesStockPrice(listOf(firstStockPrice, secondStockPrice))

        // --- When ---
        val stocks = stocksRepository.findActualCompaniesStockPriceBySymbols(listOf(company.symbol))

        // --- Then ---
        assertEquals(today, stocks.first().createdAt)
    }

    @Test
    fun findActualCompaniesStockPriceBySymbolsButEmptyListTest() {
        assertDoesNotThrow { stocksRepository.findActualCompaniesStockPriceBySymbols(emptyList()) }
    }
}