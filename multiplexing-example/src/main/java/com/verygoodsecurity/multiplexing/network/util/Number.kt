package com.verygoodsecurity.multiplexing.network.util

internal fun Number.isSuccessHttpCode() = this in 200..299
