package com.verygoodsecurity.add_card.orchestration.util

import android.app.Activity
import android.widget.Toast

fun Activity.showShort(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}