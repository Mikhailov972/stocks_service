package com.app.stocks_service.service

import com.app.stocks_service.client.StocksClient
import com.app.stocks_service.client.dto.CompanyStatus
import com.app.stocks_service.client.dto.CompanyStockPrice
import com.app.stocks_service.repository.StocksRepository
import com.app.stocks_service.repository.entity.CompanyStockPriceEntity
import com.app.stocks_service.util.toBigDecimalOrNull
import com.app.stocks_service.util.toCompanyStockPriceEntity
import com.app.stocks_service.util.toCompanyStockPriceForCompare
import io.ktor.client.call.body
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

@Service
class StocksService(val stocksClient: StocksClient, val stocksRepository: StocksRepository) {
    suspend fun getAllCompanyStatus() = stocksClient.getAllCompanyStatus().body<Array<CompanyStatus>>()

    suspend fun getCompanyInfoBySymbols(companySymbols: Set<String>): List<CompanyStockPrice> {
        val responses = mutableListOf<CompanyStockPrice>()
        coroutineScope {
            companySymbols
                .asFlow()
                .map { async { stocksClient.getCompanyInfo(it) } }
                .buffer(10)
                .collect { response ->
                    val company = response.await().body<Array<CompanyStockPrice>>().first()
                    responses.add(company)
                }
        }
        return responses
    }

    /**
     * Метод рассчитывает дельту в процентном соотношении по формуле ((b-a)/a) * 100, где
     *
     * @param previousPrice значение первого числа - a
     * @param currentPrice значение второго числа - b
     *
     * @return разницу в процентах между двумя числами
     */
    fun countDeltaChange(previousPrice: BigDecimal?, currentPrice: BigDecimal?): BigDecimal {
        if (previousPrice == null || previousPrice == BigDecimal.ZERO || currentPrice == null) return BigDecimal.ZERO
        val df = DecimalFormat("#.##")
        val formulaResult = currentPrice.subtract(previousPrice).divide(previousPrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).round(
            MathContext(4)
        )
        return df.format(formulaResult).toBigDecimal()
    }

    /**
     * Метод сравнивает текущие данные с теми, что лежат в БД
     * Если есть расхождения, то отдаём новые записи, если нет расхождений, то пропускаем запись
     *
     * @param symbols символы компаний
     *
     * @return актуальные записи, которые отличаются от предыдущих
     */
    suspend fun compareActualCompaniesInfoWithPreviousCompaniesInfo(symbols: Set<String>): List<CompanyStockPriceEntity> {
        val actualCompaniesInfo = getCompanyInfoBySymbols(symbols)

        val previousCompaniesInfo =
            stocksRepository.findActualCompaniesStockPriceBySymbols(symbols)
                .map { it.toCompanyStockPriceForCompare() }.associateBy { it.symbol }

        return actualCompaniesInfo.mapNotNull { actualValues ->
            val previousValues = previousCompaniesInfo[actualValues.symbol]

            if (previousValues != actualValues.toCompanyStockPriceForCompare()) {
                val delta = countDeltaChange(
                    previousValues?.latestPrice,
                    actualValues.latestPrice.toBigDecimalOrNull()
                )

                actualValues.toCompanyStockPriceEntity(delta)
            } else null

        }
    }
}