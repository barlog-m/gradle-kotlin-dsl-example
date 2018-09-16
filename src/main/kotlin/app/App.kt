package app

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    logger.info { "App started" }
}

