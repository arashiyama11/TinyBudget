package io.github.arashiyama11.tinybudget.util

import java.text.NumberFormat
import java.util.Locale


private val formatter = NumberFormat.getNumberInstance(Locale.JAPAN)
fun Number.toJPYString(): String {
    formatter.format(this).let { formattedNumber ->
        return "¥$formattedNumber"
    }
}