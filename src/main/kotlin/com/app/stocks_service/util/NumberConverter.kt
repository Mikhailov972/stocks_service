package com.app.stocks_service.util

import java.math.BigDecimal

fun Double?.toBigDecimalOrNull() = this?.let { BigDecimal.valueOf(it) }