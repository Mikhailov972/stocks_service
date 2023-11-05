package com.app.stocks_service.repository

import com.app.stocks_service.repository.entity.CompanyDeltaEntity
import com.app.stocks_service.repository.entity.CompanyStatusEntity
import com.app.stocks_service.repository.entity.CompanyStockPriceEntity
import com.app.stocks_service.repository.entity.CompanyVolumeEntity
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional


@Repository
class StocksRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    @Transactional
    fun saveCompaniesStatus(companiesStatusEntity: Collection<CompanyStatusEntity>) {
        val sql = """
            INSERT INTO companies(name, symbol, is_enabled)
            VALUES (:name, :symbol, :isEnabled)
            ON CONFLICT (symbol) DO UPDATE SET is_enabled = :isEnabled
        """.trimIndent()

        val companiesStatusArray = companiesStatusEntity.map { companyStatusBinding(it) }.toTypedArray()
        jdbcTemplate.batchUpdate(
            sql,
            companiesStatusArray
        )
    }

    @Transactional(readOnly = true)
    fun getAllActiveCompaniesStatus(): List<CompanyStatusEntity> {
        val sql = "SELECT id, name, symbol, is_enabled FROM companies WHERE is_enabled = true"
        return jdbcTemplate.query(sql, companyStatusEntityMapper())
    }

    @Transactional(readOnly = true)
    fun getAllCompaniesStatus(): List<CompanyStatusEntity> {
        val sql = "SELECT id, name, symbol, is_enabled FROM companies"
        return jdbcTemplate.query(sql, companyStatusEntityMapper())
    }

    @Transactional
    fun saveCompaniesStockPrice(companiesStockPrice: Collection<CompanyStockPriceEntity>) {
        val sql = """
            INSERT INTO stock_prices(latest_price, change, previous_volume, previous_close, volume, created_at, delta, company_symbol)
            VALUES (:latestPrice, :change, :previousVolume, :previousClose, :volume, :createdAt, :delta, :companySymbol)
        """.trimIndent()

        val companiesStockPriceBindingArray = companiesStockPrice.map { companyStockPriceBinding(it) }.toTypedArray()
        jdbcTemplate.batchUpdate(sql, companiesStockPriceBindingArray)
    }

    /**
     * Достаём активные компании с наибольшим процентным изменением стоимости акций
     *
     * @param limit кол-во компаний в ответе
     *
     * @return возвращает активные компании с наибольшим процентным изменением стоимости акций
     */
    @Transactional(readOnly = true)
    fun findCompaniesWithLargestDelta(limit: Int = 5): List<CompanyDeltaEntity> {
        val sql = """            
            SELECT c.name, sp.delta
            FROM stock_prices sp
                     JOIN
                 (SELECT isp.company_symbol, MAX(isp.created_at) as created_at
                  FROM stock_prices isp
                  GROUP BY isp.company_symbol) AS latest_created_at
                 ON latest_created_at.created_at = sp.created_at AND latest_created_at.company_symbol = sp.company_symbol
                     JOIN companies c on sp.company_symbol = c.symbol
            WHERE c.is_enabled = true
            ORDER BY delta DESC
            LIMIT :limit
        """.trimIndent()
        val param = MapSqlParameterSource().addValue("limit", limit)
        return jdbcTemplate.query(sql, param, companyDeltaEntityMapper())
    }

    /**
     * Достаём активные компании с наибольшим общим объёмом акций
     *
     * @param limit кол-во компаний в ответе
     *
     * @return возвращает активные компании с наибольшим общим объёмом акций
     */
    @Transactional(readOnly = true)
    fun findCompaniesWithLargestVolumeTest(limit: Int = 5): List<CompanyVolumeEntity> {
        val sql = """            
            SELECT c.name, COALESCE(sp.volume, sp.previous_volume) as volume
            FROM stock_prices sp
                     JOIN
                 (SELECT isp.company_symbol, MAX(isp.created_at) as created_at
                  FROM stock_prices isp
                  GROUP BY isp.company_symbol) AS latest_created_at
                 ON latest_created_at.created_at = sp.created_at AND latest_created_at.company_symbol = sp.company_symbol
                     JOIN companies c on sp.company_symbol = c.symbol
            WHERE c.is_enabled = true AND COALESCE(sp.volume, sp.previous_volume) IS NOT NULL
            ORDER BY volume DESC, c.name
            LIMIT :limit
        """.trimIndent()
        val param = MapSqlParameterSource().addValue("limit", limit)
        return jdbcTemplate.query(sql, param, companyVolumeEntityMapper())
    }

    /**
     * Достаём актуальную информацию по акциям компаний по ID
     *
     * @param symbols ID компаний
     *
     * @return актуальную информацию по акциям компаний
     */
    @Transactional(readOnly = true)
    fun findActualCompaniesStockPriceBySymbols(symbols: Collection<String>): List<CompanyStockPriceEntity> {
        if (symbols.isEmpty()) return emptyList()

        val sql = """
            SELECT sp.id,
                   sp.latest_price,
                   sp.change,
                   sp.previous_volume,
                   sp.previous_close,
                   sp.volume,
                   sp.created_at,
                   sp.delta,
                   sp.company_symbol
            FROM stock_prices sp
                     JOIN (SELECT company_symbol, MAX(created_at) AS created_at
                           FROM stock_prices
                           GROUP BY company_symbol) AS created_at_stock_prices
                          ON sp.company_symbol = created_at_stock_prices.company_symbol AND
                             sp.created_at = created_at_stock_prices.created_at
            WHERE sp.company_symbol IN (:symbols)
        """.trimIndent()

        val param = MapSqlParameterSource().addValue("symbols", symbols)
        return jdbcTemplate.query(sql, param, companyStockPriceEntityMapper())
    }

    private fun companyStatusEntityMapper() = RowMapper<CompanyStatusEntity> { rs, _ ->
        CompanyStatusEntity(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            symbol = rs.getString("symbol"),
            isEnabled = rs.getBoolean("is_enabled")
        )
    }

    private fun companyStockPriceEntityMapper() = RowMapper<CompanyStockPriceEntity> { rs, _ ->
        CompanyStockPriceEntity(
            id = rs.getLong("id"),
            latestPrice = rs.getBigDecimal("latest_price"),
            change = rs.getBigDecimal("change"),
            previousVolume = rs.getBigDecimal("previous_volume"),
            previousClose = rs.getBigDecimal("previous_close"),
            volume = rs.getBigDecimal("volume"),
            createdAt = rs.getTimestamp("created_at"),
            delta = rs.getBigDecimal("delta"),
            companySymbol = rs.getString("company_symbol")
        )
    }

    private fun companyVolumeEntityMapper() = RowMapper<CompanyVolumeEntity> { rs, _ ->
        CompanyVolumeEntity(
            name = rs.getString("name"),
            volume = rs.getBigDecimal("volume")
        )
    }

    private fun companyDeltaEntityMapper() = RowMapper<CompanyDeltaEntity> { rs, _ ->
        CompanyDeltaEntity(
            name = rs.getString("name"),
            delta = rs.getBigDecimal("delta")
        )
    }

    private fun companyStockPriceBinding(companyStockPriceEntity: CompanyStockPriceEntity): SqlParameterSource {
        return MapSqlParameterSource()
            .addValue("latestPrice", companyStockPriceEntity.latestPrice)
            .addValue("change", companyStockPriceEntity.change)
            .addValue("previousVolume", companyStockPriceEntity.previousVolume)
            .addValue("previousClose", companyStockPriceEntity.previousClose)
            .addValue("volume", companyStockPriceEntity.volume)
            .addValue("createdAt", companyStockPriceEntity.createdAt)
            .addValue("delta", companyStockPriceEntity.delta)
            .addValue("companySymbol", companyStockPriceEntity.companySymbol)
    }

    private fun companyStatusBinding(companyStatusEntity: CompanyStatusEntity): SqlParameterSource {
        return MapSqlParameterSource()
            .addValue("name", companyStatusEntity.name)
            .addValue("symbol", companyStatusEntity.symbol)
            .addValue("isEnabled", companyStatusEntity.isEnabled)
    }
}