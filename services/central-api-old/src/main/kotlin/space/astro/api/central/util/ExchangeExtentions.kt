package space.astro.api.central.util

import org.springframework.web.server.ServerWebExchange

object ExchangeAttributeNames {
    const val USER_ID = "user_ID"
    const val ACCESS_TOKEN = "access_token"
}

fun ServerWebExchange.getUserIDOrNUll(): String? {
    return getAttribute(ExchangeAttributeNames.USER_ID)
}

fun ServerWebExchange.getAccessTokenOrNull(): String? {
    return getAttribute(ExchangeAttributeNames.ACCESS_TOKEN)
}

fun ServerWebExchange.getUserID(): String {
    return getAttribute(ExchangeAttributeNames.USER_ID)
        ?: throw IllegalStateException("Missing expected exchange attribute ${ExchangeAttributeNames.USER_ID}")
}

fun ServerWebExchange.getAccessToken(): String {
    return getAttribute(ExchangeAttributeNames.ACCESS_TOKEN)
        ?: throw IllegalStateException("Missing expected exchange attribute ${ExchangeAttributeNames.ACCESS_TOKEN}")
}