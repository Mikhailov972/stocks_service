package com.app.stocks_service

import org.jeasy.random.EasyRandom
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
abstract class BaseTest {
    val random = EasyRandom()
}