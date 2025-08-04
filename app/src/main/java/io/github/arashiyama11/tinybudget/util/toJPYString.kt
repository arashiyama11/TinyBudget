package io.github.arashiyama11.tinybudget.util

import java.text.NumberFormat
import java.util.Locale


fun Number.toJPYString(): String {
    NumberFormat.getNumberInstance(Locale.JAPAN).format(this).let { formattedNumber ->
        return "¥$formattedNumber"
    }
}