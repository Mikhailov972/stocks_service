CREATE TABLE companies
(
    id         BIGSERIAL PRIMARY KEY,
    symbol     TEXT UNIQUE,
    name       TEXT,
    is_enabled BOOLEAN
);

CREATE TABLE stock_prices
(
    id              BIGSERIAL PRIMARY KEY,
    latest_price    DECIMAL,
    change          DECIMAL,
    previous_volume DECIMAL,
    previous_close  DECIMAL,
    volume          INT,
    created_at      TIMESTAMP,
    delta           DECIMAL NOT NULL DEFAULT 0,
    company_symbol  TEXT REFERENCES companies (symbol)
);


COMMENT ON TABLE companies IS 'Хранит данные о компаниях';

COMMENT ON COLUMN companies.id IS 'Идентификатор строки';
COMMENT ON COLUMN companies.symbol IS 'Идентификатор компании';
COMMENT ON COLUMN companies.name IS 'Имя компании';
COMMENT ON COLUMN companies.is_enabled IS 'Активна ли компания';


COMMENT ON TABLE stock_prices IS 'Данные о котировках акций.';

COMMENT ON COLUMN stock_prices.id IS 'Идентификатор строки';
COMMENT ON COLUMN stock_prices.latest_price IS 'Последняя актуальная цена';
COMMENT ON COLUMN stock_prices.change IS 'Изменение цены между latest_price и previous_close';
COMMENT ON COLUMN stock_prices.previous_volume IS 'Объем предыдущего торгового дня';
COMMENT ON COLUMN stock_prices.previous_close IS 'Цена закрытия предыдущего торгового дня';
COMMENT ON COLUMN stock_prices.volume IS 'Общий объем акций';
COMMENT ON COLUMN stock_prices.created_at IS 'Время создания записи';
COMMENT ON COLUMN stock_prices.delta IS 'Разница между текущим latest_price и предыдущим в процентном отношении';
COMMENT ON COLUMN stock_prices.company_symbol IS 'Идентификатор компании из таблицы companies';