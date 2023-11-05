package com.app.stocks_service

import org.jeasy.random.EasyRandom
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
abstract class BaseTest {
    val random = EasyRandom()
}