package com.verygoodsecurity.add_card.orchestration.network.util

internal fun Number.isSuccessHttpCode() = this in 200..299
